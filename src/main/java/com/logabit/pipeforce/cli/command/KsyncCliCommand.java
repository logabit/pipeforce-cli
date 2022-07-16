package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.StringUtil;
import io.methvin.watcher.DirectoryWatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Auto-publishes all resources of a given app to the given service inside Kubernetes.
 *
 * @author sniederm
 */
public class KsyncCliCommand extends BaseCliCommand {

    private String namespace;
    private String service;
    private Path remotePath;
    private Path localPath;
    private String owner;

    @Override
    public int call(CommandArgs args) throws Exception {

        this.service = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(service)) {
            throw new CliException("Specify a service!");
        }

        String localPathArg = args.getOptionKeyAt(1);

        if (StringUtil.isEmpty(localPathArg)) {
            throw new CliException("Specify a local path!");
        }

        String remotePathArg = args.getOptionKeyAt(2);

        if (StringUtil.isEmpty(remotePathArg)) {
            throw new CliException("Specify a remote path!");
        }

        if (args.getLength() >= 4) {
            this.owner = args.getOptionKeyAt(3);
        }

        this.remotePath = Paths.get(remotePathArg);

        this.namespace = this.getContext().getCurrentInstance().getNamespace();

        this.localPath = Paths.get(localPathArg);

        if (!Files.exists(localPath)) {
            throw new CliException("Local path doesnt exist: " + localPath.toAbsolutePath());
        }

        if (!Files.isDirectory(localPath)) {
            throw new CliException("Local path must point to a directory: " + localPath.toAbsolutePath());
        }

        // See: https://github.com/gmethvin/directory-watcher
        DirectoryWatcher.builder()
                .path(this.localPath)
                .listener(event -> {

                    Path eventPath = event.path();

                    if (!acceptPath(eventPath)) {
                        return;
                    }

                    switch (event.eventType()) {

                        case CREATE:
                            createRemote(eventPath);
                            break;

                        case MODIFY:
                            updateRemote(eventPath);
                            break;

                        case DELETE:
                            deleteRemote(eventPath, event.isDirectory());
                            break;
                    }
                })
                .build()
                .watch();

        return 0;
    }

    private void deleteRemote(Path path, boolean isDir) {

        try {
            Path remotePath = relativeToRemote(path);

            String r = "";
            if (isDir) {
                r = "-R ";
            }

            getContext().getKubectlService().exec(namespace, service, "rm " + r + remotePath);

        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    private void updateRemote(Path path) throws IOException {
        createRemote(path);
    }

    private void createRemote(Path path) throws IOException {

        try {
            Path remoteResource = relativeToRemote(path);
            getContext().getKubectlService().uploadToService(path.toAbsolutePath().toString(),
                    namespace, service, remoteResource.toString(), this.owner);
        } catch (Exception e) {
            out.println(e.getMessage());
        }
    }

    private Path relativeToRemote(Path path) {

        // Make the local resource path relative to the remote path
        Path relPath = this.localPath.relativize(path);
        Path remoteResource = this.remotePath.resolve(relPath);
        return remoteResource;
    }

    private boolean acceptPath(Path path) {

        if (path.getFileName().toString().startsWith(".")) {
            return false;
        }

        return true;
    }

    public String getUsageHelp() {

        return "pi ksync <SERVICE> <LOCAL_FOLDER> <REMOTE_FOLDER>  \n" +
                "   Watches all files in local folder and syncs them with the \n" +
                "   remote folder in a Kubernetes service container. \n" +
                "   Examples: \n" +
                "     pi ksync /src/ /srv/ - Syncs content changes inside of /src/ recursively to /srv/.\n";
    }
}
