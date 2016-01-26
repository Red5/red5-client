/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
 * 
 * Copyright 2006-2015 by respective authors (see below). All rights reserved.
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

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.red5.client.net.rtmpe.RTMPEIoFilter;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Handles all RTMP protocol events fired by the MINA framework.
 */
public class RTMPMinaIoHandler extends IoHandlerAdapter {

    private static Logger log = LoggerFactory.getLogger(RTMPMinaIoHandler.class);

    /**
     * RTMP events handler
     */
    protected BaseRTMPClientHandler handler;

    /** {@inheritDoc} */
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        log.debug("Session created");
        // add rtmpe filter, rtmp protocol filter is added upon successful handshake
        session.getFilterChain().addFirst("rtmpeFilter", new RTMPEIoFilter());
        // create a connection
        RTMPMinaConnection conn = createRTMPMinaConnection();
        // set the session on the connection
        conn.setIoSession(session);
        // add the connection
        session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
        // create an outbound handshake, defaults to non-encrypted mode
        OutboundHandshake outgoingHandshake = new OutboundHandshake();
        // add the handshake
        session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, outgoingHandshake);
        // setup swf verification
        String swfUrl = (String) handler.getConnectionParams().get("swfUrl");
        log.debug("SwfUrl: {}", swfUrl);
        if (!StringUtils.isEmpty(swfUrl)) {
            outgoingHandshake.initSwfVerification(swfUrl);
        }
        //if handler is rtmpe client set encryption on the protocol state
        //if (handler instanceof RTMPEClient) {
        //rtmp.setEncrypted(true);
        //set the handshake type to encrypted as well
        //outgoingHandshake.setHandshakeType(RTMPConnection.RTMP_ENCRYPTED);
        //}
        // set a reference to the connection on the client
        handler.setConnection((RTMPConnection) conn);
    }

    /** {@inheritDoc} */
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        log.debug("Session opened");
        super.sessionOpened(session);
        // get the handshake from the session
        RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
        // create and send C0+C1
        IoBuffer clientRequest1 = ((OutboundHandshake) handshake).generateClientRequest1();
        log.trace("C0C1 byte order: {}", clientRequest1.order());
        session.write(clientRequest1);
    }

    /** {@inheritDoc} */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        log.debug("Session closed");
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        if (sessionId != null) {
            log.trace("Session id: {}", sessionId);
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            if (conn != null) {
                conn.sendPendingServiceCallsCloseError();
                // fire-off closed event
                handler.connectionClosed(conn);
                // clear any session attributes we may have previously set
                session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                session.removeAttribute(RTMPConnection.RTMPE_CIPHER_IN);
                session.removeAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
            } else {
                log.warn("Connection was null in session");
            }
        } else {
            log.debug("Connections session id was null in session, may already be closed");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        log.debug("messageReceived");
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.trace("Session id: {}", sessionId);
        RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
        if (message instanceof IoBuffer) {
            if (log.isDebugEnabled()) {
                log.debug("rawBufferRecieved: {}", Hex.encodeHexString(((IoBuffer) message).array()));
            }
        } else {
            if (message != null) {
                conn.handleMessageReceived((Packet) message);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        log.debug("messageSent");
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.trace("Session id: {}", sessionId);
        if (message instanceof IoBuffer) {
            log.trace("messageSent: {}", Hex.encodeHexString(((IoBuffer) message).array()));
            // when handshaking is complete, the attribute will be removed and at that point we start RTMP communication
            // check and remove flag
            if (session.removeAttribute(OutboundHandshake.RTMP_HANDSHAKE_COMPLETED) != null) {
                RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
                handler.connectionOpened(conn);
            }
        } else {
            RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
            handler.messageSent(conn, (Packet) message);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        log.warn("Exception caught {}", cause.getMessage());
        if (log.isDebugEnabled()) {
            log.error("Exception detail", cause);
        }
    }

    /**
     * Setter for handler.
     *
     * @param handler RTMP events handler
     */
    public void setHandler(BaseRTMPClientHandler handler) {
        log.debug("Set handler: {}", handler);
        this.handler = handler;
    }

    protected RTMPMinaConnection createRTMPMinaConnection() {
        return (RTMPMinaConnection) RTMPConnManager.getInstance().createConnection(RTMPMinaConnection.class);
    }

}
