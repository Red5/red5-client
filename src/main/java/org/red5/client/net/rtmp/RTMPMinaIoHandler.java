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

import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.red5.client.net.rtmpe.RTMPEIoFilter;
import org.red5.io.object.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.BufferUtils;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.ICommand;
import org.red5.server.net.protocol.RTMPDecodeState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPMinaProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPMinaProtocolEncoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.service.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		//add rtmpe filter
		session.getFilterChain().addFirst("rtmpeFilter", new RTMPEIoFilter());
		//add protocol filter next
		session.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(new RTMPMinaCodecFactory()));
		//create a connection
		RTMPMinaConnection conn = createRTMPMinaConnection();
		// set the session on the connection
		conn.setIoSession(session);
		//add the connection
		session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
		// create an outbound handshake
		OutboundHandshake outgoingHandshake = new OutboundHandshake();
		// set the handshake type
		outgoingHandshake.setHandshakeType(RTMPConnection.RTMP_NON_ENCRYPTED);
		// setup swf verification
		if (handler.isSwfVerification()) {
			outgoingHandshake.initSwfVerification((String) handler.getConnectionParams().get("swfUrl"));
		}
		//if handler is rtmpe client set encryption on the protocol state
		//if (handler instanceof RTMPEClient) {
		//rtmp.setEncrypted(true);
		//set the handshake type to encrypted as well
		//outgoingHandshake.setHandshakeType(RTMPConnection.RTMP_ENCRYPTED);
		//}
		// set a reference to the connection on the client
		handler.setConnection((RTMPConnection) conn);
		//add the handshake
		session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, outgoingHandshake);
	}

	/** {@inheritDoc} */
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		log.debug("Session opened");
		super.sessionOpened(session);
		log.debug("Handshake - client phase 1");
		//get the handshake from the session
		RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
		IoBuffer clientRequest1 = handshake.doHandshake(null);
		session.write(clientRequest1);
	}

	/** {@inheritDoc} */
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		log.debug("Session closed");
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		if (sessionId != null) {
			log.trace("Session id: {}", sessionId);
			RTMPMinaConnection conn = (RTMPMinaConnection) RTMPClientConnManager.getInstance().getConnectionBySessionId(sessionId);
			if (conn != null) {
				conn.sendPendingServiceCallsCloseError();
				// fire-off closed event
				handler.connectionClosed(conn);
				// clear any session attributes we may have previously set
				// TODO: verify this cleanup code is necessary. The session is over and will be garbage collected surely?
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

	/**
	 * Handle raw buffer receiving event.
	 *
	 * @param in
	 *            Data buffer
	 * @param session
	 *            I/O session, that is, connection between two endpoints
	 */
	protected void rawBufferRecieved(IoBuffer in, IoSession session) {
		log.debug("rawBufferRecieved: {}", in);
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		log.trace("Session id: {}", sessionId);
		RTMPMinaConnection conn = (RTMPMinaConnection) RTMPClientConnManager.getInstance().getConnectionBySessionId(sessionId);
		RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
		if (handshake != null) {
			log.debug("Handshake - client phase 2 - size: {}", in.remaining());
			IoBuffer out = handshake.doHandshake(in);
			if (out != null) {
				log.debug("Output: {}", out);
				session.write(out);
				// if we are using encryption then put the ciphers in the session
				if (handshake.getHandshakeType() == RTMPConnection.RTMP_ENCRYPTED) {
					log.debug("Adding ciphers to the session");
					session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
					session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
				}
				// update the state to connected
				conn.setStateCode(RTMP.STATE_CONNECTED);
			}
		} else {
			log.warn("Handshake was not found for this connection: {}", conn);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		log.debug("messageReceived");
		if (message instanceof IoBuffer) {
			rawBufferRecieved((IoBuffer) message, session);
		} else {
			String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
			log.trace("Session id: {}", sessionId);
			RTMPMinaConnection conn = (RTMPMinaConnection) RTMPClientConnManager.getInstance().getConnectionBySessionId(sessionId);
			conn.handleMessageReceived((Packet) message);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		log.debug("messageSent");
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		log.trace("Session id: {}", sessionId);
		RTMPMinaConnection conn = (RTMPMinaConnection) RTMPClientConnManager.getInstance().getConnectionBySessionId(sessionId);
		if (message instanceof IoBuffer) {
			if (((IoBuffer) message).limit() == Constants.HANDSHAKE_SIZE) {
				handler.connectionOpened(conn);
			}
		} else {
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
		return (RTMPMinaConnection) RTMPClientConnManager.getInstance().createConnection(RTMPMinaConnection.class);
	}

	public class RTMPMinaCodecFactory implements ProtocolCodecFactory {

		private RTMPMinaProtocolDecoder clientDecoder = new RTMPMinaProtocolDecoder();

		private RTMPMinaProtocolEncoder clientEncoder = new RTMPMinaProtocolEncoder();

		{
			// RTMP Decoding
			clientDecoder = new RTMPMinaProtocolDecoder() {
				/** {@inheritDoc} */
				@Override
				public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws ProtocolCodecException {
					// create a buffer and store it on the session
					IoBuffer buf = (IoBuffer) session.getAttribute("buffer");
					if (buf == null) {
						buf = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
						buf.setAutoExpand(true);
						session.setAttribute("buffer", buf);
					}
					buf.put(in);
					buf.flip();
					// get the connection from the session
					String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
					log.trace("Session id: {}", sessionId);
					RTMPConnection conn = (RTMPConnection) RTMPClientConnManager.getInstance().getConnectionBySessionId(sessionId);
					Red5.setConnectionLocal(conn);
					final Semaphore lock = conn.getDecoderLock();
					try {
						// acquire the decoder lock
						log.trace("Decoder lock acquiring.. {}", sessionId);
						lock.acquire();
						log.trace("Decoder lock acquired {}", sessionId);
						// construct any objects from the decoded bugger
						List<?> objects = getDecoder().decodeBuffer(conn, buf);
						if (objects != null) {
							for (Object object : objects) {
								out.write(object);
							}
						}
					} catch (Exception e) {
						log.error("Error during decode", e);
					} finally {
						log.trace("Decoder lock releasing.. {}", sessionId);
						lock.release();
						Red5.setConnectionLocal(null);
					}
				}
			};
			clientDecoder.setDecoder(new RTMPClientProtocolDecoder());
			// RTMP Encoding
			clientEncoder = new RTMPMinaProtocolEncoder() {
				/** {@inheritDoc} */
				@Override
				public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws ProtocolCodecException {
					// get the connection from the session
					String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
					log.trace("Session id: {}", sessionId);
					RTMPConnection conn = (RTMPConnection) RTMPClientConnManager.getInstance().getConnectionBySessionId(sessionId);
					if (conn != null) {
						Red5.setConnectionLocal(conn);
						final Semaphore lock = conn.getEncoderLock();
						try {
							// acquire the decoder lock
							log.trace("Encoder lock acquiring.. {}", sessionId);
							lock.acquire();
							log.trace("Encoder lock acquired {}", sessionId);
							// get the buffer
							final IoBuffer buf = message instanceof IoBuffer ? (IoBuffer) message : getEncoder().encode(message);
							if (buf != null) {
								log.trace("Writing output data");
								out.write(buf);
							} else {
								log.trace("Response buffer was null after encoding");
							}
						} catch (Exception ex) {
							log.error("Exception during encode", ex);
						} finally {
							log.trace("Encoder lock releasing.. {}", sessionId);
							lock.release();
							Red5.setConnectionLocal(null);
						}
					} else {
						log.debug("Connection is no longer available for encoding, may have been closed already");
					}
				}
			};
			clientEncoder.setEncoder(new RTMPClientProtocolEncoder());
			// two other config options are available
			//clientEncoder.setBaseTolerance(baseTolerance);
			//clientEncoder.setDropLiveFuture(dropLiveFuture);
		}

		/** {@inheritDoc} */
		public ProtocolDecoder getDecoder(IoSession session) {
			return clientDecoder;
		}

		/** {@inheritDoc} */
		public ProtocolEncoder getEncoder(IoSession session) {
			return clientEncoder;
		}

	}

	/**
	 * Class to specifically handle the client side of the handshake routine.
	 */
	public class RTMPClientProtocolDecoder extends RTMPProtocolDecoder {

		private static final int HANDSHAKE_SERVER_SIZE = (HANDSHAKE_SIZE * 2);

		/**
		 * Decodes server handshake message.
		 * 
		 * @param in IoBuffer
		 * @return IoBuffer
		 */
		public IoBuffer decodeHandshake(IoBuffer in) {
			log.debug("decodeServerHandshake - buffer: {}", in);
			RTMPConnection conn = (RTMPConnection) Red5.getConnectionLocal();
			// get the local decode state
			RTMPDecodeState state = conn.getDecoderState();
			final int remaining = in.remaining();
			if (conn.getStateCode() == RTMP.STATE_CONNECT) {
				if (remaining < HANDSHAKE_SERVER_SIZE + 1) {
					log.debug("Handshake init too small, buffering. remaining: {}", remaining);
					state.bufferDecoding(HANDSHAKE_SERVER_SIZE + 1);
				} else {
					final IoBuffer hs = IoBuffer.allocate(HANDSHAKE_SERVER_SIZE);
					in.get(); // skip the header byte
					BufferUtils.put(hs, in, HANDSHAKE_SERVER_SIZE);
					hs.flip();
					conn.getState().setState(RTMP.STATE_HANDSHAKE);
					return hs;
				}
			} else if (conn.getStateCode() == RTMP.STATE_HANDSHAKE) {
				log.debug("Handshake reply");
				if (remaining < HANDSHAKE_SERVER_SIZE) {
					log.debug("Handshake reply too small, buffering. remaining: {}", remaining);
					state.bufferDecoding(HANDSHAKE_SERVER_SIZE);
				} else {
					in.skip(HANDSHAKE_SERVER_SIZE);
					conn.getState().setState(RTMP.STATE_CONNECTED);
					state.continueDecoding();
				}
			}
			return null;
		}
	}

	/**
	 * Class to specifically handle client side situations.
	 */
	public class RTMPClientProtocolEncoder extends RTMPProtocolEncoder {

		/**
		 * Encode notification event and fill given byte buffer.
		 *
		 * @param out               Byte buffer to fill
		 * @param invoke            Notification event
		 */
		@Override
		protected void encodeCommand(IoBuffer out, ICommand command) {
			log.debug("encodeCommand - command: {}", command);
			RTMPConnection conn = (RTMPConnection) Red5.getConnectionLocal();
			Output output = new org.red5.io.amf.Output(out);
			final IServiceCall call = command.getCall();
			final boolean isPending = (call.getStatus() == Call.STATUS_PENDING);
			log.debug("Call: {} pending: {}", call, isPending);
			if (!isPending) {
				log.debug("Call has been executed, send result");
				Serializer.serialize(output, call.isSuccess() ? "_result" : "_error");
			} else {
				log.debug("This is a pending call, send request");
				// for request we need to use AMF3 for client mode if the connection is AMF3
				if (conn.getEncoding() == Encoding.AMF3) {
					output = new org.red5.io.amf3.Output(out);
				}
				final String action = (call.getServiceName() == null) ? call.getServiceMethodName() : call.getServiceName() + '.' + call.getServiceMethodName();
				Serializer.serialize(output, action);
			}
			if (command instanceof Invoke) {
				Serializer.serialize(output, Integer.valueOf(command.getTransactionId()));
				Serializer.serialize(output, command.getConnectionParams());
			}
			if (call.getServiceName() == null && "connect".equals(call.getServiceMethodName())) {
				// response to initial connect, always use AMF0
				output = new org.red5.io.amf.Output(out);
			} else {
				if (conn.getEncoding() == Encoding.AMF3) {
					output = new org.red5.io.amf3.Output(out);
				} else {
					output = new org.red5.io.amf.Output(out);
				}
			}
			if (!isPending && (command instanceof Invoke)) {
				IPendingServiceCall pendingCall = (IPendingServiceCall) call;
				if (!call.isSuccess()) {
					log.debug("Call was not successful");
					StatusObject status = generateErrorResult(StatusCodes.NC_CALL_FAILED, call.getException());
					pendingCall.setResult(status);
				}
				Object res = pendingCall.getResult();
				log.debug("Writing result: {}", res);
				Serializer.serialize(output, res);
			} else {
				log.debug("Writing params");
				final Object[] args = call.getArguments();
				if (args != null) {
					for (Object element : args) {
						Serializer.serialize(output, element);
					}
				}
			}
			if (command.getData() != null) {
				out.setAutoExpand(true);
				out.put(command.getData());
			}
		}
	}

}
