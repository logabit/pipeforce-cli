package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.AttachmentCliService;

/**
 * Downloads a given attachment or collection of attachments to a local folder.
 *
 * @author sniederm
 * @since 11
 */
public class DownloadCliCommand extends BaseCliCommand {

    @Override
    public int call(CommandArgs args) throws Exception {

        if (args.getLength() < 1) {
            out.println("USAGE: " + getUsageHelp());
            return -1;
        }

        String propertyPath = args.getOptionKeyAt(0); // Required
        String targetFolderPath = args.getOptionKeyAt(1); // Target folder. If missing: Download to current work dir.
        String collectionNameAndAttachmentName = args.getOptionKeyAt(2); // If missing: Download all attachments

        String collectionName = "*";
        String attachmentName = "*";

        // collectionName/attachmentName = download specific attachment with specific collection name
        // */* or * = all collections and all attachments
        // collectionName/* = all attachments in given collection
        // attachmentName = only this specific attachment, ignore collection name
        if (collectionNameAndAttachmentName != null) {

            if (!collectionNameAndAttachmentName.contains("/")) {
                attachmentName = collectionNameAndAttachmentName;
            } else {
                String[] split = collectionNameAndAttachmentName.split("/");
                collectionName = split[0];
                attachmentName = split[1];
            }
        }

        if (collectionName.equals("*")) {
            collectionName = null;
        }

        if (attachmentName.equals("*")) {
            attachmentName = null;
        }

        AttachmentCliService attachmentService = getContext().getAttachmentService();
        attachmentService.download(propertyPath, attachmentName, collectionName, targetFolderPath);

        return 0;
    }

    public String getUsageHelp() {

        return "pi download <PROPERTY_PATH> [<FOLDER_PATH> <COLLECTION_NAME>/<ATTACHMENT_NAME>]\n" +
                "   Downloads all root attachments of given property to current work dir. \n" +
                "   If property doesnt exist, creates a new one with empty value. \n" +
                "   If collection name is set to * selects attachments without collection. \n" +
                "   If attachment name is set to * selects all attachments of property (= default). \n" +
                "   Examples: \n" +
                "     pi download global/app/myapp/data/a /local/folder - Downloads to specific local folder\n.";
    }
}
