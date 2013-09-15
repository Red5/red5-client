package org.red5.client;

import java.util.Map;

import org.red5.client.net.rtmp.ClientExceptionHandler;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.client.net.rtmpt.RTMPTClient;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.status.StatusCodes;

public class ClientTest extends RTMPClient {

	private String server = "localhost";

	private int port = 1935;
	//private int port = 5080;

	private String application = "oflaDemo";
	//private String application = "live";

	private String filename = "prometheus.flv";
	//private String filename = "NAPNAP.flv";
	//private String filename = "cameraFeed";
	//private String filename = "stream";

	// live stream (true) or vod stream (false)
	private boolean live;
	
	private static boolean finished = false;

	public static void main(String[] args) throws InterruptedException {

		final ClientTest player = new ClientTest();
		// decide whether or not the source is live or vod
		//player.setLive(true);
		// connect
		player.connect();

		synchronized (ClientTest.class) {
			if (!finished) {
				ClientTest.class.wait();
			}
		}

		System.out.println("Ended");
	}

	public void connect() {
		setExceptionHandler(new ClientExceptionHandler() {
			@Override
			public void handleException(Throwable throwable) {
				throwable.printStackTrace();
			}
		});
		setStreamEventDispatcher(streamEventDispatcher);
		connect(server, port, application, connectCallback);
	}

	private IEventDispatcher streamEventDispatcher = new IEventDispatcher() {
		public void dispatchEvent(IEvent event) {
			System.out.println("ClientStream.dispachEvent()" + event.toString());
		}
	};

	private IPendingServiceCallback connectCallback = new IPendingServiceCallback() {
		public void resultReceived(IPendingServiceCall call) {
			System.out.println("connectCallback");
			ObjectMap<?, ?> map = (ObjectMap<?, ?>) call.getResult();
			String code = (String) map.get("code");
			System.out.printf("Response code: %s\n", code);
			if ("NetConnection.Connect.Rejected".equals(code)) {
				System.out.printf("Rejected: %s\n", map.get("description"));
				disconnect();
				synchronized (ClientTest.class) {
					finished = true;
					ClientTest.class.notifyAll();
				}
			} else if ("NetConnection.Connect.Success".equals(code)) {
				createStream(createStreamCallback);
			}			
		}
	};

	private IPendingServiceCallback createStreamCallback = new IPendingServiceCallback() {
		public void resultReceived(IPendingServiceCall call) {
			int streamId = (Integer) call.getResult();
			/*
			 NetStream.play(name, start, length, reset)
			 - start An optional numeric parameter that specifies the start time, in seconds. This parameter can also be used to indicate whether the stream is live or recorded.
				The default value for start is -2, which means that Flash Player first tries to play the live stream specified in name. If a live stream of that name is not found,
				Flash Player plays the recorded stream specified in name. If neither a live nor a recorded stream is found, Flash Player opens a live stream named name, even 
				though no one is publishing on it. When someone does begin publishing on that stream, Flash Player begins playing it.
				If you pass -1 for start, Flash Player plays only the live stream specified in name. If no live stream is found, Flash Player waits for it indefinitely 
				if len is set to -1; if len is set to a different value, Flash Player waits for len seconds before it begins playing the next item in the playlist.
				If you pass 0 or a positive number for start, Flash Player plays only a recorded stream named name, beginning start seconds from the beginning of the stream.
				If no recorded stream is found, Flash Player begins playing the next item in the playlist immediately.
				If you pass a negative number other than -1 or -2 for start, Flash Player interprets the value as if it were -2.
				
			- len An optional numeric parameter that specifies the duration of the playback, in seconds.
				The default value for len is -1, which means that Flash Player plays a live stream until it is no longer available or plays a recorded stream until it ends.
				If you pass 0 for len, Flash Player plays the single frame that is start seconds from the beginning of a recorded stream (assuming that start is equal to or greater than 0).
				If you pass a positive number for len, Flash Player plays a live stream for len seconds after it becomes available, or plays a recorded stream for len seconds.
				(If a stream ends before len seconds, playback ends when the stream ends.)
				If you pass a negative number other than -1 for len, Flash Player interprets the value as if it were -1.
			
			- reset An optional Boolean value or number that specifies whether to flush any previous playlist. If reset is false (0), name is added (queued) in the current playlist;
			  that is, name plays only after previous streams finish playing. You can use this technique to create a dynamic playlist. If reset is true (1), any previous play calls 
			  are cleared and name is played immediately. By default, the value is true.
				You can also specify a value of 2 or 3 for the reset parameter, which is useful when playing recorded stream files that contain message data. These values are analogous to 
				passing false (0) and true (1), respectively: a value of 2 maintains a playlist, and a value of 3 resets the playlist. However, the difference is that specifying 2 or 3 for 
				reset causes Flash Media Server to return all messages in the recorded stream file at once, rather than at the intervals at which the messages were originally recorded
				(the default behavior).
			 */
			// live buffer 0.5s / vod buffer 4s
			if (live) {
				conn.ping(new Ping(Ping.CLIENT_BUFFER, streamId, 500));
				play(streamId, filename, -1, -1);
			} else {
				conn.ping(new Ping(Ping.CLIENT_BUFFER, streamId, 4000));
				play(streamId, filename, 0, -1);				
			}
		}
	};

	@SuppressWarnings("unchecked")
	protected void onCommand(RTMPConnection conn, Channel channel, Header header, Notify notify) {
		super.onCommand(conn, channel, header, notify);
		System.out.println("onInvoke, header = " + header.toString());
		System.out.println("onInvoke, notify = " + notify.toString());
		Object obj = notify.getCall().getArguments().length > 0 ? notify.getCall().getArguments()[0] : null;
		if (obj instanceof Map) {
			Map<String, String> map = (Map<String, String>) obj;
			String code = map.get("code");
			if (StatusCodes.NS_PLAY_STOP.equals(code)) {
				synchronized (ClientTest.class) {
					finished = true;
					ClientTest.class.notifyAll();
				}
				disconnect();
				System.out.println("Disconnected");
			}
		}

	}

	/**
	 * @return the live
	 */
	public boolean isLive() {
		return live;
	}

	/**
	 * @param live the live to set
	 */
	public void setLive(boolean live) {
		this.live = live;
	}

}
