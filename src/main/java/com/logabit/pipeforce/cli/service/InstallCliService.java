package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Service for all install / uninstall steps of the CLI tool.
 */
public class InstallCliService extends BaseCliContextAware {

    /**
     * Creates a new pi script in the PIPEFORCE home folder.
     * Overwrites any existing one.
     */
    public void createPiScript() {

        ConfigCliService config = getContext().getConfigService();

        String jarTargetPath = PathUtil.path(getContext().getUserHome(), "pipeforce", "bin", "pipeforce-cli.jar");

        String scriptContent;

        // Create a pi script depending on the operating system
        if (SystemUtils.OS_NAME.toLowerCase().contains("win")) {

            scriptContent = "" +
                    "@echo OFF\n" +
                    "java -XX:TieredStopAtLevel=1 -jar " + jarTargetPath + " %*";

            FileUtil.saveStringToFile(scriptContent, PathUtil.path(getContext().getUserHome(), "pipeforce", "pi.bat"));

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
                    "java -XX:TieredStopAtLevel=1 -jar " + jarTargetPath + " $@";
        }

        String piPath = PathUtil.path(getContext().getUserHome(), "pipeforce", "pi");
        File piScriptFile = new File(piPath);
        if (piScriptFile.exists()) {
            piScriptFile.delete(); // Delete old file before creating a new one to avoid appending to file
        }

        FileUtil.saveStringToFile(scriptContent, piPath);
        String command = "chmod u+x " + piPath;
        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException("Could not execute chmod: " + command + ": " + e.getMessage(), e);
        }
    }

    public void addToPath(String path) {

        if (!getContext().isOsMac()) {
            return; // Currently only supported for Mac
        }

        System.out.println("Add pi command to your /etc/paths?");
        Integer selection = getContext().getInputUtil().choose(ListUtil.asList("no", "yes"), "yes");
        if (selection == 0) {
            return;
        }

        try {
            Files.write(Paths.get("/etc/paths"), path.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Could not add pi to your /etc/paths. " +
                    "Make sure to run this command with admin privileges!", e);
        }
    }

    /**
     * Installs the PIPEFORCE CLI into $USER_HOME/PIPEFORCE if not already exists.
     * Does nothing in case it already exists.
     *
     * @return true in case the installation was done. false in case no installation was required since already done.
     */
    public boolean install() {

        String jarName = "pipeforce-cli.jar";
        String userHome = System.getProperty("user.home");
        String jarTargetPath = PathUtil.path(userHome, "pipeforce", "bin", jarName);

        if (FileUtil.isFileExists(jarTargetPath)) {
            return false;
        }

        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "bin"));
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "conf"));
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "log"));

        // Copy jar but only if not launched by OS native launcher
        if (!getContext().isJpackageLaunched()) {

            File jarSourceFile = new File(InstallCliService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath());

            if (!jarSourceFile.exists()) {
                throw new CliException("Could not find jar file in current working dir " +
                        jarSourceFile.getAbsolutePath() + ". Hint: Make sure you execute " +
                        "the setup command inside the same folder, the pipeforce-cli.jar exists!");
            }

            File targetJar = new File(jarTargetPath);
            targetJar.getParentFile().mkdirs();
            try {
                FileUtils.copyFile(jarSourceFile, targetJar);
            } catch (IOException e) {
                throw new RuntimeException("Could not copy jar: " + jarSourceFile.getAbsolutePath() +
                        " to " + targetJar + ": " + e.getMessage(), e);
            }
        }
        this.createPiScript();
        return true;
    }
}
