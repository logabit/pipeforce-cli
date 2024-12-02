package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.net.Request;

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

        /**
         *  $uri is dynamic here. So it can be $uri:command/pipeline or others.
         *  No change done here, as stubbing with param classes requires exact command name.
         */
        Object r = getContext().getResolver().resolve(Request.get().uri(uri), String.class);

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
