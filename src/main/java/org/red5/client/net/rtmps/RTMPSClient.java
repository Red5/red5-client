/*
 * RED5 Open Source Flash Server - https://github.com/Red5/ Copyright 2006-2015 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.client.net.rtmps;

import javax.net.ssl.SSLContext;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.client.net.rtmp.RTMPMinaIoHandler;
import org.red5.client.net.ssl.BogusSslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPS client object (RTMPS Native)
 * 
 * <pre>
 * var nc:NetConnection = new NetConnection();
 * nc.proxyType = "best";
 * nc.connect("rtmps:\\localhost\app");
 * </pre>
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
    @SuppressWarnings("unused")
    private char[] password;

    /**
     * The keystore type, valid options are JKS and PKCS12
     */
    @SuppressWarnings("unused")
    private String keyStoreType = "JKS";

    /** Constructs a new RTMPClient. */
    public RTMPSClient() {
        protocol = "rtmps";
        ioHandler = new RTMPSClientIoHandler();
        ioHandler.setHandler(this);
    }

    /**
     * Password used to access the keystore file.
     * 
     * @param password keystore password
     */
    public void setKeyStorePassword(String password) {
        this.password = password.toCharArray();
    }

    /**
     * Set the key store type, JKS or PKCS12.
     * 
     * @param keyStoreType keystore type
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    private class RTMPSClientIoHandler extends RTMPMinaIoHandler {

        /** {@inheritDoc} */
        @Override
        public void sessionOpened(IoSession session) throws Exception {
            // START OF NATIVE SSL STUFF
            SSLContext sslContext = BogusSslContextFactory.getInstance(false);
            SslFilter sslFilter = new SslFilter(sslContext);
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
            session.closeNow();
        }

    }

}
