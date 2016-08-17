package org.red5.client.net.rtmp;

import java.util.ArrayList;

import org.junit.Test;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;

public class RTMPClientTest {

    // https://github.com/Red5/red5-client/issues/26
    @Test
    public void test26() throws InterruptedException {
        final RTMPClient client = new RTMPClient();
        client.setConnectionClosedHandler(new Runnable() {
            @Override
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
            @Override
            public void run() {
                client.connect("localhost", 1935, "live/remote/0586e318-6277-11e3-adc2-22000a1d91fe", new IPendingServiceCallback() {
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
                            ArrayList<Object> list = new ArrayList<>();
                            list.add(new Object[] { "fujifilm-x100s-video-test-1080p-full-hd-hdmp4_720.mp4" });
                            list.add(new Object[] { "canon-500d-test-video-720-hd-30-fr-hdmp4_720.mp4" });

                            Object[] params = { "64", "cc-video-processed/", list };
                            //Object[] params = { "64", "cc-video-processed/" };
                            client.invoke("loadPlaylist", params, new IPendingServiceCallback() {
                                @Override
                                public void resultReceived(IPendingServiceCall result) {
                                    System.out.println(result);
                                }
                            });
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
