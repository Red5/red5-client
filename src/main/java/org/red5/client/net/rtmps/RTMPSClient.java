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

package org.red5.client.net.rtmps;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.NotActiveException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.client.net.rtmp.RTMPMinaIoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPS client object
 * 
 * @author The Red5 Project 
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Kevin Green (kevygreen@gmail.com)
 */
public class RTMPSClient extends RTMPClient {

	private static final Logger log = LoggerFactory.getLogger(RTMPSClient.class);

	// I/O handler
	private final RTMPSClientIoHandler ioHandler;

	/**
	 * Password for accessing the keystore.
	 */
	private char[] password;

	/**
	 * Stores the keystore file bytes.
	 */
	private byte[] keystore;

	/**
	 * The keystore type, valid options are JKS and PKCS12
	 */
	private String keyStoreType = "JKS";

	/** Constructs a new RTMPClient. */
	public RTMPSClient() {
		ioHandler = new RTMPSClientIoHandler();
		ioHandler.setHandler(this);
	}

	public Map<String, Object> makeDefaultConnectionParams(String server, int port, String application) {
		Map<String, Object> params = super.makeDefaultConnectionParams(server, port, application);
		if (!params.containsKey("tcUrl")) {
			params.put("tcUrl", String.format("rtmps://%s:%s/%s", server, port, application));
		}
		return params;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected void startConnector(String server, int port) {
		socketConnector = new NioSocketConnector();
		socketConnector.setHandler(ioHandler);
		future = socketConnector.connect(new InetSocketAddress(server, port));
		future.addListener(new IoFutureListener() {
			public void operationComplete(IoFuture future) {
				try {
					// will throw RuntimeException after connection error
					future.getSession();
				} catch (Throwable e) {
					//if there isn't an ClientExceptionHandler set, a 
					//RuntimeException may be thrown in handleException
					handleException(e);
				}
			}
		});
		// Do the close requesting that the pending messages are sent before
		// the session is closed
		//future.getSession().close(false);
		// Now wait for the close to be completed
		future.awaitUninterruptibly(CONNECTOR_WORKER_TIMEOUT);
		// We can now dispose the connector
		//socketConnector.dispose();
	}

	/**
	 * Password used to access the keystore file.
	 * 
	 * @param password
	 */
	public void setKeyStorePassword(String password) {
		this.password = password.toCharArray();
	}

	/**
	 * Set keystore data from a file.
	 * 
	 * @param path contains keystore
	 */
	public void setKeystoreFile(String path) {
		FileInputStream fis = null;
		try {
			File file = new File(path);
			if (file.exists()) {
				fis = new FileInputStream(file);
				FileChannel fc = fis.getChannel();
				ByteBuffer fb = ByteBuffer.allocate(Long.valueOf(file.length()).intValue());
				fc.read(fb);
				fb.flip();
				keystore = IoBuffer.wrap(fb).array();
			} else {
				log.warn("Keystore file does not exist: {}", path);
			}
			file = null;
		} catch (Exception e) {
			log.warn("Error setting keystore data", e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Set keystore data from a file.
	 * 
	 * @param arr keystore bytes
	 */
	public void setKeystoreBytes(byte[] arr) {
		keystore = new byte[arr.length];
		System.arraycopy(arr, 0, keystore, 0, arr.length);
	}

	/**
	 * Set the key store type, JKS or PKCS12.
	 * 
	 * @param keyStoreType
	 */
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	private class RTMPSClientIoHandler extends RTMPMinaIoHandler {

		// Create a trust manager that does not validate certificate chains
		private final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		/** {@inheritDoc} */
		@Override
		public void sessionOpened(IoSession session) throws Exception {
			if (password == null || keystore == null) {
				throw new NotActiveException("Keystore or password are null");
			}
			// START OF NATIVE SSL STUFF
			//install the all-trusting trust manager
			SSLContext context = SSLContext.getInstance("SSL");
			context.init(null, trustAllCerts, new SecureRandom());
			//create the ssl filter using client mode
			SslFilter sslFilter = new SslFilter(context);
			sslFilter.setUseClientMode(true);
			if (sslFilter != null) {
				session.getFilterChain().addFirst("sslFilter", sslFilter);
			}
			// END OF NATIVE SSL STUFF
			super.sessionOpened(session);
		}

		/** {@inheritDoc} */
		@Override
		public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
			log.warn("Exception caught {}", cause.getMessage());
			if (log.isDebugEnabled()) {
				log.error("Exception detail", cause);
			}
			//if there are any errors using ssl, kill the session
			session.close(true);
		}

		/**
		 * Returns a KeyStore.
		 * @return KeyStore
		 * @throws IOException 
		 * @throws CertificateException 
		 * @throws NoSuchAlgorithmException 
		 * @throws KeyStoreException 
		 */
		@SuppressWarnings("unused")
		private KeyStore getKeyStore() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
			// Sun's default kind of key store
			KeyStore ks = KeyStore.getInstance(keyStoreType);
			// For security, every key store is encrypted with a
			// pass phrase that must be provided before we can load
			// it from disk. The pass phrase is stored as a char[] array
			// so it can be wiped from memory quickly rather than
			// waiting for a garbage collector. Of course using a string
			// literal here completely defeats that purpose.
			ks.load(new ByteArrayInputStream(keystore), password);
			return ks;
		}

	}

}
