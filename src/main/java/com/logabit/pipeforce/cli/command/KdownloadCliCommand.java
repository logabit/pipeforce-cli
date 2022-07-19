package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.StringUtil;

/**
 * Uploads a local source into a pod inside Kubernetes.
 *
 * @author sniederm
 */
public class KdownloadCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        String service = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(service)) {
            throw new CliException("Specify a service!");
        }

        String remotePath = args.getOptionKeyAt(1);

        if (StringUtil.isEmpty(remotePath)) {
            throw new CliException("Specify a remote path!");
        }

        String localPath = args.getOptionKeyAt(2);

        if (StringUtil.isEmpty(localPath)) {
            throw new CliException("Specify a local path!");
        }

        String namespace = getContext().getCurrentInstance().getNamespace();
        getContext().getKubectlService().downloadFromService(namespace, service, remotePath, localPath);

        return 0;
    }

    public String getUsageHelp() {

        return "pi kdownload <SERVICE> <REMOTE_PATH> <LOCAL_PATH>\n" +
                "   Uploads the <LOCAL_PATH> resource into <REMOTE_PATH> inside the <SERVICE> container. \n" +
                "   Examples: \n" +
                "     pi kdownload orders /var/www/html/index.html /Users/foo/ - Downloads a single file to Linux. \n" +
                "     pi kdownload orders /var/file C/testing - Downloads to Windows C:\\testing folder. \n";
    }
}
