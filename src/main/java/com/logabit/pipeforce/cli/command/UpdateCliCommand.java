package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the CLI to the latest state.
 */
public class UpdateCliCommand extends BaseCliCommand {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateCliCommand.class);

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String newVersion = null;
        if (args.getLength() == 1) {

            // pi update VERSION_NUMBER
            newVersion = args.getOptionKeyAt(0);
        }

        if (newVersion == null) {

            newVersion = getContext().getUpdateService().isNewerVersionAvailable(config.getInstalledVersion());
            if (newVersion == null) {
                // No update found under given url -> Most recent version is in use. Quit update.
                out.println("Newest version already installed.");
                return 0;
            }

            // Update found. Ask user whether he wants to download + install update.
            out.println("Current version is: " + config.getInstalledVersion() +
                    ". A newer version of PIPEFORCE CLI has been detected: " +
                    newVersion + ". Download and install?");
            Integer selection = in.choose(ListUtil.asList("no", "yes"), "yes");

            if (selection == 0) {
                // No was selected. So do not update. Quit.
                return 0;
            }
        }

        getContext().getUpdateService().downloadAndInstallVersion(newVersion);

        getContext().getOutputService().println("Success. Update downloaded and installed: " +
                config.getInstalledVersion() + ".");

        // The next time a command is executed, it will use the new version...

        return 0;
    }

    public String getUsageHelp() {
        return "pi update [<version>]\n" +
                "   Checks for exact/newer version and installs it.\n" +
                "   Examples:\n" +
                "     pi update - Checks for the latest version.\n" +
                "     pi update 3.0 - Downloads and installs version 3.0.";
    }
}
