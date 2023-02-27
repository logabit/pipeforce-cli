
package com.logabit.pipeforce.cli;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents a path argument in the CLI.
 * <p>
 * Such a path argument can be a path pointing to a real existing local file
 * or a path pattern.
 * <p>
 * Holds a localPattern which is an absolute path to a file, folder or a pattern (ant style)
 * and a remotePattern which will be applied to the server.
 * Any time the user can type-in a path argument, you should wrap it into this class since
 * it converts relatively to the PIPEFORCE home and current work dir.
 */
public class CliPathArg {

    private final String propertiesHome;

    private final File propertiesHomeFile;

    private String pattern;

    private String encodedPattern;

    private static final String ASTERISK = "__1Xc__";

    /**
     * @param pattern            Can be an absolute "/Users/someUser/pipeforce/properties/global/app..." or relative
     *                           local "properties/global/app/..." or a remote path starting with "global/..."
     * @param propertiesHomeFile The path to the properties home folder (= the folder which contains the global/app/.. resources).
     */
    public CliPathArg(String pattern, File propertiesHomeFile) {

        this.propertiesHomeFile = propertiesHomeFile;
        this.propertiesHome = this.propertiesHomeFile.getName();

        Path p = Paths.get(encodePath(pattern));
        if (p.isAbsolute()) {
            throw new CliException("Absolute path not supported: " + pattern + ". Path must be relative to: " +
                    propertiesHomeFile.getAbsolutePath());
        }

        pattern = pattern.replaceAll("\\\\", "/");

        if (pattern.startsWith("global/")) {
            pattern = pattern;
        } else if (pattern.startsWith(this.propertiesHome + "/")) {
            pattern = pattern.substring((propertiesHome + "/").length());
        } else if (pattern.startsWith("pipeforce/" + propertiesHome + "/")) {
            pattern = pattern.substring(("pipeforce/" + propertiesHome + "/").length());
        } else {
            throw new IllegalArgumentException("Unrecognized pattern. Pattern must start with global/ or " +
                    this.propertiesHome + "/: " + pattern);
        }

        // Replace * by ASTERISK to avoid platform problems
        this.encodedPattern = encodePath(pattern);
        this.pattern = pattern;
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

    private String toPattern(String path) {

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

    public File getLocalFile() {

        if (isPattern()) {
            throw new CliException("Cannot return file since arg is a pattern: " + encodedPattern);
        }

        return new File(this.propertiesHomeFile, this.pattern);
    }

    /**
     * For testing purposes.
     *
     * @param file
     * @return
     */
    public boolean isDir(File file) {
        return file.isDirectory();
    }

    /**
     * Returns the local path pattern as absolute path.
     * <p>
     * For example if pattern was 'global/app/**' and propertiesHomeFile was '/Users/foo/properties' returns
     * '/Users/foo/properties/global/app/**'.
     * <p>
     * Use {@link #isPattern()} to detect whether it is a pattern or a local path.
     *
     * @return
     */
    public String getLocalPattern() {

        File f = new File(this.propertiesHomeFile, this.encodedPattern);
        String localPattern = toPattern(f.getAbsolutePath());
        return toExternalForm(localPattern).replaceAll("\\\\", "/");
    }

    /**
     * Returns the remote path pattern.
     * <p>
     * For example if given pattern was 'properties\global\app\**' returns 'global/app/**'.
     *
     * @return The path pattern ready to be applied remotely.
     */
    public String getRemotePattern() {

        return toExternalForm(toPattern(this.encodedPattern)).replaceAll("\\\\", "/");
    }

    /**
     * Returns true, in case the path is a pattern path (contains *).
     *
     * @return
     */
    public boolean isPattern() {
        return this.encodedPattern.contains(ASTERISK);
    }
}
