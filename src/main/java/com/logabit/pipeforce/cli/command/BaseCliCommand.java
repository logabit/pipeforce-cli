package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.common.util.InputUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;

import java.io.File;

/**
 * Base class for all CLI commands.
 *
 * @author sniederm
 * @since 7.0
 */
public abstract class BaseCliCommand implements ICliCommand {

    protected CliContext context;

    protected InputUtil in;
    protected OutputCliService out;

    protected ConfigCliService config;

    protected CommandResult result;

    public void setContext(CliContext context) {
        this.context = context;
        this.out = context.getOutputService();
        this.config = context.getConfigService();
        this.in = context.getInputUtil();
    }

    public CliContext getContext() {
        return context;
    }

    /**
     * Prepares a given command line user argument which is considered as a REMOTE property key pattern:
     * <ul>
     *     <li>
     *         Removes extension from path if exists: global/app/myapp/file.json -> global/app/myapp/file
     *     </li>
     *     <li>
     *         Expands to full app path if only single item is given: myapp -> global/app/myapp/**
     *     </li>
     *     <li>
     *         Expands to double wildcard if folder is given: global/app/myapp/ -> global/app/myapp/**
     *     </li>
     * </ul>
     *
     * @param remotePath
     * @return
     */
    public String prepareRemotePropertyKeyPattern(String remotePath, boolean removeExtension) {

        if (removeExtension) {
            // Remove extension if there is any: global/app/myapp/file.json -> myapp/file
            remotePath = PathUtil.removeExtensions(remotePath);
        }

        // Only single string is given, expand to app key: myapp -> global/app/myapp/
        if (!remotePath.contains("/")) {
            remotePath = PathUtil.path("global", "app", remotePath, "/");
        }

        // If folder path is given, expand to wildcards to delete recursively: global/app/myapp/ -> global/app/myapp/**
        if (remotePath.endsWith("/")) {
            remotePath = remotePath + "**";
        }

        return remotePath;

    }

    /**
     * Expects a potential local path pattern and prepares it:
     * <ul>
     *     <li>
     *         If empty or null -> Set it to /**
     *     </li>
     *     <li>
     *         If absolute path -> Throws exception (must be relative to config home)
     *     </li>
     *     <li>
     *         Returns the given path as absolute path PATTERN pointing inside pipeforce home.
     *     </li>
     * </ul>
     * The path can also contain no wildcards at all!
     *
     * @param path
     * @return
     */
    public String prepareLocalPathPattern(String path) {

        if (!StringUtil.isEmpty(path)) {
            String tmpPath = path.replace('*', '_'); // Windows has problems with * in File object
            File pathFile = new File(tmpPath); // We need it only because of the absolute checking
            if (pathFile.isAbsolute()) {
                throw new CliException("Absolute path not allowed for security reasons: " + path);
            }
        } else {
            path = "/**";
        }

        // Replace any backslash to forward slash \ -> / (for windows)
        path.replaceAll("\\\\", "/");

        path = prepareRemotePropertyKeyPattern(path, false);

        String src = "";
        if (!path.startsWith("/" + config.getPropertiesHome())) {
            src = config.getPropertiesHome();
        }

        File homeFolder = getContext().getRepoHome();
        path = PathUtil.path(homeFolder.toURI().toString(), src, path);
        return path;
    }

    public void setResult(CommandResult result) {

        if (this.result != null) {
            throw new CliException("Result already set!");
        }

        this.result = result;
    }

    public void createResult(int resultCode, String resultMessage, Object resultValue) {

        if (this.result != null) {
            throw new CliException("Result already set!");
        }

        this.result = new CommandResult(resultCode, resultMessage, resultValue);
    }

    @Override
    public CommandResult getResult() {
        return result;
    }
}
