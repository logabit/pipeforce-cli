package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.common.io.ChunkSplitter;
import com.logabit.pipeforce.common.net.ClientPipeforceURIResolver;
import com.logabit.pipeforce.common.net.Request;
import com.logabit.pipeforce.common.pipeline.Result;
import com.logabit.pipeforce.common.util.JsonUtil;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import static com.logabit.pipeforce.common.util.BooleanUtil.toBoolean;

/**
 * Manages the upload of local files to property attachments.
 *
 * @author sn
 * @since 8.5
 */
public class UploadCliService extends BaseCliContextAware {

    public void upload(String filePath, String propertyKey) {

        File file = new File(filePath);

        //CommandRunner commandRunner = getCommandRunner();

        createPropertyIfNotExists(propertyKey);

        ClientPipeforceURIResolver resolver = getContext().getResolver();

        // Create an attachment to the property
        resolver.resolve(
                Request.get()
                        .uri("$uri:command:property.attachment.put")
                        .param("path", propertyKey)
                        .param("name", file.getName()),
                Void.class
        );

        List<String> md5List = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {

            // Upload chunks to the property
            ChunkSplitter splitter = new ChunkSplitter();
            splitter.onEachChunk(chunk -> {

                Result chunkUploadResult = resolver.resolve(
                        Request.post()
                                .uri("$uri:command:property.attachment.chunk.put")
                                .param("path", propertyKey)
                                .param("name", file.getName())
                                .body(chunk),
                        Result.class
                );

                md5List.add(chunk.getChecksum());
                System.out.println(JsonUtil.objectToJsonNode(chunkUploadResult.getValue()).toPrettyString());
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

            resolver.resolve(
                    Request.get().uri("$uri:command:property.attachment.checksum?checksum=" + finalMd5 +
                            "&path=" + propertyKey + "&name=" + file.getName()),
                    Void.class);

        } catch (Exception e) {
            throw new CliException("Could not upload file: " + file + ": " + e.getMessage(), e);
        }
    }

    private void createPropertyIfNotExists(String propertyKey) {

        ClientPipeforceURIResolver resolver = getContext().getResolver();

        Boolean exists = resolver.resolve(
                Request.get().uri("$uri:command:property.exists?path=" + propertyKey), Boolean.class);

        if (!toBoolean(exists)) {
            resolver.resolve(
                    Request.post().uri("$uri:command:property.schema.put?path=" + propertyKey), Void.class);
        }
    }
}
