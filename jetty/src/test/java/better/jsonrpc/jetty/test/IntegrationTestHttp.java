package better.jsonrpc.jetty.test;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.exception.JsonRpcException;
import better.jsonrpc.exception.JsonRpcTimeout;
import better.jsonrpc.jetty.http.JsonRpcHttpClient;
import better.jsonrpc.jetty.test.model.SimpleAddress;
import better.jsonrpc.jetty.test.model.SimplePerson;
import better.jsonrpc.jetty.test.rpc.ISimpleServer;
import better.jsonrpc.jetty.test.server.TestServer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.client.HttpClient;
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
 * Integration test using HTTP
 *
 * This tests our own client against our own server
 * using calls to the ISimpleServer service.
 *
 * Communication is done using HTTP on localhost,
 * with the server being started per-test.
 */
public class IntegrationTestHttp {

    static final Logger LOG = (Logger) LoggerFactory.getLogger(IntegrationTestHttp.class);

    /** Server to test against */
    private TestServer mServer;

    /** URI of server (initialized at start) */
    private URI mServerUri;

    /** Underlying HTTP client */
    private HttpClient mHttpClient;

    /** JSON-RPC client */
    private JsonRpcClient mRpcClient;

    /** JSON-RPC transport */
    private JsonRpcHttpClient mRpcHttpClient;

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
        mServerUri = new URI("http://localhost:" + mServer.getLocalPort() + "/http");

        // create and start HTTP client
        LOG.info("Starting HTTP client");
        mHttpClient = new HttpClient();
        mHttpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
        mHttpClient.start();

        // create the JSON-RPC client
        LOG.info("Starting RPC client");
        mRpcClient = new JsonRpcClient();
        mRpcClient.setRequestTimeout(2000);

        // create the HTTP transport
        LOG.info("Creating RPC transport");
        mRpcHttpClient = new JsonRpcHttpClient(mServerUri, "application/json", mHttpClient, new ObjectMapper());

        // bind the HTTP transport
        LOG.info("Binding HTTP client to RPC client");
        mRpcHttpClient.bindClient(mRpcClient);

        // create a client proxy for our RPC interface
        LOG.info("Creating RPC proxy");
        mProxy = mRpcHttpClient.makeProxy(ISimpleServer.class);
    }

    @After
    public void after() throws Exception {
        // stop the HTTP client
        LOG.info("Stopping client");
        mHttpClient.stop();

        // stop and join the test server
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
