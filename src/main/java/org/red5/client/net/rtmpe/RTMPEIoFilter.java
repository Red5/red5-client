/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

package org.red5.client.net.rtmpe;

import javax.crypto.Cipher;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.client.net.rtmp.OutboundHandshake;
import org.red5.client.net.rtmp.RTMPConnManager;
import org.red5.client.net.rtmp.codec.RTMPMinaCodecFactory;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPE IO filter - Client version.
 * 
 * @author Peter Thomas (ptrthomas@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPEIoFilter extends IoFilterAdapter {

    private static final Logger log = LoggerFactory.getLogger(RTMPEIoFilter.class);

    private ThreadLocal<byte[]> buffer; 
    
    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.trace("Session id: {}", sessionId);
        RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
        if (log.isTraceEnabled()) {
            log.trace("Bytes read: {} written: {}", conn.getReadBytes(), conn.getWrittenBytes());
        }
        // filter based on current connection state
        RTMP rtmp = conn.getState();
        final byte connectionState = conn.getStateCode();
        // assume message is an IoBuffer
        IoBuffer message = (IoBuffer) obj;
        // client handshake handling
        OutboundHandshake handshake = null;
        int remaining = 0;
        switch (connectionState) {
            case RTMP.STATE_CONNECTED:
                Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN);
                if (cipher != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Decrypting message: {}", message);
                    }
                    byte[] encrypted = new byte[message.remaining()];
                    message.get(encrypted);
                    message.clear();
                    message.free();
                    byte[] plain = cipher.update(encrypted);
                    IoBuffer messageDecrypted = IoBuffer.wrap(plain);
                    if (log.isDebugEnabled()) {
                        log.debug("Decrypted buffer: {}", messageDecrypted);
                    }
                    nextFilter.messageReceived(session, messageDecrypted);
                } else {
                    log.trace("Not decrypting message: {}", obj);
                    nextFilter.messageReceived(session, obj);
                }
                break;
            case RTMP.STATE_CONNECT:
                // we're expecting S0+S1 here
                log.trace("S0S1 byte order: {}", message.order());
                // get the handshake from the session
                handshake = (OutboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                int handshakeType = handshake.getHandshakeType();
                if (handshakeType == 0) {
                    log.trace("Handshake type is not currently set");
                    // holds the handshake type, default is un-encrypted
                    byte handshakeByte = RTMPConnection.RTMP_NON_ENCRYPTED;
                    message.mark();
                    handshakeByte = message.get();
                    message.reset();
                    // set the type
                    handshake.setHandshakeType(handshakeByte);
                    // set encryption flag the rtmp state
                    rtmp.setEncrypted(handshake.useEncryption());
                }
                log.debug("decodeHandshakeS0S1 - buffer: {}", message);
                // we want 1537 bytes for S0S1
                remaining = message.remaining();
                log.trace("Incoming S0S1 size: {}", remaining);
                if (remaining >= (Constants.HANDSHAKE_SIZE + 1)) {
                    // get the connection type byte, may want to set this on the conn in the future
                    byte connectionType = message.get();
                    log.trace("Incoming S0 connection type: {}", connectionType);
                    // TODO ensure handshake types match
                    if (handshake.getHandshakeType() != connectionType) {
                        log.debug("Server requested handshake type: {} client requested: {}", connectionType, handshake.getHandshakeType());
                    }
                    // create array for decode
                    byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                    // copy out 1536 bytes
                    message.get(dst);
                    log.debug("S1 - buffer: {}", Hex.encodeHexString(dst));
                    // set state to indicate we're waiting for S2
                    conn.getState().setState(RTMP.STATE_HANDSHAKE);
                    // buffer any extra bytes
                    remaining = message.remaining();
                    if (log.isTraceEnabled()) {
                        log.trace("Incoming S1 remaining size: {}", remaining);
                    }
                    if (remaining > 0) {
                        // store the remaining bytes in a thread local for use by S2 decoding
                        byte[] remainder = new byte[remaining];
                        message.get(remainder);
                        buffer = new ThreadLocal<>();
                        buffer.set(remainder);
                        log.trace("Stored {} bytes for later decoding", remaining);
                    }
                    IoBuffer c2 = handshake.decodeServerResponse1(IoBuffer.wrap(dst));
                    if (c2 != null) {
                        log.trace("C2 byte order: {}", c2.order());
                        session.write(c2);
                    } else {
                        conn.close();
                    }
                }
                break;
            case RTMP.STATE_HANDSHAKE:
                // we're expecting S2 here
                log.trace("S2 byte order: {}", message.order());
                // get the handshake from the session
                handshake = (OutboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                log.debug("decodeHandshakeS2 - buffer: {}", message);
                remaining = message.remaining();
                // check for remaining stored bytes left over from S0S1
                if (buffer != null) {
                    remaining += buffer.get().length;
                    log.trace("Remainder: {}", Hex.encodeHexString(buffer.get()));
                }
                log.trace("Incoming S2 size: {}", remaining);
                if (remaining >= (Constants.HANDSHAKE_SIZE + 1)) {
                    // create array for decode
                    byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                    // get the connection type byte, may want to set this on the conn in the future
                    byte connectionType = 0;
                    // check for remaining stored bytes left over from S0S1 and prepend to the dst array
                    if (buffer != null) {
                        byte[] remainder = buffer.get();
                        connectionType = remainder[0];
                        // copy into dst
                        System.arraycopy(remainder, 1, dst, 0, remainder.length - 1);
                        // copy
                        message.get(dst, remainder.length, message.limit());
                        // reset threadlocal
                        buffer.set(null);
                    } else {
                        connectionType = message.get();
                        // copy
                        message.get(dst);
                    }
                    log.trace("Incoming S2 connection type: {}", connectionType);
                    log.debug("S2 - buffer: {}", Hex.encodeHexString(dst));
                    // buffer any extra bytes
                    remaining = message.remaining();
                    if (log.isTraceEnabled()) {
                        log.trace("Incoming S2 remaining size: {}", remaining);
                    }
                    if (handshake.decodeServerResponse2(IoBuffer.wrap(dst))) {
                        log.debug("S2 decoding successful");
                    } else {
                        log.debug("S2 decoding failed");
                    }
                    // set state to indicate we're connected
                    conn.getState().setState(RTMP.STATE_CONNECTED);
                    log.debug("Connected, removing handshake data");
                    // remove handshake from session now that we are connected
                    session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                    // add protocol filter as the last one in the chain
                    log.debug("Adding RTMP protocol filter");
                    session.getFilterChain().addAfter("rtmpeFilter", "protocolFilter", new ProtocolCodecFilter(new RTMPMinaCodecFactory()));
                    if (handshake.useEncryption()) {
                        // set encryption flag the rtmp state
                        rtmp.setEncrypted(true);
                        // ensure we have received enough bytes to be encrypted
                        long readBytesCount = conn.getReadBytes();
                        long writeBytesCount = conn.getWrittenBytes();
                        log.trace("Bytes read: {} written: {}", readBytesCount, writeBytesCount);
                        // don't remove the handshake when using RTMPE until we've written all the handshake data
                        if (writeBytesCount >= (Constants.HANDSHAKE_SIZE * 2)) {
                            // if we are connected and doing encryption, add the ciphers
                            log.debug("Adding ciphers to the session");
                            session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
                            session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
                        }
                    }
                    // flag to indicate that we're read to be an opened rtmp client
                    session.setAttribute(OutboundHandshake.RTMP_HANDSHAKE_COMPLETED, null);
                    // compact here?
                    message.compact();
                    // nothing to send to the server after we receive s2
                    nextFilter.messageReceived(session, message);
                }
                break;
            case RTMP.STATE_ERROR:
            case RTMP.STATE_DISCONNECTING:
            case RTMP.STATE_DISCONNECTED:
                // do nothing, really
                log.debug("Nothing to do, connection state: {}", RTMP.states[connectionState]);
                break;
            default:
                throw new IllegalStateException("Invalid RTMP state: " + connectionState);
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
        RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId((String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID));
        // filter based on current connection state
        if (conn.getState().getState() == RTMP.STATE_CONNECTED && session.containsAttribute(RTMPConnection.RTMPE_CIPHER_OUT)) {
            Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
            IoBuffer message = (IoBuffer) request.getMessage();
            if (!message.hasRemaining()) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring empty message");
                }
                nextFilter.filterWrite(session, request);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Encrypting message: {}", message);
                }
                byte[] plain = new byte[message.remaining()];
                message.get(plain);
                message.clear();
                message.free();
                // encrypt and write
                byte[] encrypted = cipher.update(plain);
                IoBuffer messageEncrypted = IoBuffer.wrap(encrypted);
                if (log.isDebugEnabled()) {
                    log.debug("Encrypted message: {}", messageEncrypted);
                }
                nextFilter.filterWrite(session, new EncryptedWriteRequest(request, messageEncrypted));
            }
        } else {
            log.trace("Non-encrypted message");
            nextFilter.filterWrite(session, request);
        }
    }

    private static class EncryptedWriteRequest extends WriteRequestWrapper {
        private final IoBuffer encryptedMessage;

        private EncryptedWriteRequest(WriteRequest writeRequest, IoBuffer encryptedMessage) {
            super(writeRequest);
            this.encryptedMessage = encryptedMessage;
        }

        @Override
        public Object getMessage() {
            return encryptedMessage;
        }
    }

}
