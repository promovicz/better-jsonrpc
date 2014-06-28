package better.jsonrpc.jetty.test.rpc;

import better.jsonrpc.jetty.test.model.SimpleAddress;
import better.jsonrpc.jetty.test.model.SimplePerson;
import better.jsonrpc.jetty.test.rpc.ISimpleServer;

import java.util.Calendar;
import java.util.Date;

public class SimpleServer implements ISimpleServer {

    @Override
    public void ping() {
    }

    @Override
    public String toString(Object object) {
        return object.toString();
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public int mul(int a, int b) {
        return a * b;
    }

    @Override
    public Date inOneHour(Date from) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.HOUR, 1);
        return calendar.getTime();
    }

    @Override
    public void throwException() throws Exception {
        throw new Exception("Just a test");
    }

    @Override
    public void throwRuntimeException() {
        throw new RuntimeException("Just a test");
    }

    @Override
    public void timeout(long msecsToBlock) {
        long now = System.currentTimeMillis();
        long end = now + msecsToBlock;
        while (now < end) {
            try {
                Thread.sleep(end - now);
            } catch (InterruptedException e) {
                return;
            }
            now = System.currentTimeMillis();
        }
    }

    @Override
    public SimpleAddress extractAddress(SimplePerson person) {
        return person.getAddress();
    }

}
