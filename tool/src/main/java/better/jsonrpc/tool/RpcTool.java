package better.jsonrpc.tool;

import better.cli.CLIContext;
import better.cli.CommandLineApplication;
import better.cli.annotations.CLIEntry;
import better.cli.exceptions.CLIInitException;

@CLIEntry
public class RpcTool extends CommandLineApplication<RpcToolContext> {

    public RpcTool() throws CLIInitException {
        super();
    }

    @Override
    protected CLIContext createContext() {
        return new RpcToolContext(this);
    }

    @Override
    protected void shutdown() {

    }

}
