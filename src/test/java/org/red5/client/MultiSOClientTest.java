package org.red5.client;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
//import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectBase;
import org.red5.server.api.so.ISharedObjectListener;
//import org.red5.server.scope.WebScope;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public class MultiSOClientTest {

	protected static Logger log = LoggerFactory.getLogger(MultiSOClientTest.class);

	/** commented due to dependencies problem
	private static WebScope appScope;
	 */	
	private static TestRunnable[] trs;

	private ApplicationContext applicationContext;
	
	@SuppressWarnings("unused")
	private String host = "localhost";

	@SuppressWarnings("unused")
	private String appPath = "junit";

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "bin");
		System.setProperty("red5.config_root", "bin/conf");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}	
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		fail("Not yet implemented"); // TODO
	}


	@Test
	public void testDeepDirty() throws Throwable {
		/** commented due to dependencies problem
		log.debug("testDeepDirty");
		ApplicationAdapter app = (ApplicationAdapter) applicationContext.getBean("web.handler");
		// get our room
		IScope room = ScopeUtils.resolveScope(appScope, "/junit");
		// create the SO
		app.createSharedObject(room, "dirtySO", true);
		// test runnables represent clients
		trs = new TestRunnable[2];
		for (int t = 0; t < 2; t++) {
			trs[t] = new SOClientWorker(t, app, room);
		}
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		// fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime: " + (System.nanoTime() - start) + "ns");

		// go to sleep
		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}

		for (TestRunnable r : trs) {
			SOClientWorker cl = (SOClientWorker) r;
			log.debug("Worker: {} shared object: {}", cl.getId(), cl.getSO().getAttributes());
		}

		log.debug("testDeepDirty-end");
		*/
	}
	
	/** commented due to dependencies problem
	// Used to ensure all the test-runnables are in "runTest" block.
	private static boolean allThreadsRunning() {
		for (TestRunnable r : trs) {
			if (!((SOClientWorker) r).isRunning()) {
				return false;
			}
		}
		return true;
	}

	private class SOClientWorker extends TestRunnable {

		private int id;

		private ISharedObject so;

		private volatile boolean running = false;
		
		public SOClientWorker(int id, ApplicationAdapter app, IScope room) {
			this.id = id;
			this.so = app.getSharedObject(room, "dirtySO", true);
			ISharedObjectListener listener = new MySOListener(id);
			so.addSharedObjectListener(listener);
		}

		public void runTest() throws Throwable {
			log.debug("runTest#{}", id);
			running = true;
			do {
				Thread.sleep(100);
			} while (!allThreadsRunning());
			// create complex type object
			Complex complex = (Complex) so.getAttribute("complex");
			if (complex == null) {
				complex = new Complex();
				complex.getMap().put("myId", id);
				so.setAttribute("complex", complex);
			}
			Thread.sleep(500);
			log.debug("runTest-end#{}", id);
			running = false;
		}

		public int getId() {
			return id;
		}

		public ISharedObject getSO() {
			return so;
		}
 
		public boolean isRunning() {
			return running;
		}
	}
*/
	private class MySOListener implements ISharedObjectListener {

		private int id;

		public MySOListener(int id) {
			this.id = id;
		}

		@Override
		public void onSharedObjectConnect(ISharedObjectBase so) {
			log.trace("onSharedObjectConnect");
		}

		@Override
		public void onSharedObjectDisconnect(ISharedObjectBase so) {
			log.trace("onSharedObjectDisconnect");
		}

		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, String key, Object value) {
			log.trace("onSharedObjectUpdate - key: {} value: {}", key, value);
		}

		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, IAttributeStore values) {
			log.trace("onSharedObjectUpdate - values: {}", values);
		}

		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, Map<String, Object> values) {
			log.trace("onSharedObjectUpdate - values: {}", values);
		}

		@Override
		public void onSharedObjectDelete(ISharedObjectBase so, String key) {
			log.trace("onSharedObjectDelete");
		}

		@Override
		public void onSharedObjectClear(ISharedObjectBase so) {
			log.trace("onSharedObjectClear");
		}

		@Override
		public void onSharedObjectSend(ISharedObjectBase so, String method, List<?> params) {
			log.trace("onSharedObjectSend");
		}
	}

	private class Complex {
		
		private long x = System.currentTimeMillis();

		private String s = "Complex object";

		@SuppressWarnings("rawtypes")
		private Map map = new HashMap();

		public long getX() {
			return x;
		}

		public void setX(long x) {
			this.x = x;
		}

		public String getS() {
			return s;
		}

		public void setS(String s) {
			this.s = s;
		}

		public Map getMap() {
			return map;
		}

		public void setMap(Map map) {
			this.map = map;
		}

		@Override
		public String toString() {
			return "Complex [x=" + x + ", s=" + s + ", map=" + map + "]";
		}

	}	
	
}
