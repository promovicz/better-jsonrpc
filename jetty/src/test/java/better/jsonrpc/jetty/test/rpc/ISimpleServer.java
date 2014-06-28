package better.jsonrpc.jetty.test.rpc;

import better.jsonrpc.annotations.JsonRpcInterface;
import better.jsonrpc.annotations.JsonRpcMethod;
import better.jsonrpc.jetty.test.model.SimpleAddress;
import better.jsonrpc.jetty.test.model.SimplePerson;

import java.util.Date;

@JsonRpcInterface(prefix = "simple.")
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
