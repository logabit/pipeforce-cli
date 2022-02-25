package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;

/**
 * Initializes a given folder as a PIPEFORCE repo.
 */
public class InitCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String path = null;
        if (args.getLength() == 1) {
            path = args.getOptionKeyAt(0);
        } else if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        if (path == null) {
            path = System.getProperty("user.dir");
        }

        getContext().getInitService().init(path);

        return 0;
    }

    public String getUsageHelp() {
        return "pi init [path]\n" +
                "   Initializes a given folder as a PIPEFORCE app repository.\n" +
                "   Examples: \n" +
                "     pi init - Initializes the current work dir.\n" +
                "     pi init /some/repo - Initializes the given folder.";
    }
}
