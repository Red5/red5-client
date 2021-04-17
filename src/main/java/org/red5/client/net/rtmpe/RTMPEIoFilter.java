/*
 * RED5 Open Source Flash Server - https://github.com/Red5/ Copyright 2006-2013 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.client.net.rtmpe;

import javax.crypto.Cipher;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.client.net.rtmp.BaseRTMPClientHandler;
import org.red5.client.net.rtmp.OutboundHandshake;
import org.red5.client.net.rtmp.RTMPConnManager;
import org.red5.client.net.rtmp.codec.RTMPMinaCodecFactory;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmpe.EncryptedWriteRequest;
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

    @Override
    public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.trace("Session id: {}", sessionId);
        RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
        if (conn == null) {
            throw new Exception("Receive on unavailable connection - session id: " + sessionId);
        }
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
        switch (connectionState) {
            case RTMP.STATE_CONNECTED:
                if (rtmp.isEncrypted()) {
                    Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN);
                    if (cipher != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Decrypting message: {}", message);
                        }
                        byte[] encrypted = new byte[message.remaining()];
                        message.get(encrypted);
                        message.free();
                        byte[] plain = cipher.update(encrypted);
                        IoBuffer messageDecrypted = IoBuffer.wrap(plain);
                        if (log.isDebugEnabled()) {
                            log.debug("Decrypted buffer: {}", messageDecrypted);
                        }
                        nextFilter.messageReceived(session, messageDecrypted);
                    } else {
                        log.warn("Decryption cipher is missing from the session");
                    }
                } else {
                    log.trace("Not decrypting message: {}", obj);
                    nextFilter.messageReceived(session, obj);
                }
                break;
            case RTMP.STATE_CONNECT:
                // get the handshake from the session and process S0+S1 if we have enough bytes
                handshake = (OutboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                // copy into the handshake buffer
                handshake.addBuffer(message);
                // we want 1537 bytes for S0S1
                int s0s1Size = handshake.getBufferSize();
                log.trace("Incoming S0S1 size: {}", s0s1Size);
                if (s0s1Size >= (Constants.HANDSHAKE_SIZE + 1)) {
                    log.debug("decodeHandshakeS0S1");
                    // check the initial handshake type
                    int handshakeType = handshake.getHandshakeType();
                    if (handshakeType == 0) {
                        log.trace("Handshake type is not currently set");
                        // set the type, default is un-encrypted
                        handshake.setHandshakeType(RTMPConnection.RTMP_NON_ENCRYPTED);
                        // set encryption flag the rtmp state
                        rtmp.setEncrypted(handshake.useEncryption());
                    }
                    // get the buffered bytes
                    IoBuffer buf = handshake.getBufferAsIoBuffer();
                    // get the connection type byte, may want to set this on the conn in the future
                    byte connectionType = buf.get();
                    log.trace("Incoming S0 connection type: {}", connectionType);
                    if (handshake.getHandshakeType() != connectionType) {
                        log.debug("Server requested handshake type: {} client requested: {}", connectionType, handshake.getHandshakeType());
                    }
                    // create array for decode
                    byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                    // copy out 1536 bytes
                    buf.get(dst);
                    //log.debug("S1 - buffer: {}", Hex.encodeHexString(dst));
                    // buffer any extra bytes
                    int remaining = buf.remaining();
                    if (remaining > 0) {
                        // store the remaining bytes in a thread local for use by S2 decoding
                        handshake.addBuffer(buf);
                        log.trace("Stored {} bytes for later decoding", remaining);
                    }
                    IoBuffer c2 = handshake.decodeServerResponse1(IoBuffer.wrap(dst));
                    if (c2 != null) {
                        // set state to indicate we're waiting for S2
                        rtmp.setState(RTMP.STATE_HANDSHAKE);
                        //log.trace("C2 byte order: {}", c2.order());
                        session.write(c2);
                        // if we got S0S1+S2 continue processing
                        if (handshake.getBufferSize() >= Constants.HANDSHAKE_SIZE) {
                            // clear
                            buf.clear();
                            // re-set
                            buf = handshake.getBufferAsIoBuffer();
                            if (handshake.decodeServerResponse2(buf)) {
                                log.debug("S2 decoding successful");
                            } else {
                                log.warn("Handshake failed on S2 processing");
                            }
                            completeConnection(session, conn, rtmp, handshake);
                        }
                    } else {
                        conn.close();
                    }
                }
                break;
            case RTMP.STATE_HANDSHAKE:
                // get the handshake from the session and process S2 if we have enough bytes
                handshake = (OutboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                log.debug("decodeHandshakeS2 - buffer: {}", message);
                // buffer the incoming message
                handshake.addBuffer(message);
                int s2Size = handshake.getBufferSize();
                log.trace("Incoming S2 size: {}", s2Size);
                if (s2Size >= Constants.HANDSHAKE_SIZE) {
                    // get the buffered bytes
                    IoBuffer buf = handshake.getBufferAsIoBuffer();
                    // create array for decode
                    byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                    // get S2 out
                    buf.get(dst);
                    int index = buf.indexOf(handshake.getHandshakeType());
                    if (index != -1) {
                        log.trace("Connection type index in message: {}", index);
                        buf.position(index);
                    }
                    log.trace("Message - pos: {} {}", buf.position(), message);
                    if (handshake.decodeServerResponse2(IoBuffer.wrap(dst))) {
                        log.debug("S2 decoding successful");
                    } else {
                        log.debug("S2 decoding failed");
                    }
                    // complete the connection regardless of the S2 success or failure
                    completeConnection(session, conn, rtmp, handshake);
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

    /**
     * Provides connection completion.
     * 
     * @param session
     * @param conn
     * @param rtmp
     * @param handshake
     */
    private static void completeConnection(IoSession session, RTMPMinaConnection conn, RTMP rtmp, OutboundHandshake handshake) {
        // set state to indicate we're connected
        rtmp.setState(RTMP.STATE_CONNECTED);
        // configure encryption
        if (handshake.useEncryption()) {
            log.debug("Connected, setting up encryption and removing handshake data");
            // set encryption flag the rtmp state
            rtmp.setEncrypted(true);
            // add the ciphers
            log.debug("Adding ciphers to the session");
            // seems counter intuitive, but it works 
            session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherOut());
            session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherIn());
            log.trace("Ciphers in: {} out: {}", handshake.getCipherIn(), handshake.getCipherOut());
        } else {
            log.debug("Connected, removing handshake data");
        }
        // remove handshake from session now that we are connected
        session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
        // add protocol filter as the last one in the chain
        log.debug("Adding RTMP protocol filter");
        session.getFilterChain().addAfter("rtmpeFilter", "protocolFilter", new ProtocolCodecFilter(new RTMPMinaCodecFactory()));
        // let the client know it may proceed
        BaseRTMPClientHandler handler = (BaseRTMPClientHandler) session.getAttribute(RTMPConnection.RTMP_HANDLER);
        handler.connectionOpened(conn);
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
        // grab the message
        Object message = request.getMessage();
        // if its bytes, we may encrypt thme
        if (message instanceof IoBuffer) {
            // filter based on current connection state
            Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
            if (cipher != null) {
                IoBuffer buf = (IoBuffer) message;
                int remaining = buf.remaining();
                if (remaining > 0) {
                    if (log.isDebugEnabled()) {
                        log.debug("Encrypting {} bytes, message: {}", remaining, buf);
                    }
                    byte[] plain = new byte[remaining];
                    buf.get(plain);
                    buf.free();
                    // encrypt and write
                    byte[] encrypted = cipher.update(plain);
                    buf = IoBuffer.wrap(encrypted);
                    if (log.isDebugEnabled()) {
                        log.debug("Encrypted message: {}", buf);
                    }
                }
                nextFilter.filterWrite(session, new EncryptedWriteRequest(request, buf));
            } else {
                log.trace("Non-encrypted message");
                nextFilter.filterWrite(session, request);
            }
        } else {
            log.trace("Passing through packet");
            nextFilter.filterWrite(session, request);
        }
    }

}
