package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CliPathArg;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.common.util.ListUtil;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import io.methvin.watcher.DirectoryWatcher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Auto-publishes all resources of a given app to the server.
 *
 * @author sniederm
 * @since 7.5
 */
public class SyncCliCommand extends BaseCliCommand {

    private PublishCliCommand publishCommand;
    private DeleteCliCommand deleteCommand;

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() > 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String path = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(path)) {
            new CliException("Please specify a path!");
        }

        this.publishCommand = (PublishCliCommand) getContext().createCommandInstance("Publish");
        this.deleteCommand = (DeleteCliCommand) getContext().createCommandInstance("Delete");

        CliPathArg pathArg = getContext().createPathArg(path);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource("file:" + pathArg.getLocalPattern());

        File file = resource.getFile();

        if (!file.isDirectory()) {

            throw new CliException("Can only watch existing folders. " +
                    "This path does not point to a folder or doesnt exist: " + file.getAbsolutePath());
        }

        CliPathArg folderPattern = getContext().createPathArg(pathArg.getLocalPattern());

        out.println("Backup locally and cleanup on server " + pathArg.getLocalPattern() + " first?");
        Integer choose = in.choose(ListUtil.asList("no", "yes"), "yes", null);
        if (choose == 1) {

            // Create a local backup
            GetCliCommand getCommand = (GetCliCommand) getContext().createCommandInstance("Get");
            getCommand.get(folderPattern, "backup/sync/" + System.currentTimeMillis());

            // Delete remote all remote files
            deleteCommand.delete(folderPattern);
        }

        PublishCliService publishService = getContext().getPublishService();
        publishService.removeFolder(pathArg.getLocalPathAbsolute());
        publishService.save();

        // Upload all files from watch folder
        publishCommand.publish(folderPattern);

        // See: https://github.com/gmethvin/directory-watcher
        DirectoryWatcher.builder()
                .path(file.toPath())
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

        String p = null;
        if (isDir) {
            p = PathUtil.path(p.toString(), "**"); // Delete recursive
        } else {
            p = path.toString();
        }

        CliPathArg pathArg = getContext().createPathArg(p);
        deleteCommand.delete(pathArg);
    }

    private void updateRemote(Path path) throws IOException {
        publishCommand.publish(path.toFile());
    }

    private void createRemote(Path path) throws IOException {
        publishCommand.publish(path.toFile());
    }

    private boolean acceptPath(Path path) {

        if (path.getFileName().toString().startsWith(".")) {
            return false;
        }

        return true;
    }

    public String getUsageHelp() {

        return "pi sync <FOLDER_PATH>\n" +
                "   Watches all files in given folder and automatically syncs them with the property store at server side. \n" +
                "   <FOLDER_PATH> must point to a folder inside src.\n" +
                "   Examples: \n" +
                "     pi sync src/global/app/myapp/ - Syncs content of myapp recursively.\n" +
                "     pi sync src/global/app/ - Syncs anything below global/app/ path.";
    }
}
