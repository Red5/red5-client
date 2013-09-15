package org.red5.client;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.so.IClientSharedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteSOTest {

	private static Logger log;

	private int threads = 500;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
		log = LoggerFactory.getLogger(RemoteSOTest.class);
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRemoteSO() throws Throwable {
		// test runnables represent clients
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new SOClientWorker(t);
		}
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		// fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime: " + (System.nanoTime() - start) + "ns");
		for (TestRunnable r : trs) {
			SOClientWorker cl = (SOClientWorker) r;
			log.debug("Worker: {}", cl.getId());
		}
	}

	private class SOClientWorker extends TestRunnable {

		int id;

		volatile boolean running;

		SharedObjectClient client;

		IClientSharedObject so;

		public SOClientWorker(int id) {
			this.id = id;
		}

		public void runTest() throws Throwable {
			log.debug("runTest#{}", id);
			running = true;
			client = new SharedObjectClient("localhost", 1935, "myapp", "myroom");
			while (!client.isBandwidthCheckDone()) {
				Thread.sleep(100L);
			}
			so = client.getSharedObject();
			if (so != null) {
				log.debug("Current so 'text' attribute: {}", so.getAttribute("text"));
				so.beginUpdate();
				so.setAttribute("text", RandomStringUtils.randomAlphabetic(16));
				so.endUpdate();
			} else {
				log.debug("SO was null for client: {}", id);
			}
			Thread.sleep(100L);
			client.disconnect();
			running = false;
		}

		public int getId() {
			return id;
		}

		@SuppressWarnings("unused")
		public boolean isRunning() {
			return running;
		}
	}

}
