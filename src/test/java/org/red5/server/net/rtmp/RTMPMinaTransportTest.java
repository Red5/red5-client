package org.red5.server.net.rtmp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.red5.client.net.rtmp.ClientExceptionHandler;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.net.rtmp.codec.RTMPMinaCodecFactory;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.service.Call;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(locations = { "../../context.xml" })
public class RTMPMinaTransportTest extends AbstractJUnit4SpringContextTests {

	private long clientLifetime = 60 * 1000;

	private int threads = 2;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "target/test-classes");
		System.setProperty("red5.config_root", "src/main/server/conf");
		System.setProperty("logback.ContextSelector",
				"org.red5.logging.LoggingContextSelector");
	}

	@Before
	public void setUp() throws Exception {
		assertNotNull(applicationContext);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLoad() throws Exception {
		RTMPMinaTransport mina = (RTMPMinaTransport) applicationContext
				.getBean("rtmpTransport");
		// check the io handler
		RTMPMinaIoHandler ioHandler = (RTMPMinaIoHandler) mina.ioHandler;
		if (ioHandler.codecFactory == null) {
			RTMPMinaCodecFactory codecFactory = new RTMPMinaCodecFactory();
			codecFactory.setApplicationContext(applicationContext);
			codecFactory.afterPropertiesSet();
			ioHandler.setCodecFactory(codecFactory);
		}
		mina.setBacklog(128);
		mina.setEnableDefaultAcceptor(false);
		mina.setEnableMinaMonitor(false);
		mina.setMinaPollInterval(15);
		mina.setTcpNoDelay(true);
		mina.setTrafficClass(10);
		// used when default acceptor is false
		mina.setInitialPoolSize(0);
		mina.setMaxPoolSize(4);
		mina.setMaxProcessorPoolSize(256);
		mina.setExecutorKeepAliveTime(30000);
		// create an address
		mina.setAddress("0.0.0.0:1935");
		// start
		mina.start();
		// create some clients
		List<CreatorWorker> tasks = new ArrayList<CreatorWorker>(threads);
		for (int t = 0; t < threads; t++) {
			tasks.add(new CreatorWorker());
		}
		Runtime rt = Runtime.getRuntime();
		long startFreeMem = rt.freeMemory();
		System.out.printf("Free mem: %s\n", startFreeMem);
		ExecutorService executorService = Executors.newFixedThreadPool(threads);
		// invokeAll() blocks until all tasks have run...
		List<Future<Object>> futures = executorService.invokeAll(tasks);
		assertTrue(futures.size() == threads);

		int noAV = 0;
		for (CreatorWorker r : tasks) {
			TestClient cli = r.getClient();
			assertNotNull(cli);
			if (cli != null) {
				System.out.printf("Client %d - audio: %d video: %d\n", cli
						.getConnection().getId(), cli.getAudioCounter(), cli
						.getVideoCounter());
				if (cli.getAudioCounter() == 0 || cli.getVideoCounter() == 0) {
					noAV++;
				}
				try {
					cli.disconnect();
				} catch (Throwable t) {
				}
			}
		}
		System.out.printf("Free mem: %s\n", rt.freeMemory());
		System.out.printf("Memory change: %d\n",
				(rt.freeMemory() - startFreeMem));
		System.out.printf("Client fail count: %d\n", noAV);
		// TODO change this after we fix the client handshake issues in the
		// RTMPClient
		// assertTrue(noAV == 0);
		// stop
		mina.stop();
	}

	private class CreatorWorker implements Callable<Object> {
		TestClient client;

		public Object call() throws Exception {
			client = new TestClient();
			client.connect();
			Thread.sleep(clientLifetime);
			return null;
		}

		public TestClient getClient() {
			return client;
		}

	}

	private class TestClient extends RTMPClient {

		private String server = "localhost";

		private int port = 1935;

		private String application = "junit";

		private int audioCounter;

		private int videoCounter;

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
				// System.out.println("ClientStream.dispachEvent()" +
				// event.toString());
				String evt = event.toString();
				if (evt.indexOf("Audio") >= 0) {
					audioCounter++;
				} else if (evt.indexOf("Video") >= 0) {
					videoCounter++;
				}
			}
		};

		private IPendingServiceCallback connectCallback = new IPendingServiceCallback() {
			public void resultReceived(IPendingServiceCall call) {
				// System.out.println("connectCallback");
				// if we aren't connection, skip any further processing
				if (Call.STATUS_NOT_CONNECTED != call.getStatus()) {
					ObjectMap<?, ?> map = (ObjectMap<?, ?>) call.getResult();
					String code = (String) map.get("code");
					if ("NetConnection.Connect.Rejected".equals(code)) {
						System.out.printf("Rejected: %s\n",
								map.get("description"));
						disconnect();
					} else if ("NetConnection.Connect.Failed".equals(code)) {
						System.out.printf("Failed: %s\n",
								map.get("description"));
						disconnect();
					} else if ("NetConnection.Connect.Success".equals(code)) {
						createStream(createStreamCallback);
					} else {
						System.out
								.printf("Unhandled response code: %s\n", code);
					}
				} else {
					System.err
							.println("Pending call skipped due to being no longer connected");
				}
			}
		};

		private IPendingServiceCallback createStreamCallback = new IPendingServiceCallback() {
			public void resultReceived(IPendingServiceCall call) {
				// if we aren't connection, skip any further processing
				if (Call.STATUS_NOT_CONNECTED != call.getStatus()) {
					int streamId = (Integer) call.getResult();
					conn.ping(new Ping(Ping.CLIENT_BUFFER, streamId, 4000));
					// play 2 min test clip
					play(streamId, "h264_mp3", 0, -1);
				} else {
					System.err
							.println("Pending call skipped due to being no longer connected");
				}
			}
		};

		/**
		 * @return the audioCounter
		 */
		public int getAudioCounter() {
			return audioCounter;
		}

		/**
		 * @return the videoCounter
		 */
		public int getVideoCounter() {
			return videoCounter;
		}
	}

}
