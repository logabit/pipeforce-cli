package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;

/**
 * Service for all install / uninstall steps of the CLI tool.
 */
public class InstallCliService extends BaseCliContextAware {

    public static final String CLI_JAR_FILENAME = "pipeforce-cli.jar";

    /**
     * Installs the PIPEFORCE CLI into $USER_HOME/pipeforce/... if not already exists.
     * Does nothing in case it already exists.
     *
     * @return true in case the installation was done. false in case no installation was required since already done.
     */
    public boolean install() {

        this.createUserFolders();

        boolean installed = this.copyJar();

        this.createPiScript();

        return installed;
    }

    /**
     * Creates the required folders for pipeforce-cli inside the users home folder if they do not exist yet.
     * If these folders already exist, nothing happens.
     */
    private void createUserFolders() {

        String userHome = System.getProperty("user.home");
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "pipeforce-cli", "bin"));
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "pipeforce-cli", "conf"));
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "pipeforce-cli", "log"));
    }

    /**
     * Copies the downloaded jar file (given on the command line) to the final installation path.
     *
     * @return
     */
    private boolean copyJar() {

        String jarReleaseTag = getContext().getConfigService().getReleaseTagFromJar();
        String installationHome = getContext().getConfigService().getInstallationHome();
        String jarTargetPath = PathUtil.path(installationHome, "bin", "pipeforce-cli-" + jarReleaseTag + ".jar");

        if (FileUtil.isFileExists(jarTargetPath)) {
            return false;
        }

        // Do not execute installation if launched via MacOS launcher
        if (getContext().isJpackageLaunched()) {
            return false;
        }

        // Get location of pipeforce-cli.jar when called via: java -jar pipeforce-cli.jar setup
        File jarSourceFile = new File(InstallCliService.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath());

        if (!jarSourceFile.exists()) {
            throw new CliException("Could not find jar file in current working dir " +
                    jarSourceFile.getAbsolutePath() + ". Hint: Make sure you execute " +
                    "the setup command inside the same folder, the " + CLI_JAR_FILENAME + " exists!");
        }

        if (jarSourceFile.isFile()) { // When executing inside IDE -> It is a folder -> ignore
            File targetJar = new File(jarTargetPath);
            try {
                targetJar.getParentFile().mkdirs();
                FileUtils.copyFile(jarSourceFile, targetJar);
            } catch (Exception e) {
                throw new RuntimeException("Could not copy jar: " + jarSourceFile.getAbsolutePath() +
                        " to " + targetJar + ": " + e.getMessage(), e);
            }
        }

        getContext().getConfigService().setInstalledReleaseTag(jarReleaseTag);
        getContext().getConfigService().saveConfiguration();

        return true;
    }

    /**
     * Creates a new pi script in the PIPEFORCE installation folder.
     * Overwrites any existing one.
     */
    public void createPiScript() {

        String scriptContent;
        String jarTargetPath = getContext().getConfigService().getInstalledJarPath();
        File scriptFile = new File(PathUtil.path(getContext().getConfigService().getInstallationHome(), "bin", "pi"));

        // Create a pi script depending on the operating system
        if (SystemUtils.OS_NAME.toLowerCase().contains("win")) {

            scriptContent = "" +
                    "@echo OFF\n" +
                    "java -XX:TieredStopAtLevel=1 -jar " + jarTargetPath + " %*";

            // Windows needs the .bat suffix -> Change path
            scriptFile = new File(scriptFile.getAbsolutePath() + ".bat");

        } else if (getContext().isJpackageLaunched() && getContext().isOsMac()) {

            // CLI was called from Mac + installed inside Applications using jpackage (see pom.xml)
            scriptContent = "" +
                    "#!/usr/bin/env bash\n" +
                    "PIPEFORCE_WORKSPACE_HOME=\"" + getContext().getWorkspaceHome() + "\"\n" +
                    "/Applications/pi.app/Contents/MacOS/universalJavaApplicationStub $@";
        } else {

            // Mac and *nix work the same here
            scriptContent = "" +
                    "#!/usr/bin/env bash\n" +
                    "set -o noglob\n" +
                    "set -f\n" +
                    "java -XX:TieredStopAtLevel=1 -jar " + jarTargetPath + " $@";
        }

        if (scriptFile.exists()) {
            scriptFile.delete(); // Delete old file before creating a new one to avoid appending to file
        }

        FileUtil.saveStringToFile(scriptContent, scriptFile.getAbsolutePath());

        if (!getContext().isOsWin()) {
            String command = "chmod u+x " + scriptFile.getAbsolutePath();
            try {
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                throw new RuntimeException("Could not execute chmod: " + command + ": " + e.getMessage(), e);
            }
        }
    }
}
