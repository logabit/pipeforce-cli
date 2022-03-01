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

        String tag = null;
        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        } else if (args.getLength() == 1) {
            tag = args.getOptionKeyAt(0);
        }

        UpdateCliService.VersionInfo versionInfo;
        if (tag == null) {
            versionInfo = this.updateToLatest();
        } else {
            versionInfo = this.updateToTag(tag);
        }

        getContext().getOutputService().println("Update from " + versionInfo.getCurrentVersion() + " to " +
                versionInfo.getLatestVersion() + " was successful.");
        // The next time a command is executed, it will use the new version...

        return 0;
    }

    private UpdateCliService.VersionInfo updateToLatest() {

        UpdateCliService.VersionInfo versionInfo = getContext().getUpdateService().getVersionInfo();

        if (!versionInfo.isNewerVersionAvailable()) {
            // No update found under given url -> Most recent version is in use. Quit update.
            createResult(0, MSG_REJECTED_LATEST_INSTALLED, null);
            return versionInfo;
        }

        // Update found. Ask user whether he wants to download + install update.
        out.println("A newer version of PIPEFORCE CLI has been detected: " +
                versionInfo.getLatestVersion() + ". Download and install?");
        Integer selection = in.choose(ListUtil.asList("no", "yes"), "yes");

        if (selection == 0) {
            // No was selected. So do not update. Quit.
            createResult(0, MSG_REJECTED_USER_CANCELLED, null);
            return versionInfo;
        }

        createResult(0, MSG_SUCCESS, null);
        getContext().getUpdateService().downloadAndUpdateVersion(versionInfo);
        return versionInfo;
    }

    private UpdateCliService.VersionInfo updateToTag(String tag) {

        String url = "https://github.com/logabit/pipeforce-cli/releases/download/" + tag + "/pipeforce-cli.jar";
        String currentReleaseName = getContext().getConfigService().getReleaseTagFromJar();
        UpdateCliService.VersionInfo versionInfo = new UpdateCliService.VersionInfo(currentReleaseName, tag, url);

        createResult(0, MSG_REJECTED_USER_CANCELLED, null);
        getContext().getUpdateService().downloadAndUpdateVersion(versionInfo);

        return versionInfo;
    }

    public String getUsageHelp() {
        return "pi update [tag]\n" +
                "   Checks for newer version and installs it.\n" +
                "   Note: Doesnt work with native launcher on Mac.\n" +
                "   Examples:\n" +
                "     pi update - Checks for the latest version.\n" +
                "     pi update v3.0.3-RC1 - Downloads and updates to the exact version tag.\n";
    }
}
