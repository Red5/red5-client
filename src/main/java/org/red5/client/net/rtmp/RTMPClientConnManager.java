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

import org.red5.client.net.rtmpt.RTMPTClientConnection;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class RTMPClientConnManager extends RTMPConnManager {

	private static final Logger log = LoggerFactory.getLogger(RTMPClientConnManager.class);

	private RTMPClientConnManager() {
	}

	public static RTMPClientConnManager getInstance() {
		if (instance == null) {
			instance = new RTMPClientConnManager();
		}
		return (RTMPClientConnManager) instance;
	}

	/**
	 * Creates a connection of the type specified.
	 * 
	 * @param connCls
	 */
	@Override
	public RTMPConnection createConnection(Class<?> connCls) {
		RTMPConnection conn = null;
		if (RTMPConnection.class.isAssignableFrom(connCls)) {
			try {
				// create connection
				conn = createConnectionInstance(connCls);
				// add to local map
				connMap.put(conn.getSessionId(), conn);
				log.trace("Connections: {}", conns.incrementAndGet());
				log.trace("Connection created: {}", conn);
			} catch (Exception ex) {
				log.warn("Exception creating connection", ex);
			}
		}
		return conn;
	}
	
	/**
	 * Creates a connection of the type specified with associated session id.
	 * 
	 * @param connCls
	 * @param sessionId
	 */	
	public RTMPConnection createConnection(Class<?> connCls, String sessionId) {
		RTMPConnection conn = null;
		if (RTMPConnection.class.isAssignableFrom(connCls)) {
			try {
				// create connection
				conn = createConnectionInstance(connCls);
				// set the session id
				if (conn instanceof RTMPTClientConnection) {
					((RTMPTClientConnection) conn).setSessionId(sessionId);
				}
				// add to local map
				connMap.put(conn.getSessionId(), conn);
				log.trace("Connections: {}", conns.incrementAndGet());
				log.trace("Connection created: {}", conn);
			} catch (Exception ex) {
				log.warn("Exception creating connection", ex);
			}
		}
		return conn;
	}		

	/**
	 * Creates a connection instance based on the supplied type.
	 * 
	 * @param cls
	 * @return connection
	 * @throws Exception
	 */
	@Override
	public RTMPConnection createConnectionInstance(Class<?> cls) throws Exception {
		RTMPConnection conn = null;
		if (cls == RTMPMinaConnection.class) {
			conn = (RTMPMinaConnection) cls.newInstance();
		} else if (cls == RTMPTConnection.class) {
			conn = (RTMPTClientConnection) cls.newInstance();
		} else {
			conn = (RTMPConnection) cls.newInstance();
		}
		conn.setMaxHandshakeTimeout(7000);
		conn.setMaxInactivity(60000);
		conn.setPingInterval(0);
		// setup executor
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setDaemon(true);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(16);
		executor.initialize();
		conn.setExecutor(executor);
		return conn;
	}

}
