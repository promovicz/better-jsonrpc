package better.jsonrpc.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Vector;

public abstract class JsonRpcConnectedTransport extends JsonRpcTransport {

    /** Transport listeners */
    Vector<Listener> mListeners = new Vector<Listener>();

    public JsonRpcConnectedTransport(ObjectMapper mapper, boolean connected) {
        super(mapper);
    }

    /** Interface of connection state listeners */
    public interface Listener {
        public void onOpen(JsonRpcTransport connection);
        public void onClose(JsonRpcTransport connection);
    }

    /**
     * Add a connection state listener
     * @param l
     */
    public void addListener(Listener l) {
        mListeners.add(l);
    }

    /**
     * Remove the given connection state listener
     * @param l
     */
    public void removeListener(Listener l) {
        mListeners.remove(l);
    }


    /** Dispatch connection open event (for subclasses to call) */
    protected void onOpen() {
        for(Listener l: mListeners) {
            l.onOpen(this);
        }
    }

    /** Dispatch connection close event (for subclasses to call) */
    protected void onClose() {
        for(Listener l: mListeners) {
            l.onClose(this);
        }
    }

}
