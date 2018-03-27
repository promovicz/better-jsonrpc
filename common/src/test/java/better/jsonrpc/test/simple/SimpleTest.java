package better.jsonrpc.test.simple;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.core.JsonRpcExecutorTransport;
import better.jsonrpc.exception.JsonRpcException;
import better.jsonrpc.exception.JsonRpcTimeout;
import better.jsonrpc.server.JsonRpcServer;
import better.jsonrpc.test.simple.model.SimpleAddress;
import better.jsonrpc.test.simple.model.SimplePerson;
import better.jsonrpc.test.simple.rpc.ISimpleServer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * A simple functional test of the whole library in default configuration
 */
public class SimpleTest {

    private static JsonRpcExecutorTransport connectionA;
    private static JsonRpcExecutorTransport connectionB;

    private static JsonRpcClient client;
    private static JsonRpcServer server;

    private static ISimpleServer proxy;

    static {
        // create a pair of local connections
        List<JsonRpcExecutorTransport> connections = JsonRpcExecutorTransport.createExecutorConnectionPair();
        connectionA = connections.get(0);
        connectionB = connections.get(1);

        // create the server
        SimpleRpcServer serverHandler = new SimpleRpcServer();
        server = new JsonRpcServer(ISimpleServer.class);
        connectionA.bindServer(server, serverHandler);

        // create the client
        client = new JsonRpcClient();
        client.setRequestTimeout(500);
        connectionB.bindClient(client);
        proxy = connectionB.makeProxy(ISimpleServer.class);
    }

    @Test
    public void testPing() {
        proxy.ping();
    }

    @Test
    public void testMath() {
        Assert.assertEquals(5, proxy.add(2,3));
        Assert.assertEquals(36, proxy.mul(6,6));
        Assert.assertEquals(666, proxy.add(600, proxy.add(60, 6)));
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
        Assert.assertEquals(calendar.getTime(), proxy.inOneHour(now));
    }

    @Test
    public void testToString() {
        // note that these calls are boxed
        Assert.assertEquals("23", proxy.toString(23));
        Assert.assertEquals("false", proxy.toString(false));
        Assert.assertEquals("fnord", proxy.toString("fnord"));
    }

    @Test
    public void testPojo() {
        SimplePerson person = new SimplePerson("Alice", "Archer");
        SimpleAddress address = new SimpleAddress();
        address.setCity("Aberdeen");
        address.setStreet("Archer Alley");
        address.setNumber("23e");
        person.setAddress(address);
        SimpleAddress result = proxy.extractAddress(person);
        Assert.assertTrue(address.equals(result));
    }

    @Test(expected = JsonRpcTimeout.class)
    public void testTimeout() {
        proxy.timeout(5000);
    }

    @Test
    public void testSequentialCalls() {
        for(int i = 0; i < 1000; i++) {
            testPojo();
        }
    }

    @Test
    public void testSimpleExceptionA() throws Exception {
        try {
            proxy.throwSimpleException("SimpleExceptionA");
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(1000, e.getCode());
            Assert.assertEquals("SimpleExceptionA", e.getMessage());
        }
    }

    @Test
    public void testSimpleExceptionB() throws Exception {
        try {
            proxy.throwSimpleException("SimpleExceptionB");
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(1000, e.getCode());
            Assert.assertEquals("SimpleExceptionB", e.getMessage());
        }
    }

    @Test(expected = JsonRpcException.class)
    public void testException() throws Exception {
        proxy.throwException();
    }

    @Test(expected = JsonRpcException.class)
    public void testRuntimeException() {
        proxy.throwRuntimeException();
    }

    @Test
    public void testTranslatedException() throws Exception {
        try {
            proxy.throwTranslatedException();
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(2342, e.getCode());
            Assert.assertEquals("TranslatedException", e.getMessage());
        }
    }

    @Test
    public void testTranslatedRuntimeException() {
        try {
            proxy.throwTranslatedRuntimeException();
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(2342, e.getCode());
            Assert.assertEquals("TranslatedRuntimeException", e.getMessage());
        }
    }

    @Test
    public void testTranslatedExceptionsA() throws Exception {
        try {
            proxy.throwTranslatedExceptions(false);
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(23, e.getCode());
            Assert.assertEquals("TranslatedIOException", e.getMessage());
        }
    }

    @Test
    public void testTranslatedExceptionsB() throws Exception {
        try {
            proxy.throwTranslatedExceptions(true);
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(42, e.getCode());
            Assert.assertEquals("TranslatedInterruptedException", e.getMessage());
        }
    }

    @Test
    public void testTranslatedRuntimeExceptionsA() throws Exception {
        try {
            proxy.throwTranslatedRuntimeExceptions(false);
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(23, e.getCode());
            Assert.assertEquals("TranslatedNullPointerException", e.getMessage());
        }
    }

    @Test
    public void testTranslatedRuntimeExceptionsB() throws Exception {
        try {
            proxy.throwTranslatedRuntimeExceptions(true);
            Assert.fail();
        } catch (JsonRpcException e) {
            Assert.assertEquals(42, e.getCode());
            Assert.assertEquals("TranslatedArrayIndexOutOfBoundsException", e.getMessage());
        }
    }

}
