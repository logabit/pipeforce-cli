
package com.logabit.pipeforce.cli;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a path argument in the CLI.
 * Holds a localPattern which is an absolute path to a file, folder or a pattern (ant style)
 * and a remotePattern which will be applied to the server.
 * Any time the user can type-in a path argument, you should wrap it into this class since
 * it converts relatively to the PIPEFORCE home and and current work dir.
 */
public class CliPathArg {

    private final String cwd;
    private final String home;
    private final String localPattern;
    private final String remotePattern;
    private String originalPath;
    private String encodedOriginalPath;
    private static final String ASTERISK = "%1Xc";

    public CliPathArg(String path, String cwd, String home) {
        this.originalPath = path;
        this.cwd = cwd;
        this.home = home;
        this.originalPath = path == null ? "" : path;
        this.encodedOriginalPath = encodePath(originalPath);

        String encodedLocalPattern = toAbsoluteLocalPath(this.encodedOriginalPath);
        String encodedRemotePattern = toRemotePattern(encodedLocalPattern);

        this.localPattern = toExternalForm(encodedLocalPattern).replaceAll("\\\\", "/");
        this.remotePattern = toExternalForm(encodedRemotePattern).replaceAll("\\\\", "/");
    }

    /**
     * Replaces any asterisk * by ASTERISK and converts any backward slash to forward slash.
     * Notes the position of the asterisk in the asteriskPositions.
     *
     * @param originalPath
     * @return
     */
    private String encodePath(String originalPath) {

        originalPath = originalPath.replaceAll("\\\\", "/");
        if (!originalPath.contains("*")) {
            return originalPath;
        }

        return originalPath.replaceAll("\\*", ASTERISK);
    }

    public String getOriginalPath() {
        return originalPath;
    }

    /**
     * Returns the absolute path of the given path.
     * If the given path is absolute, it is returned as it is.
     * If the given path is relative, the absolute path inside the home folder is returned.
     *
     * @param path
     * @return
     */
    private String toAbsoluteLocalPath(String path) {

        Path homePath = Paths.get(home);
        Path srcPath = Paths.get(home, "src");
        Path cwdPath = Paths.get(cwd);

        // CWD must be PIPEFORCE home or its src sub path
        if (!(homePath.equals(cwdPath) || cwdPath.startsWith(srcPath))) {
            throw new CliException("Current working dir must be inside workspace home path [" + home + "] but is: " + cwdPath.toString());
        }

        Path p = Paths.get(path);
        if (p.isAbsolute()) {

            if (!p.startsWith(srcPath)) {
                throw new CliException("Absolute path [" + toExternalForm(path) + "] must point to src inside home folder: " + srcPath);
            }

            return p.toString();
        }

        // CWD is inside PIPEFORCE home but given path doesnt start with src -> add it
        if (cwdPath.equals(homePath) && (!p.startsWith("src"))) {
            p = Paths.get("src", p.toString());
        }

        return Paths.get(cwd, p.toString()).toString();
    }

    private String toExternalForm(String path) {
        return path.replaceAll(ASTERISK, "*");
    }

    private String toRemotePattern(String absoulteLocalPath) {

        Path absPath = Paths.get(absoulteLocalPath);
        Path srcPath = Paths.get(home, "src");
        Path remotePath = srcPath.relativize(absPath);

        // Is it already a pattern like this? global/*/myapp/**
        if (remotePath.toString().contains(ASTERISK)) {
            return remotePath.toString();
        }

        // If it is a folder, add /** for remote
        if (encodedOriginalPath.endsWith("/")) {
            return Paths.get(remotePath.toString(), ASTERISK + ASTERISK).toString(); // Add **: global/app/**
        }

        File file = remotePath.toFile();
        if (isDir(file)) {
            return Paths.get(remotePath.toString(), ASTERISK + ASTERISK).toString(); // Add **: global/app/**
        }

        // Return pattern as it is
        return remotePath.toString();
    }

    public boolean isDir(File file) {
        return file.isDirectory();
    }

    /**
     * Returns the local path pattern as absolute path.
     * Use {@link #isPattern()} to detect whether it is a pattern or a 100% valid path.
     *
     * @return
     */
    public String getLocalPattern() {
        return this.localPattern;
    }

    /**
     * Returns the remote key pattern.
     *
     * @return
     */
    public String getRemotePattern() {
        return this.remotePattern;
    }

    /**
     * Returns true, in case the original given path is a pattern path (contains *).
     *
     * @return
     */
    public boolean isPattern() {
        return this.originalPath.contains("*");
    }
}
