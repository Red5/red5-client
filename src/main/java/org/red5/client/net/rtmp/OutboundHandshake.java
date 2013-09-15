/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.client.net.rtmp;

import java.io.File;
import java.security.KeyPair;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.util.FileUtil;

/**
 * Performs handshaking for client connections.
 * 
 * @author Paul Gregoire
 */
public class OutboundHandshake extends RTMPHandshake {

	private static final byte[] SERVER_CONST = "Genuine Adobe Flash Media Server 001".getBytes();

	private static final byte[] CLIENT_CONST = "Genuine Adobe Flash Player 001".getBytes();

	private byte[] outgoingDigest;

	private byte[] incomingDigest;

	private byte[] swfHash;

	private int swfSize;

	public OutboundHandshake() {
		super();
	}

	/**
	 * Generates initial handshake request, validates response, and generates final handshake.
	 * 
	 * @param input incoming RTMP bytes
	 * @return outgoing handshake
	 */
	public IoBuffer doHandshake(IoBuffer input) {
		log.trace("doHandshake: {}", input);
		IoBuffer out = null;
		if (input == null) {
			out = generateClientRequest1();
		} else {
			log.trace("Server handshake - remaining: {} limit: {}", input.remaining(), input.limit());
			// ensure that we have enough handshake data
			if (input.remaining() < (HANDSHAKE_SIZE_SERVER - 1)) {
				log.trace("Handshake was too small, buffering...");
			} else {
				if (decodeServerResponse(input)) {
					out = generateClientRequest2();
				} else {
					log.warn("Decoding server response failed");
				}
			}
		}
		return out;
	}

	/**
	 * Creates the servers handshake bytes
	 */
	@Override
	protected void createHandshakeBytes() {
		handshakeBytes = new byte[Constants.HANDSHAKE_SIZE];
		// timestamp
		handshakeBytes[0] = 0;
		handshakeBytes[1] = 0;
		handshakeBytes[2] = 0;
		handshakeBytes[3] = 0;
		// flash player version 11.2.202.235
		handshakeBytes[4] = (byte) 0x80;
		handshakeBytes[5] = 0;
		handshakeBytes[6] = 7;
		handshakeBytes[7] = 2;
		// fill the rest with random bytes
		byte[] rndBytes = new byte[Constants.HANDSHAKE_SIZE - 8];
		random.nextBytes(rndBytes);
		// copy random bytes into our handshake array
		System.arraycopy(rndBytes, 0, handshakeBytes, 8, (Constants.HANDSHAKE_SIZE - 8));
	}

	/**
	 * Create the first part of the outgoing (client) connection request.
	 * 
	 * @return outgoing handshake
	 */
	public IoBuffer generateClientRequest1() {
		log.debug("generateClientRequest1");
		IoBuffer request = IoBuffer.allocate(Constants.HANDSHAKE_SIZE + 1);
		// set the handshake type byte
		request.put(handshakeType);
		log.debug("Creating client handshake part 1 with keys/hashes");
		IoBuffer buf = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
		buf.put(handshakeBytes);
		buf.flip();
		// create our keypair
		KeyPair keyPair = generateKeyPair();
		outgoingPublicKey = getPublicKey(keyPair);
		byte[] dhPointer = getFourBytesFrom(buf, Constants.HANDSHAKE_SIZE - 4);
		int dhOffset = calculateOffset(dhPointer, 632, 772);
		buf.position(dhOffset);
		buf.put(outgoingPublicKey);
		log.debug("Client public key: {}", Hex.encodeHexString(outgoingPublicKey));
		byte[] digestPointer = getFourBytesFrom(buf, 8);
		int digestOffset = calculateOffset(digestPointer, 728, 12);
		buf.rewind();
		int messageLength = Constants.HANDSHAKE_SIZE - RTMPHandshake.DIGEST_LENGTH;
		byte[] message = new byte[messageLength];
		buf.get(message, 0, digestOffset);
		int afterDigestOffset = digestOffset + RTMPHandshake.DIGEST_LENGTH;
		buf.position(afterDigestOffset);
		buf.get(message, digestOffset, Constants.HANDSHAKE_SIZE - afterDigestOffset);
		outgoingDigest = calculateHMAC_SHA256(message, CLIENT_CONST);
		buf.position(digestOffset);
		buf.put(outgoingDigest);
		buf.rewind();
		// put the generated data into our request
		request.put(buf);
		request.flip();
		return request;
	}

	public boolean decodeServerResponse(IoBuffer in) {
		log.debug("decodeServerResponse: {}", in);
		// minus one for the first byte which is not included (handshake type byte)
		byte[] bytes = new byte[HANDSHAKE_SIZE_SERVER - 1];
		in.get(bytes);

		IoBuffer buf = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
		buf.put(bytes, 0, Constants.HANDSHAKE_SIZE);
		buf.flip();
		log.debug("Server response part 1: {}", buf);
		log.info("Processing server response for encryption");
		byte[] serverTime = new byte[4];
		buf.get(serverTime);
		log.debug("Server time: {}", Hex.encodeHexString(serverTime));

		byte[] serverVersion = new byte[4];
		buf.get(serverVersion);
		log.debug("Server version: {}", Hex.encodeHexString(serverVersion));

		byte[] digestPointer = new byte[4]; // position 8
		buf.get(digestPointer);
		int digestOffset = calculateOffset(digestPointer, 728, 12);
		buf.rewind();

		int messageLength = Constants.HANDSHAKE_SIZE - RTMPHandshake.DIGEST_LENGTH;
		byte[] message = new byte[messageLength];
		buf.get(message, 0, digestOffset);
		int afterDigestOffset = digestOffset + RTMPHandshake.DIGEST_LENGTH;
		buf.position(afterDigestOffset);
		buf.get(message, digestOffset, Constants.HANDSHAKE_SIZE - afterDigestOffset);
		byte[] digest = calculateHMAC_SHA256(message, SERVER_CONST);
		incomingDigest = new byte[RTMPHandshake.DIGEST_LENGTH];
		buf.position(digestOffset);
		buf.get(incomingDigest);

		incomingPublicKey = new byte[128];
		if (Arrays.equals(digest, incomingDigest)) {
			log.info("Type 0 digest comparison success");
			byte[] dhPointer = getFourBytesFrom(buf, Constants.HANDSHAKE_SIZE - 4);
			int dhOffset = calculateOffset(dhPointer, 632, 772);
			buf.position(dhOffset);
			buf.get(incomingPublicKey);
		} else {
			log.warn("Type 0 digest comparison failed, trying type 1 algorithm");
			digestPointer = getFourBytesFrom(buf, 772);
			digestOffset = calculateOffset(digestPointer, 728, 776);
			message = new byte[messageLength];
			buf.rewind();
			buf.get(message, 0, digestOffset);
			afterDigestOffset = digestOffset + RTMPHandshake.DIGEST_LENGTH;
			buf.position(afterDigestOffset);
			buf.get(message, digestOffset, Constants.HANDSHAKE_SIZE - afterDigestOffset);
			digest = calculateHMAC_SHA256(message, SERVER_CONST);
			incomingDigest = new byte[RTMPHandshake.DIGEST_LENGTH];
			buf.position(digestOffset);
			buf.get(incomingDigest);
			if (Arrays.equals(digest, incomingDigest)) {
				log.info("type 1 digest comparison success");
				byte[] dhPointer = getFourBytesFrom(buf, 768);
				int dhOffset = calculateOffset(dhPointer, 632, 8);
				buf.position(dhOffset);
				buf.get(incomingPublicKey);
			} else {
				throw new RuntimeException("Type 1 digest comparison failed");
			}
		}
		log.debug("server public key: {}", Hex.encodeHexString(incomingPublicKey));
		byte[] sharedSecret = getSharedSecret(incomingPublicKey, keyAgreement);
		log.debug("shared secret: {}", Hex.encodeHexString(sharedSecret));

		byte[] digestOut = calculateHMAC_SHA256(incomingPublicKey, sharedSecret);
		try {
			cipherOut = Cipher.getInstance("RC4");
			cipherOut.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(digestOut, 0, 16, "RC4"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		byte[] digestIn = calculateHMAC_SHA256(outgoingPublicKey, sharedSecret);
		try {
			cipherIn = Cipher.getInstance("RC4");
			cipherIn.init(Cipher.DECRYPT_MODE, new SecretKeySpec(digestIn, 0, 16, "RC4"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		// setting up part 2
		IoBuffer partTwo = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
		partTwo.put(bytes, Constants.HANDSHAKE_SIZE, Constants.HANDSHAKE_SIZE);
		partTwo.flip();
		log.debug("Server response part 2: {}", partTwo);
		// validate server response part 2, not really required for client, but just to show off ;)
		byte[] firstFourBytes = getFourBytesFrom(partTwo, 0);
		if (Arrays.equals(new byte[] { 0, 0, 0, 0 }, firstFourBytes)) {
			log.warn("Server response part 2 first four bytes are zero, did handshake fail ?");
		}
		message = new byte[Constants.HANDSHAKE_SIZE - RTMPHandshake.DIGEST_LENGTH];
		partTwo.get(message);
		digest = calculateHMAC_SHA256(outgoingDigest, RTMPHandshake.GENUINE_FMS_KEY);
		byte[] signature = calculateHMAC_SHA256(message, digest);
		byte[] serverSignature = new byte[RTMPHandshake.DIGEST_LENGTH];
		partTwo.get(serverSignature);
		if (Arrays.equals(signature, serverSignature)) {
			log.info("server response part 2 validation success, is Flash Player v9 handshake");
		} else {
			log.warn("server response part 2 validation failed, not Flash Player v9 handshake");
		}
		// swf verification
		if (swfHash != null) {
			byte[] bytesFromServer = new byte[RTMPHandshake.DIGEST_LENGTH];
			buf.position(Constants.HANDSHAKE_SIZE - RTMPHandshake.DIGEST_LENGTH);
			buf.get(bytesFromServer);
			byte[] bytesFromServerHash = calculateHMAC_SHA256(swfHash, bytesFromServer);
			// construct SWF verification pong payload
			IoBuffer swfv = IoBuffer.allocate(42);
			swfv.put((byte) 0x01);
			swfv.put((byte) 0x01);
			swfv.putInt(swfSize);
			swfv.putInt(swfSize);
			swfv.put(bytesFromServerHash);
			swfVerificationBytes = new byte[42];
			swfv.flip();
			swfv.get(swfVerificationBytes);
			log.info("initialized swf verification response from swfSize: {} & swfHash: {} = {}",
					new Object[] { swfSize, Hex.encodeHexString(swfHash), Hex.encodeHexString(swfVerificationBytes) });
		}
		return true;
	}

	public IoBuffer generateClientRequest2() {
		log.debug("generateClientRequest2");
		byte[] randomBytes = new byte[Constants.HANDSHAKE_SIZE];
		random.nextBytes(randomBytes);
		IoBuffer buf = IoBuffer.wrap(randomBytes);
		byte[] digest = calculateHMAC_SHA256(incomingDigest, RTMPHandshake.GENUINE_FP_KEY);
		byte[] message = new byte[Constants.HANDSHAKE_SIZE - RTMPHandshake.DIGEST_LENGTH];
		buf.rewind();
		buf.get(message);
		byte[] signature = calculateHMAC_SHA256(message, digest);
		buf.put(signature);
		buf.rewind();
		if (handshakeType == RTMPConnection.RTMP_ENCRYPTED) {
			// update 'encoder / decoder state' for the RC4 keys. Both parties *pretend* as if handshake part 2 (1536 bytes) was encrypted
			// effectively this hides / discards the first few bytes of encrypted session which is known to increase the secure-ness of RC4
			// RC4 state is just a function of number of bytes processed so far that's why we just run 1536 arbitrary bytes through the keys below
			byte[] dummyBytes = new byte[Constants.HANDSHAKE_SIZE];
			cipherIn.update(dummyBytes);
			cipherOut.update(dummyBytes);
		}
		return buf;
	}

	private int addBytes(byte[] bytes) {
		if (bytes.length != 4) {
			throw new RuntimeException("Unexpected byte array size: " + bytes.length);
		}
		int result = 0;
		for (int i = 0; i < bytes.length; i++) {
			result += bytes[i] & 0xff;
		}
		return result;
	}

	private int calculateOffset(byte[] pointer, int modulus, int increment) {
		int offset = addBytes(pointer);
		offset %= modulus;
		offset += increment;
		return offset;
	}

	protected byte[] getFourBytesFrom(IoBuffer buf, int offset) {
		int initial = buf.position();
		buf.position(offset);
		byte[] bytes = new byte[4];
		buf.get(bytes);
		buf.position(initial);
		return bytes;
	}

	/**
	 * Determines the validation scheme for given input.
	 * 
	 * @param input
	 * @return true if client used a supported validation scheme, false if unsupported
	 */
	@Override
	public boolean validate(IoBuffer input) {
		byte[] pBuffer = new byte[input.remaining()];
		//put all the input bytes into our buffer
		input.get(pBuffer, 0, input.remaining());
		if (validateScheme(pBuffer, 0)) {
			validationScheme = 0;
			log.debug("Selected scheme: 0");
			return true;
		}
		if (validateScheme(pBuffer, 1)) {
			validationScheme = 1;
			log.debug("Selected scheme: 1");
			return true;
		}
		log.error("Unable to validate client");
		return false;
	}

	private boolean validateScheme(byte[] pBuffer, int scheme) {
		int digestOffset = -1;
		switch (scheme) {
			case 0:
				digestOffset = getDigestOffset0(pBuffer);
				break;
			case 1:
				digestOffset = getDigestOffset1(pBuffer);
				break;
			default:
				log.error("Unknown scheme: {}", scheme);
		}
		log.debug("Scheme: {} client digest offset: {}", scheme, digestOffset);

		byte[] tempBuffer = new byte[Constants.HANDSHAKE_SIZE - DIGEST_LENGTH];
		System.arraycopy(pBuffer, 0, tempBuffer, 0, digestOffset);
		System.arraycopy(pBuffer, digestOffset + DIGEST_LENGTH, tempBuffer, digestOffset, Constants.HANDSHAKE_SIZE - digestOffset - DIGEST_LENGTH);

		byte[] tempHash = calculateHMAC_SHA256(tempBuffer, GENUINE_FP_KEY, 30);
		log.debug("Temp: {}", Hex.encodeHexString(tempHash));

		boolean result = true;
		for (int i = 0; i < DIGEST_LENGTH; i++) {
			//log.trace("Digest: {} Temp: {}", (pBuffer[digestOffset + i] & 0x0ff), (tempHash[i] & 0x0ff));
			if (pBuffer[digestOffset + i] != tempHash[i]) {
				result = false;
				break;
			}
		}

		return result;
	}

	/**
	 * Initialize SWF verification data.
	 * 
	 * @param swfFilePath path to the swf file or null
	 */
	public void initSwfVerification(String swfFilePath) {
		log.info("Initializing swf verification for: {}", swfFilePath);
		byte[] bytes = null;
		if (swfFilePath != null) {
			File localSwfFile = new File(swfFilePath);
			if (localSwfFile.exists() && localSwfFile.canRead()) {
				log.info("Swf file path: {}", localSwfFile.getAbsolutePath());
				bytes = FileUtil.readAsByteArray(localSwfFile);
			} else {
				bytes = "Red5 is awesome for handling non-accessable swf file".getBytes();
			}
		} else {
			bytes = new byte[42];
		}
		swfHash = calculateHMAC_SHA256(bytes, CLIENT_CONST, 30);
		swfSize = bytes.length;
		log.info("Verification - size: {}, hash: {}", swfSize, Hex.encodeHexString(swfHash));
	}

}
