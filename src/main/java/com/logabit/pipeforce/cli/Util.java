package com.logabit.pipeforce.cli;

import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.StringUtil;

/**
 * Common place for CLI tools and utils.
 *
 * @author sn
 * @since 8.0
 */
public class Util {

    private Util() {
    }

    public static void main(String[] args) {

        String keyfileJson = FileUtil.readFileToString("file://Users/sniederm/git/secret-test/keyfile.json");
        String yaml = createSecretFromGoogleArtifactRegistry("us-east1-docker.pkg.dev", "latest", keyfileJson, "tmh-container-registry");
        System.out.println(yaml);
    }

    /**
     * Creates a secret.yaml file ready to be deployed to a Kubernetes cluster which
     * configures a given Google Cloud Artifact Registry as an additional docker push target.
     * See here for documentation about these steps:
     * https://logabit.atlassian.net/wiki/spaces/DEV/pages/2197487621/Artifact+Registry#Use-it-inside-Minikube
     *
     * @param name
     * @param namespace
     * @param keyfileJson
     * @return
     */
    public static String createSecretFromGoogleArtifactRegistry(String name, String namespace, String keyfileJson, String registryName) {

        String keyfileJsonBase64 = StringUtil.toBase64(keyfileJson);

        System.out.println(keyfileJson);
        System.out.println("---");

        String auth = "_json_key_base64:" + keyfileJsonBase64;

        System.out.println(auth);
        System.out.println("---");

        String authBase64 = StringUtil.toBase64(auth);

        String dockerconfig = "{\n" +
                "    \"auths\": {\n" +
                "        \"https://us-east1-docker.pkg.dev/pipeforce/" + registryName + "/\": {\n" +
                "            \"auth\": \"" + authBase64 + "\"\n" +
                "        }\n" +
                "}}";

        System.out.println(dockerconfig);
        System.out.println("---");

        String dockerconfigBase64 = StringUtil.toBase64(dockerconfig);

        String yaml = "apiVersion: v1\n" +
                "kind: Secret\n" +
                "metadata:\n" +
                "  name: " + name + "\n" +
                "  namespace: " + namespace + "\n" +
                "data:\n" +
                "  .dockerconfigjson: " + dockerconfigBase64 + "\n" +
                "type: kubernetes.io/dockerconfigjson";

        return yaml;
    }

    /**
     * Since some tools like kubectl do not allow drive letters C: and backwardslashes \ we have
     * to rewrite inputs like C:\testing to C/testing.
     *
     * @param path
     * @return
     */
    public static String convertToLinuxPath(String path) {

        path = path.replace(":", "");
        path = path.replace("\\", "/");
        return path;
    }
}
