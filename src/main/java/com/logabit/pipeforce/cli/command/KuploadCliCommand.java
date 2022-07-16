package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.util.StringUtil;

/**
 * Uploads a local source into a pod inside Kubernetes.
 *
 * @author sniederm
 */
public class KuploadCliCommand extends BaseCliCommand {

    private String owner;

    @Override
    public int call(CommandArgs args) throws Exception {

        String service = args.getOptionKeyAt(0);

        if (StringUtil.isEmpty(service)) {
            throw new CliException("Specify a service!");
        }

        String localPath = args.getOptionKeyAt(1);

        if (StringUtil.isEmpty(localPath)) {
            throw new CliException("Specify a local path!");
        }

        String remotePath = args.getOptionKeyAt(2);

        if (StringUtil.isEmpty(remotePath)) {
            throw new CliException("Specify a remote path!");
        }

        if (args.getLength() >= 4) {
            this.owner = args.getOptionKeyAt(3);
        }

        String namespace = getContext().getCurrentInstance().getNamespace();
        getContext().getKubectlService().uploadToService(localPath, namespace, service, remotePath, owner);

        return 0;
    }

    public String getUsageHelp() {

        return "pi kupload <SERVICE> <LOCAL_PATH> <REMOTE_PATH>\n" +
                "   Uploads the <LOCAL_PATH> resource into <REMOTE_PATH> inside the <SERVICE> container. \n" +
                "   Examples: \n" +
                "     pi kupload orders /Users/foo/myfile /var/www/html/ - Uploads a single file into orders microservice.";
    }
}
