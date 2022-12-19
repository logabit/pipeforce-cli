package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.UploadCliService;

/**
 * Uploads a given file or folder to the server as attachments to a property.
 *
 * @author sniederm
 * @since 7.0
 */
public class UploadCliCommand extends BaseCliCommand {

    private int filesCounter = 0;
    private int publishedCounter = 0;
    private int updatedCounter = 0;
    private int createdCounter = 0;

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() != 2) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String filePath = args.getOptionKeyAt(0);
        String propertyPath = args.getOptionKeyAt(1);

        doUpload(filePath, propertyPath);

        return 0;
    }

    private void doUpload(String filePath, String propertyKey) {

        UploadCliService uploadService = getContext().getUploadService();

        uploadService.upload(filePath, propertyKey);
    }

    public String getUsageHelp() {

        return "pi upload <FILE> <PROPERTY>\n" +
                "   Uploads the local file as attachment to the given property. \n" +
                "   If property doesnt exist, creates a new one with empty value. \n" +
                "   Examples: \n" +
                "     pi upload /Users/sam/contract.pdf global/app/myapp/contracts - Uploads to a property.";
    }
}
