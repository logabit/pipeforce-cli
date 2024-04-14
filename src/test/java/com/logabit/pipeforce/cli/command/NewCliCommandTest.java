package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.common.model.WorkspaceConfig;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PipelineUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import com.logabit.pipeforce.common.util.XMLUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

/**
 * Tests the {@link NewCliCommand}.
 *
 * @author sniederm
 * @since 6.0
 */
@RunWith(MockitoJUnitRunner.class)
public class NewCliCommandTest {

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TextFromStandardInputStream systemInMock = emptyStandardInputStream();

    @InjectMocks
    private final CliContext context = new CliContext("new");

    @Mock
    private ConfigCliService configService;

    private File appRepoHome;

    @Before
    public void setUp() {

        WorkspaceConfig config = new WorkspaceConfig();
        Mockito.when(configService.getWorkspaceConfig()).thenReturn(config);
        appRepoHome = createTestAppRepoHome();
        context.setCurrentWorkDir(appRepoHome);
    }

    @After
    public void tearDown() {
        FileUtil.delete(appRepoHome);
    }

    @Test
    public void testNewApp() throws Exception {

        systemInMock.provideLines("app", "com.logabit.someapp", null, "someDescription", null);

        context.callCommand();

        File appHome = new File(appRepoHome, "properties/global/app/com.logabit.someapp");
        List<File> files = FileUtil.listFiles(appHome);
        files.sort(Comparator.comparing(File::getName));
        Assert.assertEquals(1, files.size());

        File configFile = new File(appHome, "config/app.json");
        String configString = FileUtil.fileToString(configFile);
        JsonNode config = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("com.logabit.someapp", config.get("title").textValue());
        Assert.assertEquals("someDescription", config.get("description").textValue());
        Assert.assertEquals("ROLE_DEVELOPER", config.get("permissions").get("read").get(0).textValue());
        Assert.assertEquals("assignment", config.get("icon").textValue());
    }

    @Test
    public void testNewFormAndNewObjectSchema() throws Exception {

        systemInMock.provideLines("0",
                "com.logabit.someapp1", null, null, "someicon",
                "someform", "someformdesc", "1", "someobject");

        context.setArgs("new", "form");
        context.callCommand();

        File appHome = new File(appRepoHome, "properties/global/app/com.logabit.someapp1");

        String configString = FileUtil.fileToString(new File(appHome, "config/app.json"));
        JsonNode appConfig = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("com.logabit.someapp1", appConfig.get("title").textValue());
        Assert.assertEquals("", appConfig.get("description").textValue());
        Assert.assertEquals("ROLE_DEVELOPER", appConfig.get("permissions").get("read").get(0).textValue());
        Assert.assertEquals("someicon", appConfig.get("icon").textValue());

        String formString = FileUtil.fileToString(new File(appHome, "form/someform.json"));
        JsonNode formConfig = JsonUtil.jsonStringToJsonNode(formString);

        Assert.assertEquals("someform", formConfig.get("title").textValue());
        Assert.assertEquals("someformdesc", formConfig.get("description").textValue());
        Assert.assertEquals("$uri:property:global/app/com.logabit.someapp1/schema/someobject", formConfig.get("schema").textValue());
        Assert.assertEquals("$uri:property:global/app/com.logabit.someapp1/data/someobject/", formConfig.get("output").textValue());

        String schemaString = FileUtil.fileToString(new File(appHome, "schema/someobject.json"));
        JsonNode schemaConfig = JsonUtil.jsonStringToJsonNode(schemaString);

        Assert.assertEquals("object", schemaConfig.get("type").textValue());
        Assert.assertEquals("First Name", schemaConfig.get("properties").get("firstName").get("title").textValue());
    }

    @Test
    public void testNewListAndNewObjectSchema() throws Exception {

        systemInMock.provideLines(
                "0",
                "com.logabit.someapp1",
                null,
                null,
                "someicon",
                "somelist",
                "somelistdesc",
                "1",
                "someobject");

        context.setArgs("new", "list");
        context.callCommand();

        File appHome = new File(appRepoHome, "properties/global/app/com.logabit.someapp1");

        String configString = FileUtil.fileToString(new File(appHome, "config/app.json"));
        JsonNode appConfig = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("com.logabit.someapp1", appConfig.get("title").textValue());
        Assert.assertEquals("", appConfig.get("description").textValue());
        Assert.assertEquals("ROLE_DEVELOPER", appConfig.get("permissions").get("read").get(0).textValue());
        Assert.assertEquals("someicon", appConfig.get("icon").textValue());

        String formString = FileUtil.fileToString(new File(appHome, "list/somelist.json"));
        JsonNode formConfig = JsonUtil.jsonStringToJsonNode(formString);

        Assert.assertEquals("somelist", formConfig.get("title").textValue());
        Assert.assertEquals("somelistdesc", formConfig.get("description").textValue());
        Assert.assertEquals("$uri:command:property.value.list?pattern=global/app/com.logabit.someapp1/data/someobject/*", formConfig.get("input").textValue());
        Assert.assertEquals("$uri:property:global/app/com.logabit.someapp1/schema/someobject", formConfig.get("schema").textValue());

        String schemaString = FileUtil.fileToString(new File(appHome, "schema/someobject.json"));
        JsonNode schemaConfig = JsonUtil.jsonStringToJsonNode(schemaString);

        Assert.assertEquals("object", schemaConfig.get("type").textValue());
        Assert.assertEquals("First Name", schemaConfig.get("properties").get("firstName").get("title").textValue());
    }

    @Test
    public void testNewListExistingObjectSchema() throws Exception {

        systemInMock.provideLines("0",
                "com.logabit.someapp1",
                null,
                null,
                "someicon",
                "somelist",
                "somelistdesc",
                "1", // New schema
                "someobject" // Schema name
        );

        context.setArgs("new", "object");
        context.callCommand();

        context.setArgs("new", "list");
        context.callCommand();

        File appHome = new File(appRepoHome, "properties/global/app/com.logabit.someapp1");

        String configString = FileUtil.fileToString(new File(appHome, "config/app.json"));
        JsonNode appConfig = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("com.logabit.someapp1", appConfig.get("title").textValue());
        Assert.assertEquals("", appConfig.get("description").textValue());
        Assert.assertEquals("ROLE_DEVELOPER", appConfig.get("permissions").get("read").get(0).textValue());
        Assert.assertEquals("someicon", appConfig.get("icon").textValue());

        String listString = FileUtil.fileToString(new File(appHome, "list/somelist.json"));
        JsonNode listConfig = JsonUtil.jsonStringToJsonNode(listString);

        Assert.assertEquals("somelist", listConfig.get("title").textValue());
        Assert.assertEquals("somelistdesc", listConfig.get("description").textValue());
        Assert.assertEquals("$uri:command:property.value.list?pattern=global/app/com.logabit.someapp1/data/someobject/*", listConfig.get("input").textValue());
        Assert.assertEquals("$uri:property:global/app/com.logabit.someapp1/schema/someobject", listConfig.get("schema").textValue());

        String schemaString = FileUtil.fileToString(new File(appHome, "schema/someobject.json"));
        JsonNode schemaConfig = JsonUtil.jsonStringToJsonNode(schemaString);

        Assert.assertEquals("object", schemaConfig.get("type").textValue());
        Assert.assertEquals("First Name", schemaConfig.get("properties").get("firstName").get("title").textValue());
    }

    @Test
    public void testNewPipeline() throws Exception {

        systemInMock.provideLines(
                "4", // Create new... pipeline
                "0", // Create new app
                "io.pipeforce.testnewpipeline", // App name
                null, // Default title
                null, // No description
                null, // Default icon
                "somepipeline"
        );

        System.out.println("> new");
        context.setArgs("new");
        context.callCommand();

        File appHome = new File(appRepoHome, "properties/global/app/io.pipeforce.testnewpipeline");

        String pipelineString = FileUtil.fileToString(new File(appHome, "pipeline/somepipeline.pi.yaml"));
        JsonNode pipeline = JsonUtil.yamlStringToJsonNode(pipelineString);

        Assert.assertTrue(pipeline.has("pipeline"));
    }

    @Test
    public void testNewWorkflow() throws Exception {

        systemInMock.provideLines(
                "5", // Create new... workflow
                "0", // Create new app
                "io.pipeforce.testnewworkflow", // App name
                null, // Default title
                null, // No description
                null, // Default icon
                "someworkflow"
        );

        System.out.println("> new");
        context.setArgs("new");
        context.callCommand();

        File appHome = new File(appRepoHome, "properties/global/app/io.pipeforce.testnewworkflow");

        String bpmnString = FileUtil.fileToString(new File(appHome, "workflow/someworkflow.bpmn"));
        Document bpmn = XMLUtil.toDOM(bpmnString);

        Assert.assertNotNull(((Element) bpmn.getDocumentElement().getElementsByTagName("bpmn:process").item(0)).getTagName());
    }

    private File createTestAppRepoHome() {

        File testRepo = new File(System.getProperty("user.home"), "PIPEFORCE_TEST_" + StringUtil.randomString(5));

        File srcFolder = new File(testRepo, "properties");
        FileUtil.createFolders(srcFolder);

        File pipeforceFolder = new File(testRepo, ".pipeforce");
        FileUtil.createFolders(pipeforceFolder);

        return testRepo;
    }
}
