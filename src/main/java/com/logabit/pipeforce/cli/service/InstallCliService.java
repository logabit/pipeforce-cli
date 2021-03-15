package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.InputUtil;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;

/**
 * Service for all install / uninstall steps of the CLI tool.
 *
 * @author sniederm
 * @since 6.0
 */
public class InstallCliService extends BaseCliContextAware {

    /**
     * Creates a new pi script in the PIPEFORCE home folder.
     * Overwrites any existing one.
     */
    public void createPiScript() {

        ConfigCliService config = getContext().getConfigService();

        String jarTargetPath = PathUtil.path(getContext().getUserHome(), "pipeforce", "tool", "pipeforce-cli-" +
                config.getInstalledVersion() + ".jar");

        // Create a pi script depending on the operating system
        if (SystemUtils.OS_NAME.toLowerCase().contains("win")) {

            String batContent = "" +
                    "@echo OFF\n" +
                    "java -XX:TieredStopAtLevel=1 -jar " + jarTargetPath + " %*";

            FileUtil.saveStringToFile(batContent, PathUtil.path(getContext().getUserHome(), "pipeforce", "pi.bat"));

        } else {

            // Mac and *nix work the same here
            String bashContent = "" +
                    "#!/usr/bin/env bash\n" +
                    "java -XX:TieredStopAtLevel=1 -jar " + jarTargetPath + " $@";

            String piPath = PathUtil.path(getContext().getUserHome(), "pipeforce", "pi");
            FileUtil.saveStringToFile(bashContent, piPath);
            String command = "chmod u+x " + piPath;
            try {
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                throw new RuntimeException("Could not execute chmod: " + command + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Installs the PIPEFORCE CLI into $USER_HOME/PIPEFORCE if not already exists.
     * Does nothing in case it already exists.
     *
     * @return true in case the installation was done. false in case no installation was required since already done.
     */
    public boolean install() {

        ConfigCliService config = getContext().getConfigService();

        String jarName = "pipeforce-cli-" + config.getInstalledVersion() + ".jar";
        String userHome = System.getProperty("user.home");
        String jarTargetPath = PathUtil.path(userHome, "pipeforce", "tool", jarName);
        String pipeforceHome = PathUtil.path(userHome, "pipeforce");

        if (FileUtil.isFileExists(jarTargetPath)) {
            return false;
        }

        // TODO Check for updates

        System.out.println("Install PIPEFORCE CLI to " + pipeforceHome + "?");
        Integer selection = InputUtil.choose(ListUtil.asList("no", "yes"), "yes");
        if (selection == 0) {
            return false;
        }

        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "tool"));
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "src"));
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "conf"));
        FileUtil.createFolders(PathUtil.path(userHome, "pipeforce", "log"));

        // Copy jar
        String jarSourcePath = PathUtil.path(System.getProperty("user.dir"), jarName);
        File targetJar = new File(jarTargetPath);
        targetJar.getParentFile().mkdirs();
        try {
            FileUtils.copyFile(new File(jarSourcePath), targetJar);
        } catch (IOException e) {
            throw new RuntimeException("Could not copy jar: " + jarSourcePath + " to " + targetJar + ": " + e.getMessage(), e);
        }

        this.createPiScript();
        return true;
    }
}
