package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.io.ChunkSplitter;
import com.logabit.pipeforce.common.pipeline.CommandRunner;
import com.logabit.pipeforce.common.pipeline.Result;
import com.logabit.pipeforce.common.util.JsonUtil;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static com.logabit.pipeforce.common.util.BooleanUtil.toBoolean;
import static com.logabit.pipeforce.common.util.Create.newMap;

/**
 * Manages the upload of local files to property attachments.
 *
 * @author sn
 * @since 8.5
 */
public class UploadCliService extends BaseCliContextAware {

    private CommandRunner commandRunner;

    public void upload(String filePath, String propertyKey) {

        File file = new File(filePath);

        CommandRunner commandRunner = getCommandRunner();

        createPropertyIfNotExists(propertyKey);

        // Create an attachment to the property
        Result attachmentPutResult = commandRunner.executeCommand(
                "property.attachment.put",
                newMap(
                        "path", propertyKey,
                        "name", file.getName()
                ), null);

        List<String> md5List = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {

            // Upload chunks to the property
            ChunkSplitter splitter = new ChunkSplitter();
            splitter.setChunkListener(chunk -> {

                Result chunkUploadResult = commandRunner.executeCommand(
                        "property.attachment.chunk.put",
                        newMap(
                                "path", propertyKey,
                                "name", file.getName()
                        ),
                        chunk
                );

                md5List.add(chunk.getChecksum());
                System.out.println(JsonUtil.objectToJsonNode(chunkUploadResult.getValue()).toPrettyString());
            });
            splitter.splitIntoChunks(fis, file.length());

            // Finalize the attachment and verify checksum
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");

            for (String md5 : md5List) {

                // md5=abcdefghi
                String[] split = md5.split("=");
                String md5Data = split[1];
                md5Digest.update(md5Data.getBytes());
            }

            String finalMd5 = "md5=" + new String(Hex.encodeHex(md5Digest.digest()));

            commandRunner.executeCommand("property.attachment.checksum",
                    newMap(
                            "checksum", finalMd5,
                            "path", propertyKey,
                            "name", file.getName()
                    ), null);

        } catch (Exception e) {
            throw new CliException("Could not upload file: " + file + ": " + e.getMessage(), e);
        }
    }

    private CommandRunner getCommandRunner() {

        if (this.commandRunner != null) {
            return this.commandRunner;
        }

        this.commandRunner = getContext().getCommandRunner();
        return commandRunner;
    }

    private void createPropertyIfNotExists(String propertyKey) {
        Result propertyExistsResult = getCommandRunner().executeCommand("property.exists",
                newMap("path", propertyKey), null);

        if (!toBoolean(propertyExistsResult.getValue())) {
            Result propertyCreateResult = getCommandRunner().executeCommand(
                    "property.schema.put",
                    newMap(
                            "path", propertyKey
                    ), null);
        }
    }
}
