package com.logabit.pipeforce.cli.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.command.stub.PropertyAttachmentChecksumParams;
import com.logabit.pipeforce.common.command.stub.PropertyAttachmentChunkContentParams;
import com.logabit.pipeforce.common.command.stub.PropertyAttachmentChunkPutParams;
import com.logabit.pipeforce.common.command.stub.PropertyAttachmentListParams;
import com.logabit.pipeforce.common.command.stub.PropertyAttachmentPutParams;
import com.logabit.pipeforce.common.command.stub.PropertyExistsParams;
import com.logabit.pipeforce.common.command.stub.PropertySchemaPutParams;
import com.logabit.pipeforce.common.io.ChunkSplitter;
import com.logabit.pipeforce.common.net.ClientPipeforceURIResolver;
import com.logabit.pipeforce.common.pipeline.Result;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.commons.codec.binary.Hex;
import org.springframework.core.io.InputStreamResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static com.logabit.pipeforce.common.util.BooleanUtil.toBoolean;
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM;

/**
 * Manages the upload and download of file and attachments to/from properties.
 *
 * @author sn
 * @since 8.5, 11
 */
public class AttachmentCliService extends BaseCliContextAware {

    /**
     * Uploads the file at given path and ads it as attachment to the given property.
     * If file path points to a folder, uploads all files inside this folder.
     * Note: Doesn't do recursive uploads of folders inside folders. Only first level files will be uploaded.
     * Files starting with a period . will be ignored.
     *
     * @param filePath       The path to the file or folder to be uploaded.
     * @param propertyPath   The path to the property to add the uploaded files as attachments.
     *                       If property doesn't exist yet, it will be created.
     * @param collectionName The optional name of the collection to group the attachment.
     */
    public void upload(String filePath, String propertyPath, String collectionName) {

        File file = new File(filePath);

        if (file.isDirectory()) {
            File[] files = file.listFiles((dir, name) -> (!name.startsWith(".")));

            if (files == null || files.length == 0) {
                return;
            }

            for (File childFile : files) {

                if (childFile.isDirectory()) {
                    getContext().getOutputService().println("Warning: Nested folders not supported. Skipping: " + childFile);
                }

                uploadFile(childFile, propertyPath, collectionName);
            }
        } else {
            uploadFile(file, propertyPath, null);
        }
    }

    private void uploadFile(File file, String propertyKey, String collectionName) {

        createPropertyIfNotExists(propertyKey);

        ClientPipeforceURIResolver resolver = getContext().getResolver();

        // Create an attachment to the property
        resolver.command(
                new PropertyAttachmentPutParams()
                        .path(propertyKey)
                        .name(file.getName())
                        .collectionName(collectionName)
                        .length(file.length()),
                Void.class
        );

        List<String> md5List = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {

            // Upload chunks to the property
            ChunkSplitter splitter = new ChunkSplitter();
            splitter.onEachChunk(chunk -> {

                resolver.command(
                        new PropertyAttachmentChunkPutParams()
                                .path(propertyKey)
                                .name(file.getName())
                                .setBody(APPLICATION_OCTET_STREAM.toString(), new InputStreamResource(chunk.getInputStream())),
                        Result.class
                );

                md5List.add(chunk.getChecksum());

            });
            splitter.splitStreamIntoChunks(fis, 0L, file.length());

            // Finalize the attachment and verify checksum
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");

            for (String md5 : md5List) {

                // md5=abcdefghi
                String[] split = md5.split("=");
                String md5Data = split[1];
                md5Digest.update(md5Data.getBytes());
            }

            String finalMd5 = "md5=" + new String(Hex.encodeHex(md5Digest.digest()));

            resolver.command(
                    new PropertyAttachmentChecksumParams()
                            .checksum(finalMd5).path(propertyKey).name(file.getName()),
                    Void.class
            );

            getContext().getOutputService().println("File [" + file.getAbsolutePath() +
                    "] uploaded + added as attachment to property: " + propertyKey + "@" + file.getName());

        } catch (Exception e) {
            throw new CliException("Could not upload file: " + file + ": " + e.getMessage(), e);
        }
    }

    private void createPropertyIfNotExists(String propertyKey) {

        ClientPipeforceURIResolver resolver = getContext().getResolver();

        Boolean exists = resolver.command(new PropertyExistsParams().path(propertyKey), Boolean.class);

        if (!toBoolean(exists)) {
            resolver.command(new PropertySchemaPutParams().path(propertyKey), Void.class);
        }
    }

    public void download(String propertyPath, String attachmentName, String collectionName, String targetFolderPath) {

        File targetFolder = null;
        if (targetFolderPath == null) {
            targetFolder = new File("."); // Current work dir
        } else {
            targetFolder = new File(targetFolderPath);
        }

        ClientPipeforceURIResolver resolver = getContext().getResolver();
        ArrayNode attachmentInfoList = resolver.command(
                new PropertyAttachmentListParams()
                        .path(propertyPath)
                        .collectionName(collectionName),
                ArrayNode.class);

        for (JsonNode attachmentNode : attachmentInfoList) {

            if (attachmentName == null || attachmentNode.get("name").textValue().equals(attachmentName)) {
                downloadAttachment(attachmentNode, targetFolder);
            }
        }
    }

    private void downloadAttachment(JsonNode attachmentNode, File targetFolder) {

        String collectionName = attachmentNode.get("collectionName").textValue();
        String attachmentUuid = attachmentNode.get("uuid").textValue();
        if (!StringUtil.isEmpty(collectionName)) {
            targetFolder = new File(targetFolder, collectionName);
        }

        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        ClientPipeforceURIResolver resolver = getContext().getResolver();

        int numberOfChunks = attachmentNode.get("numberOfChunks").intValue();

        File targetFile = new File(targetFolder, attachmentNode.get("name").textValue());
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {

            for (int i = 0; i < numberOfChunks; i++) {

                byte[] chunkBytes = resolver.command(
                        new PropertyAttachmentChunkContentParams()
                                .attachmentUuid(attachmentUuid)
                                .index(i),
                        byte[].class);

                if (chunkBytes == null) {
                    fos.flush();
                    break;
                }

                fos.write(chunkBytes);
                fos.flush();

                // TODO Calculate checksum of chunk to make sure it is not corrupted
            }

        } catch (IOException e) {
            throw new RuntimeException("Could not download file: " + targetFile + ": " + e.getMessage(), e);
        }

        // TODO Do integrity check
//        String remoteChecksum = attachmentNode.get("checksum").textValue();
//        String localChecksum = HashUtil.createHash(targetFile, HashUtil.getHashType(remoteChecksum));
//
//        if (remoteChecksum == null) {
//            getContext().getOutputService().println("Warning: No remote checksum found.
//            File integrity could not be determined: " + targetFile);
//        } else if (!remoteChecksum.equals(localChecksum)) {
//            throw new SecurityException("File integrity check failed. Remote checksum is different
//            from local checksum: " + targetFile);
//        }

        getContext().getOutputService().println("File downloaded: " + targetFile);
    }
}
