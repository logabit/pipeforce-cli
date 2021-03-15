package com.logabit.pipeforce.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.command.ICliCommand;
import com.logabit.pipeforce.cli.service.AppResourceCliService;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.service.InstallCliService;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.cli.service.PublishCliService;
import com.logabit.pipeforce.cli.service.UpdateCliService;
import com.logabit.pipeforce.common.content.service.MimeTypeService;
import com.logabit.pipeforce.common.pipeline.PipelineRunner;
import com.logabit.pipeforce.common.util.PathUtil;
import com.logabit.pipeforce.common.util.ReflectionUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * This is a lightweight approach of an application context,
 * holding only the required objects and initializing them on request.
 *
 * @author sniederm
 * @since 7.0
 */
public class CliContext {

    private ConfigCliService configService;
    private OutputCliService outputService;
    private MimeTypeService mimeTypeService;
    private PipelineRunner pipelineRunner;
    private RestTemplate restTemplate;
    private HttpClient httpClient;
    private PublishCliService publishService;
    private InstallCliService installService;
    private AppResourceCliService appResourceService;
    private UpdateCliService updateService;
    private File workDir = new File(System.getProperty("user.dir"));
    private CommandArgs args;
    private String command;
    private Integer serverVersionMajor;

    public CliContext(String... args) {
        setArgs(args);
    }

    public void setArgs(String... defaultArgs) {

        if (defaultArgs == null || defaultArgs.length == 0) {
            this.args = CommandArgs.EMPTY;
            return;
        }

        this.command = defaultArgs[0];

        // COMMAND ARG1 ARG2 -> split off: COMMAND
        defaultArgs = Arrays.copyOfRange(defaultArgs, 1, defaultArgs.length);

        this.args = new CommandArgs(defaultArgs);
    }

    public String getCommand() {
        return command;
    }

    public ConfigCliService getConfigService() {

        if (configService == null) {
            configService = new ConfigCliService();
        }

        return configService;
    }

    public CommandArgs getArgs() {
        return args;
    }

    public void setConfigService(ConfigCliService configService) {
        this.configService = configService;
    }

    /**
     * Creates a new command instance and returns it.
     *
     * @param command
     * @return
     */
    public ICliCommand createCommandInstance(String command) {

        String prefix;

        if (command.contains(".")) {
            // Convert some.command -> SomeCommand
            String[] split = command.split("\\.");
            prefix = StringUtil.firstCharToUpperCase(split[0]);
            prefix = prefix + StringUtil.firstCharToUpperCase(split[1]);
        } else {
            prefix = StringUtil.firstCharToUpperCase(command);
        }

        ICliCommand cmd = (ICliCommand) ReflectionUtil.newInstance("com.logabit.pipeforce.cli.command." + prefix + "CliCommand");
        initComponent(cmd);
        return cmd;
    }

    /**
     * Creates a new command instance and executes it depending on the command arg from the console input
     * using the default args initially set on the command line.
     *
     * @return
     * @throws Exception
     */
    public ICliCommand callCommand() throws Exception {

        // The very first arg is the command name -> Lookup a class with this name: setup -> SetupCallable
        ICliCommand command = createCommandInstance(getCommand());
        command.call(args);
        return command;
    }

    public OutputCliService getOutputService() {

        if (outputService == null) {
            outputService = new OutputCliService();
            initComponent(outputService);
        }

        return outputService;
    }

    public MimeTypeService getMimeTypeService() {

        if (mimeTypeService == null) {
            mimeTypeService = new MimeTypeService(new Tika(), TikaConfig.getDefaultConfig());
            initComponent(mimeTypeService);
        }

        return mimeTypeService;
    }

    public UpdateCliService getUpdateService() {

        if (updateService == null) {
            updateService = new UpdateCliService();
            initComponent(updateService);
        }

        return updateService;
    }

    public PipelineRunner getPipelineRunner() {

        if (pipelineRunner == null) {
            ConfigCliService cfg = getConfigService();
            pipelineRunner = new PipelineRunner(cfg.getHubApiUrl("/pipeline"), cfg.getApiToken(), getRestTemplate());
        }

        return pipelineRunner;
    }

    public RestTemplate getRestTemplate() {

        if (restTemplate != null) {
            return restTemplate;
        }

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(getHttpClient());

        return new RestTemplate(requestFactory);
    }

    public HttpClient getHttpClient() {

        if (httpClient != null) {
            return httpClient;
        }

        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        HostnameVerifier hostnameVerifier = (s, sslSession) -> true;
        SSLContext sslContext = null;

        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();
        return httpClient;
    }

    /**
     * Returns the full relative path of the given file inside the home folder.
     * Throws an exception in case the given file's path doesnt point to the pipeforce home.
     *
     * @param file The file to return the relative path for. If null, current work dir is used.
     * @return
     */
    public String getRelativeToHome(File file) {

        if (file == null) {
            file = getCurrentWorkDir();
        }

        String absolutePath = file.getAbsolutePath();
        String home = getConfigService().getHome();

        if (!absolutePath.startsWith(home)) {
            throw new CliException("File [" + absolutePath + "] is not inside pipeforce home: " + home);
        }

        return file.getAbsolutePath().substring(home.length() + 1);
    }

    /**
     * In case the current work dir is inside an app folder, returns the name of this app folder.
     * Returns null in case the cwd is not inside a src/global/app/APP folder
     *
     * @return
     */
    public String getCurrentAppFolderName() {

        Path workDir = getCurrentWorkDir().toPath();
        Path pipeforceHome = Paths.get(PathUtil.path(getConfigService().getHome(), "src", "global", "app"));
        if (workDir.startsWith(pipeforceHome)) {
            Path appFolder = workDir.subpath(pipeforceHome.getNameCount(), pipeforceHome.getNameCount() + 1);
            return appFolder.toFile().getName();
        }

        return null;
    }


    /**
     * Gets the normalized path to the src dir:
     * <ul>
     *     <li>
     *         CWD=$PIPEFORCE_HOME, src/global/app/myapp = global/app/myapp
     *     </li>
     *     <li>
     *         CWD=$PIPEFORCE_HOME, global/app/myapp = global/app/myapp
     *     </li>
     *     <li>
     *         CWD=global/app/myapp, "" = global/app/myapp
     *     </li>
     *     <li>
     *         CWD=global/app/myapp, "foo" = global/app/myapp/foo
     *     </li>
     *     <li>
     *         CWD=outside $PIPEFORCE_HOME, ERROR (CWD must be inside $PIPEFORCE_HOME)
     *     </li>
     * </ul>
     *
     * @param path
     * @return
     */
    public String getRelativeToSrc(String path) {

        if (path == null) {
            path = "";
        }

        Path cwd = getCurrentWorkDir().toPath();
        String home = getConfigService().getHome();

        // The CWD must be inside the home folder (outside calls are not allowed for security reasons)
        Path homePath = Paths.get(home);

        if (!PathUtil.isSubPath(homePath, cwd)) {
            throw new CliException("Current work dir must be inside " + homePath + " but is: " + cwd);
        }

        Path srcHome = Paths.get(getConfigService().getHome(), "src");

        // If CWD is in $PIPEFORCE_HOME -> Move to src
        if (cwd.equals(homePath)) {
            cwd = srcHome;
        }

        Path pathPath = Paths.get(path);
        if (pathPath.startsWith("src")) {
            pathPath = pathPath.subpath(1, pathPath.getNameCount());
        }

        Path absolute = cwd.resolve(pathPath);

        String result = srcHome.relativize(absolute).toString();
        if (result.startsWith("..")) {
            throw new CliException("Invalid path [" + absolute + "]. It is not inside src: " + srcHome);
        }

        return result;
    }

    public PublishCliService getPublishService() {
        if (publishService == null) {
            publishService = new PublishCliService();
            initComponent(publishService);
        }

        return publishService;
    }

    public String getUserHome() {
        return System.getProperty("user.home");
    }

    public File getCurrentWorkDir() {
        return workDir;
    }

    public void setCurrentWorkDir(File workDir) {
        this.workDir = workDir;
    }

    public InstallCliService getInstallService() {

        if (this.installService == null) {
            this.installService = new InstallCliService();
            initComponent(installService);
        }

        return installService;
    }

    public AppResourceCliService getAppResourceService() {

        if (appResourceService == null) {
            appResourceService = new AppResourceCliService();
            initComponent(appResourceService);
        }

        return appResourceService;
    }

    public void initComponent(Object component) {

        if (component == null) {
            return;
        }

        if (CliContextAware.class.isAssignableFrom(component.getClass())) {
            try {
                ReflectionUtil.invokeMethod(component, "setContext", this);
            } catch (Exception e) {
                throw new CliException("Could not set context on component: " + component);
            }
        }
    }

    public int getSeverVersionMajor() {

        if (this.serverVersionMajor != null) {
            return serverVersionMajor;
        }

        try {
            JsonNode serverInfo = (JsonNode) getPipelineRunner().executePipelineUri("server.info");
            this.serverVersionMajor = serverInfo.get("versionMajor").intValue();
        } catch (Exception e) {
            this.serverVersionMajor = 6;
        }

        return serverVersionMajor;
    }
}
