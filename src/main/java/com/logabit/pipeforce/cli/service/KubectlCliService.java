package com.logabit.pipeforce.cli.service;

import com.logabit.pipeforce.cli.BaseCliContextAware;
import com.logabit.pipeforce.cli.CliException;
import com.logabit.pipeforce.cli.Util;
import com.logabit.pipeforce.common.util.StringUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.logabit.pipeforce.common.util.Create.newList;
import static com.logabit.pipeforce.common.util.Create.newListFromArray;
import static com.logabit.pipeforce.common.util.StringUtil.isEmpty;

/**
 * Wrapper for kubectl commands.
 *
 * @author sn
 */
public class KubectlCliService extends BaseCliContextAware {

    private Map<String, List<String>> serviceToPodCache = new HashMap<>();

    /**
     * See: kubectl cp localPath namespace/pod/:remotePath
     *
     * @param localPath
     * @param namespace
     * @param serviceName
     * @param remotePath
     * @param owner       The chown args to be applied to the ressources after upload: group:user.
     *                    For example www-data:root. If this arg is null or empty, no chown will be executed.
     */
    public void uploadToService(String localPath, String namespace, String serviceName, String remotePath, String owner) {

        List<String> pods = getPodNamesByServiceName(namespace, serviceName);

        if (pods.isEmpty()) {
            throw new CliException("Cannot upload: No pod found for service: " + serviceName);
        }

        remotePath = Util.convertToLinuxPath(remotePath);
        localPath = Util.convertToLinuxPath(localPath);

        localExec("kubectl",
                "cp",
                "--no-preserve=true",
                localPath,
                namespace + "/" + pods.get(0) + ":" + remotePath);

        if (!isEmpty(owner)) {
            exec(namespace, serviceName, "chown", "-R ", owner, remotePath);
        }
    }

    public String exec(String namespace, String serviceName, String... command) {

        String pod = getFirstPodByServiceName(namespace, serviceName);

        List<String> list = newList("kubectl", "exec", "-n", namespace, pod, "--");
        list.addAll(newListFromArray(command));
        String[] cmd = list.toArray(new String[list.size()]);

        return localExec(cmd);
    }

    /**
     * Returns the all existing pod names for a given service or empty list if no pod has been found. Returns never null.
     *
     * @param serviceName
     * @return
     */
    public List<String> getPodNamesByServiceName(String namespace, String serviceName) {

        String key = namespace + ":" + serviceName;
        if (this.serviceToPodCache.containsKey(key)) {
            return this.serviceToPodCache.get(key);
        }

        //String c = "kubectl -n " + namespace + " get pods -l pipeforce.io/app=" + serviceName + " -o jsonpath={.items[*].metadata.name}";
        String result = localExec("kubectl",
                "-n", namespace,
                "get",
                "pods",
                "-l", "pipeforce.io/app=" + serviceName,
                "-o", "jsonpath={.items[*].metadata.name}");

        if (result == null) {
            result = "";
        }

        List<String> pods = StringUtil.splitToList(result, " ");
        this.serviceToPodCache.put(key, pods);
        return pods;
    }

    public String getFirstPodByServiceName(String namespace, String serviceName) {

        List<String> pods = getPodNamesByServiceName(namespace, serviceName);

        if (pods.isEmpty()) {
            return null;
        }

        return pods.get(0);
    }

    public String localExec(String... command) {

        Process pr;
        try {

            System.out.println(StringUtil.concat(" ", command));

            Runtime rt = Runtime.getRuntime();
            pr = rt.exec(command);

            pr.waitFor();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String result = StringUtil.fromReader(stdInput);

            if (pr.exitValue() > 0) {
                BufferedReader stdError = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
                String error = StringUtil.fromReader(stdError);
                result = result + error;
                throw new RuntimeException(result);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    public void downloadFromService(String namespace, String service, String remotePath, String localPath) {

        String pod = getFirstPodByServiceName(namespace, service);

        if (isEmpty(pod)) {
            throw new CliException("Cannot download: No pod found for service: " + service);
        }

        remotePath = Util.convertToLinuxPath(remotePath);
        localPath = Util.convertToLinuxPath(localPath);

        localExec("kubectl", "cp", namespace + "/" + pod + ":" + remotePath, localPath);
    }
}
