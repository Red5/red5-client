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

package org.red5.client.net.rtmpt;

import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.client.net.rtmp.BaseRTMPClientHandler;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmpt.codec.RTMPTCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPT client object
 * 
 * @author Anton Lebedevich
 */
public class RTMPTClient extends BaseRTMPClientHandler {

	private static final Logger log = LoggerFactory.getLogger(RTMPTClient.class);

	// guarded by this
	private RTMPTClientConnector connector;

	private RTMPTCodecFactory codecFactory;

	public RTMPTClient() {
		protocol = "rtmpt";
		codecFactory = new RTMPTCodecFactory();
		codecFactory.init();
	}

	public Map<String, Object> makeDefaultConnectionParams(String server, int port, String application) {
		Map<String, Object> params = super.makeDefaultConnectionParams(server, port, application);
		if (!params.containsKey("tcUrl")) {
			params.put("tcUrl", protocol + "://" + server + ':' + port + '/' + application);
		}
		return params;
	}

	protected synchronized void startConnector(String server, int port) {
		connector = new RTMPTClientConnector(server, port, this);
		log.debug("Created connector {}", connector);
		connector.start();
	}

	/**
	 * Received message object router.
	 * 
	 * @param message an IoBuffer or Packet
	 */
	public void messageReceived(Object message) {
		if (message instanceof Packet) {
			try {
				messageReceived(conn, (Packet) message);
			} catch (Exception e) {
				log.warn("Exception on packet receive", e);
			}
		} else {
			IoBuffer in = (IoBuffer) message;
			log.debug("Handshake 3d phase - size: {}", in.remaining());
			IoBuffer out = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
			out.setAutoExpand(true);
			log.debug("Skip first byte: {}", in.get());
			out.put(in);
			out.flip();
			conn.writeRaw(out);
			connectionOpened(conn);	
		}
	}		

	public synchronized void disconnect() {
		if (connector != null) {
			connector.setStopRequested(true);
			connector.interrupt();
		}
		super.disconnect();
	}

	public RTMPProtocolDecoder getDecoder() {
		return codecFactory.getRTMPDecoder();
	}

	public RTMPProtocolEncoder getEncoder() {
		return codecFactory.getRTMPEncoder();
	}

}
