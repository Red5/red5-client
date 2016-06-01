package org.red5.client.net.rtmps;

import org.junit.Test;
import org.red5.client.net.rtmp.ClientExceptionHandler;
import org.red5.client.util.PropertiesReader;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;

public class RTMPSClientTest {

    // https://github.com/Red5/red5-client/pull/31
    @Test
    public void test31() throws InterruptedException {
        final RTMPSClient client = new RTMPSClient();
        client.setConnectionClosedHandler(new Runnable() {
            public void run() {
                System.out.println("Connection closed");
            }
        });        
        client.setExceptionHandler(new ClientExceptionHandler() {
            @Override
            public void handleException(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        Thread t = new Thread(new Runnable(){
            public void run() {
                client.connect(PropertiesReader.getProperty("rtmps.server"), Integer.valueOf(PropertiesReader.getProperty("rtmps.port")), PropertiesReader.getProperty("rtmps.app"), new IPendingServiceCallback() {
                    @Override
                    public void resultReceived(IPendingServiceCall result) {
                        System.out.println("resultReceived: " + result);
                        ObjectMap<?, ?> map = (ObjectMap<?, ?>) result.getResult();
                        String code = (String) map.get("code");
                        System.out.printf("Response code: %s\n", code);
                        if ("NetConnection.Connect.Rejected".equals(code)) {
                            System.out.printf("Rejected: %s\n", map.get("description"));
                            client.disconnect();
                        } else if ("NetConnection.Connect.Success".equals(code)) {
                            System.out.println("success: " + result.isSuccess());
                        }
                    }
                });
            }
        });
        t.start();
        t.join(60000L);
        Thread.sleep(1000L);
    }

}
