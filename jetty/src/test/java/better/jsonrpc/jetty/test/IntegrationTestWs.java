package better.jsonrpc.jetty.test;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.exception.JsonRpcException;
import better.jsonrpc.exception.JsonRpcTimeout;
import better.jsonrpc.jetty.test.model.SimpleAddress;
import better.jsonrpc.jetty.test.model.SimplePerson;
import better.jsonrpc.jetty.test.rpc.ISimpleServer;
import better.jsonrpc.jetty.test.server.TestServer;
import better.jsonrpc.jetty.websocket.JsonRpcWsClient;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Integration test using WebSocket
 *
 * This tests our own client against our own server
 * using calls to the ISimpleServer service.
 *
 * Communication is done using a WebSocket connection
 * on localhost, with the server being started per-test.
 */
public class IntegrationTestWs {

    private static final Logger LOG = (Logger) LoggerFactory.getLogger(IntegrationTestWs.class);

    /** Server to test against */
    private TestServer mServer;

    /** URI of server (initialized at start) */
    private URI mServerUri;

    /** WS client factors */
    private WebSocketClientFactory mWscFactory;

    /** JSON-RPC client */
    private JsonRpcClient mClient;

    /** JSON-RPC transport */
    private JsonRpcWsClient mWsClient;

    /** JSON-RPC client proxy */
    private ISimpleServer mProxy;

    static {
        // initialize logging to be less noisy
        Logger ROOT_LOGGER = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        ROOT_LOGGER.setLevel(Level.INFO);
    }

    @Before
    public void before() throws Exception {
        // start the test server
        LOG.info("Starting server");
        mServer = new TestServer();
        mServer.start();

        // get URI of the server
        mServerUri = new URI("ws://localhost:" + mServer.getLocalPort() + "/ws");

        // create JSON-RPC client
        LOG.info("Creating RPC client");
        mClient = new JsonRpcClient();
        mClient.setRequestTimeout(2000);

        // create WSC factory
        LOG.info("Creating WS client factory");
        mWscFactory = new WebSocketClientFactory();
        mWscFactory.start();

        // create and bind the ws client
        LOG.info("Creating WS client");
        mWsClient = new JsonRpcWsClient(mServerUri, "jsonrpc/json", mWscFactory.newWebSocketClient());

        // bind the ws client to the transport
        LOG.info("Binding WS client to RPC client");
        mWsClient.bindClient(mClient);

        // create RPC proxy
        LOG.info("Creating RPC proxy");
        mProxy = mWsClient.makeProxy(ISimpleServer.class);

        // let the ws client connect
        LOG.info("Connecting WS client");
        mWsClient.connect(10, TimeUnit.SECONDS);
    }

    @After
    public void after() throws Exception {
        // disconnect the ws client
        LOG.info("Disconnecting WS client");
        mWsClient.disconnect();
        // stop the WS client factory
        LOG.info("Stopping WS client factory");
        mWscFactory.stop();
        // stop and join the server
        LOG.info("Stopping server");
        mServer.stop();
        mServer.join();
    }

    @Test
    public void testPing() {
        mProxy.ping();
    }

    @Test
    public void testMath() {
        Assert.assertEquals(5, mProxy.add(2, 3));
        Assert.assertEquals(36, mProxy.mul(6, 6));
        Assert.assertEquals(666, mProxy.add(600, mProxy.add(60, 6)));
    }

    @Test
    public void testDate() {
        // get date
        Date now = new Date();
        // compute reference value
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.HOUR, 1);
        // call and compare
        Assert.assertEquals(calendar.getTime(), mProxy.inOneHour(now));
    }

    @Test
    public void testToString() {
        // note that these calls are boxed
        Assert.assertEquals("23", mProxy.toString(23));
        Assert.assertEquals("false", mProxy.toString(false));
        Assert.assertEquals("fnord", mProxy.toString("fnord"));
    }

    @Test(expected = JsonRpcException.class)
    public void testException() throws Exception {
        mProxy.throwException();
    }

    @Test(expected = JsonRpcException.class)
    public void testRuntimeException() {
        mProxy.throwRuntimeException();
    }

    @Test
    public void testPojo() {
        SimplePerson person = new SimplePerson("Alice", "Archer");
        SimpleAddress address = new SimpleAddress();
        address.setCity("Aberdeen");
        address.setStreet("Archer Alley");
        address.setNumber("23e");
        person.setAddress(address);
        SimpleAddress result = mProxy.extractAddress(person);
        Assert.assertTrue(address.equals(result));
    }

    @Test(expected = JsonRpcTimeout.class)
    public void testTimeout() {
        mProxy.timeout(5000);
    }

    @Test
    public void testSequentialCalls() {
        for(int i = 0; i < 1000; i++) {
            testPojo();
        }
    }

    @Test
    public void testConcurrentCalls() throws InterruptedException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
        for(int i = 0; i < 5000; i++) {
            executor.execute(this::testPojo);
        }
        executor.shutdown();
        Assert.assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
    }

}
