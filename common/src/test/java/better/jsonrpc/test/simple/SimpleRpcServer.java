package better.jsonrpc.test.simple;

import better.jsonrpc.test.simple.model.SimpleAddress;
import better.jsonrpc.test.simple.model.SimplePerson;
import better.jsonrpc.test.simple.rpc.ISimpleServer;
import better.jsonrpc.test.simple.rpc.SimpleException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class SimpleRpcServer implements ISimpleServer {

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
    public void timeout(long msecsToBlock) {
        long now = System.currentTimeMillis();
        long end = now + msecsToBlock;
        while (now < end) {
            try {
                Thread.sleep(end - now);
            } catch (InterruptedException e) {
            }
            now = System.currentTimeMillis();
        }
    }

    @Override
    public SimpleAddress extractAddress(SimplePerson person) {
        return person.getAddress();
    }

    @Override
    public void throwSimpleException(String message) throws SimpleException {
        throw new SimpleException(message);
    }

    @Override
    public void throwException() throws Exception {
        throw new Exception("Test exception");
    }

    @Override
    public void throwRuntimeException() {
        throw new RuntimeException("Test exception");
    }

    @Override
    public void throwTranslatedException() throws Exception {
        throw new Exception("Test exception");
    }

    @Override
    public void throwTranslatedRuntimeException() {
        throw new RuntimeException("Test runtime exception");
    }

    @Override
    public void throwTranslatedExceptions(boolean interrupted) throws IOException, InterruptedException {
        if(interrupted) {
            throw new InterruptedException("Test exception");
        } else {
            throw new IOException("Test exception");
        }
    }

    @Override
    public void throwTranslatedRuntimeExceptions(boolean arrayIndex) {
        if(arrayIndex) {
            throw new ArrayIndexOutOfBoundsException("Test exception");
        } else {
            throw new NullPointerException("Test exception");
        }
    }

}
