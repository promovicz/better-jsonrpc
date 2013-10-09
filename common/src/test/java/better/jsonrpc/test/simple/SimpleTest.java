package better.jsonrpc.test.simple;

import better.jsonrpc.client.JsonRpcClient;
import better.jsonrpc.client.JsonRpcClientTimeout;
import better.jsonrpc.core.JsonRpcExecutorConnection;
import better.jsonrpc.core.JsonRpcLocalConnection;
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

    static JsonRpcLocalConnection connectionA;
    static JsonRpcLocalConnection connectionB;

    static JsonRpcClient client;
    static JsonRpcServer server;

    static ISimpleServer proxy;

    static {
        // create a pair of local connections
        List<JsonRpcExecutorConnection> connections = JsonRpcExecutorConnection.createExecutorConnectionPair();
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
        proxy = (ISimpleServer)connectionB.makeProxy(ISimpleServer.class);
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

    @Test(expected = Exception.class)
    public void testException() throws Exception {
        proxy.throwException();
    }

    @Test(expected = RuntimeException.class)
    public void testRuntimeException() {
        proxy.throwRuntimeException();
    }

    @Test
    public void testPojo() {
        SimplePerson person = new SimplePerson("Alice", "Archer");
        SimpleAddress address = new SimpleAddress();
        address.setCity("Aberdeen");
        address.setCity("Archer Alley");
        address.setNumber("23e");
        person.setAddress(address);
        SimpleAddress result = proxy.extractAddress(person);
        Assert.assertTrue(address.equals(result));
    }

    @Test(expected = JsonRpcClientTimeout.class)
    public void testTimeout() {
        proxy.timeout(5000);
    }

}