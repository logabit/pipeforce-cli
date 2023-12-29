package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;

import static com.logabit.pipeforce.cli.uri.CliPipeforceURIResolver.Method.GET;

/**
 * Executes a PIPEFORCE URI and returns the result.
 *
 * @author sniederm
 * @since 10
 */
public class UriCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        // pi uri URI
        String uri = args.getOriginalArgs()[0];

        Object r = getContext().getResolver().resolveToObject(GET, uri, null, null, null, String.class);

        out.printResult(r);

        return 0;
    }

    public String getUsageHelp() {
        return "pi uri <URI>\n" +
                "   Executes a PIEPFORCE URI and outputs the result. \n" +
                "   Examples:\n" +
                "     pi uri $uri:command:datetime - Executes a command remotely.\n" +
                "     pi uri $uri:pipeline:global/app/myapp/hello - Executes a remote pipeline.";
    }
}
