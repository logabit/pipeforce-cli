
package com.logabit.pipeforce.cli;

import com.logabit.pipeforce.common.util.PathUtil;

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

    private final Path home;
    private final Path src;
    private final String localPattern;
    private final String remotePattern;
    private String encodedPath;
    private String path;
    private static final String ASTERISK = "%1Xc";

    /**
     * @param path Can be an absolute local src "/Users/someUser/pipeforce/src/global/app..." or relative
     *             local "src/global/app/..." or a remote path starting with "global/..."
     * @param home
     */
    public CliPathArg(String path, String home) {

        this.home = Paths.get(home);
        this.src = Paths.get(home, "src");

        Path p = Paths.get(path);

        if (p.isAbsolute()) {
            if (!p.startsWith(this.home)) {
                throw new IllegalArgumentException("Absolute path [" + p + "] doesn't point to resource " +
                        "inside [" + this.src + "]!");
            }

            path = path.substring(this.home.toString().length() + 1);
        }

        if (path.startsWith("global/")) {
            this.path = path;
        } else if (path.startsWith("src")) {
            this.path = path.substring("src/".length());
        } else {
            throw new IllegalArgumentException("Unrecognized path: " + path);
        }

        // Replace * by ASTERISK to avoid platform problems
        this.encodedPath = encodePath(this.path);

        this.localPattern = toExternalForm(encodePath(getLocalPathAbsolute())).replaceAll("\\\\", "/");
        this.remotePattern = toExternalForm(toRemotePattern(this.encodedPath)).replaceAll("\\\\", "/");
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

    /**
     * Replaces any internal ASTERISK code by the original * one.
     *
     * @param path
     * @return
     */
    private String toExternalForm(String path) {
        return path.replaceAll(ASTERISK, "*");
    }

    private String toRemotePattern(String path) {

        // Is it already a pattern like this? global/*/myapp/**
        if (path.contains(ASTERISK)) {
            return path;
        }

        // If it is a folder, add /** for remote
        if (path.endsWith("/")) {
            return Paths.get(path, ASTERISK + ASTERISK).toString(); // Add **: global/app/**
        }

        // Is it pointing to a local folder -> Also add /**
        File file = new File(path);
        if (isDir(file)) {
            return Paths.get(path, ASTERISK + ASTERISK).toString(); // Add **: global/app/**
        }

        return path;
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
     * Returns true, in case the path is a pattern path (contains *).
     *
     * @return
     */
    public boolean isPattern() {
        return this.encodedPath.contains(ASTERISK);
    }

    /**
     * Returns the local path relative to workspace.
     *
     * @return
     */
    public String getLocalPathRelative() {
        return PathUtil.path("src", this.path);
    }

    public String getLocalPathAbsolute() {
        return this.home.resolve(getLocalPathRelative()).toString();
    }

    public String getRemotePath() {
        return this.path;
    }
}
