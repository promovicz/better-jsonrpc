package better.jsonrpc.test.simple.rpc;

import better.jsonrpc.test.simple.model.SimpleAddress;
import better.jsonrpc.test.simple.model.SimplePerson;

import java.util.Date;

public interface ISimpleServer {

    void ping();

    String toString(Object object);

    int add(int a, int b);
    int mul(int a, int b);

    Date inOneHour(Date from);

    void throwRuntimeException();

    void throwException() throws Exception;

    void timeout(long msecsToBlock);

    SimpleAddress extractAddress(SimplePerson person);

}
