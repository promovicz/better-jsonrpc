package better.jsonrpc.test.simple.rpc;

import better.jsonrpc.annotations.JsonRpcTranslateException;
import better.jsonrpc.annotations.JsonRpcTranslateExceptions;
import better.jsonrpc.test.simple.model.SimpleAddress;
import better.jsonrpc.test.simple.model.SimplePerson;

import java.io.IOException;
import java.util.Date;

@JsonRpcTranslateException(
    exception = SimpleException.class, code = 1000
)
public interface ISimpleServer {

    void ping();

    String toString(Object object);

    int add(int a, int b);
    int mul(int a, int b);

    Date inOneHour(Date from);

    void timeout(long msecsToBlock);

    SimpleAddress extractAddress(SimplePerson person);

    void throwSimpleException(String message) throws SimpleException;

    void throwException() throws Exception;

    void throwRuntimeException();

    @JsonRpcTranslateException(
        exception = Exception.class,
        code = 2342, message = "TranslatedException"
    )
    void throwTranslatedException() throws Exception;

    @JsonRpcTranslateException(
        exception = RuntimeException.class,
        code = 2342, message = "TranslatedRuntimeException"
    )
    void throwTranslatedRuntimeException();

    @JsonRpcTranslateExceptions({
        @JsonRpcTranslateException(
            exception = IOException.class,
            code = 23, message = "TranslatedIOException"),
        @JsonRpcTranslateException(
            exception = InterruptedException.class,
            code = 42, message = "TranslatedInterruptedException")
    })
    void throwTranslatedExceptions(boolean interrupted) throws IOException, InterruptedException;

    @JsonRpcTranslateExceptions({
        @JsonRpcTranslateException(
            exception = NullPointerException.class,
            code = 23, message = "TranslatedNullPointerException"),
        @JsonRpcTranslateException(
            exception = ArrayIndexOutOfBoundsException.class,
            code = 42, message = "TranslatedArrayIndexOutOfBoundsException")
    })
    void throwTranslatedRuntimeExceptions(boolean arrayIndex);

}
