package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.UpdateCliService;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the CLI to the latest state.
 */
public class UpdateCliCommand extends BaseCliCommand {

    public static final String MSG_REJECTED_LATEST_INSTALLED = "update.rejected.latest.installed";
    public static final String MSG_REJECTED_USER_CANCELLED = "update.rejected.user.cancelled";
    public static final String MSG_REJECTED_MAC_NATIVE = "update.rejected.mac.native";
    public static final String MSG_SUCCESS = "update.success";

    private static final Logger LOG = LoggerFactory.getLogger(UpdateCliCommand.class);

    @Override
    public int call(CommandArgs args) throws Exception {

        // If natively launched with javapackage -> We do not support auto-update for now
        String nativeLaunched = System.getProperty("jpackageLaunched");
        if (!StringUtil.isEmpty(nativeLaunched)) {
            out.println("Auto-update doesnt work with native launcher on Mac.");
            out.println("Please download update manually from: https://docs.pipeforce.org");
            createResult(-1, MSG_REJECTED_MAC_NATIVE, null);

            // TODO Refactor to not return result code here. Instead caller should use getResult.
            return -1;
        }

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        UpdateCliService.VersionInfo versionInfo = getContext().getUpdateService().getVersionInfo();

        if (!versionInfo.isNewerVersionAvailable()) {
            // No update found under given url -> Most recent version is in use. Quit update.
            out.println("Latest version " + versionInfo.getCurrentVersion() + " already installed.");
            createResult(0, MSG_REJECTED_LATEST_INSTALLED, null);
            return 0;
        }

        // Update found. Ask user whether he wants to download + install update.
        out.println("A newer version of PIPEFORCE CLI has been detected: " +
                versionInfo.getLatestVersion() + ". Download and install?");
        Integer selection = in.choose(ListUtil.asList("no", "yes"), "yes");

        if (selection == 0) {
            // No was selected. So do not update. Quit.
            createResult(0, MSG_REJECTED_USER_CANCELLED, null);
            return 0;
        }

        getContext().getUpdateService().downloadAndUpdateVersion(versionInfo);

        getContext().getOutputService().println("Update from " + versionInfo.getCurrentVersion() + " to " +
                versionInfo.getLatestVersion() + " was successful.");

        // The next time a command is executed, it will use the new version...

        createResult(0, MSG_SUCCESS, null);
        return 0;
    }

    public String getUsageHelp() {
        return "pi update\n" +
                "   Checks for newer version and installs it.\n" +
                "   Note: Doesnt work with native launcher on Mac.\n" +
                "   Examples:\n" +
                "     pi update - Checks for the latest version.\n";
    }
}
