package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.PathUtil;
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
        appRepoHome = createTestAppRepoHome();
        context.setCurrentWorkDir(appRepoHome);
    }

    @After
    public void tearDown() {
        FileUtil.delete(appRepoHome);
    }

    @Test
    public void testNewApp() throws Exception {

        systemInMock.provideLines("app", "someapp", null, "someDescription", null);

        context.callCommand();

        File appHome = new File(appRepoHome, "src/global/app/someapp");
        List<File> files = FileUtil.listFiles(appHome);
        files.sort(Comparator.comparing(File::getName));
        Assert.assertEquals(9, files.size());

        Assert.assertEquals("config", files.get(0).getName());
        Assert.assertEquals("form", files.get(1).getName());
        Assert.assertEquals("list", files.get(2).getName());
        Assert.assertEquals("object", files.get(3).getName());
        Assert.assertEquals("pipeline", files.get(4).getName());
        Assert.assertEquals("script", files.get(5).getName());
        Assert.assertEquals("setup", files.get(6).getName());
        Assert.assertEquals("test", files.get(7).getName());
        Assert.assertEquals("workflow", files.get(8).getName());

        File configFile = new File(appHome, "config/app.json");
        String configString = FileUtil.readFileToString(configFile);
        JsonNode config = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("someapp", config.get("title").textValue());
        Assert.assertEquals("someDescription", config.get("description").textValue());
        Assert.assertEquals("CAN_APP_SOMEAPP", config.get("show").textValue());
        Assert.assertEquals("assignment", config.get("icon").textValue());
    }

    @Test
    public void testNewFormAndNewObjectSchema() throws Exception {

        systemInMock.provideLines("0",
                "someapp1", null, null, "someicon",
                "someform", "someformdesc", "1", "someobject");

        context.setArgs("new", "form");
        context.callCommand();

        File appHome = new File(appRepoHome, "src/global/app/someapp1");

        String configString = FileUtil.readFileToString(new File(appHome, "config/app.json"));
        JsonNode appConfig = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("someapp1", appConfig.get("title").textValue());
        Assert.assertEquals("", appConfig.get("description").textValue());
        Assert.assertEquals("CAN_APP_SOMEAPP1", appConfig.get("show").textValue());
        Assert.assertEquals("someicon", appConfig.get("icon").textValue());

        String formString = FileUtil.readFileToString(new File(appHome, "form/someform.json"));
        JsonNode formConfig = JsonUtil.jsonStringToJsonNode(formString);

        Assert.assertEquals("someform", formConfig.get("title").textValue());
        Assert.assertEquals("someformdesc", formConfig.get("description").textValue());
        Assert.assertEquals("property.list?filter=global/app/someapp1/object/someobject/v1/schema", formConfig.get("schema").textValue());
        Assert.assertEquals("global/app/someapp1/object/someobject/v1/instance/%23%7Bvar.property.uuid%7D", formConfig.get("output").textValue());

        String schemaString = FileUtil.readFileToString(new File(appHome, "object/someobject/v1/schema.json"));
        JsonNode schemaConfig = JsonUtil.jsonStringToJsonNode(schemaString);

        Assert.assertEquals("object", schemaConfig.get("type").textValue());
        Assert.assertEquals("Some Number", schemaConfig.get("properties").get("someNumber").get("title").textValue());
    }

    @Test
    public void testNewListAndNewObjectSchema() throws Exception {

        systemInMock.provideLines(
                "0",
                "someapp1",
                null,
                null,
                "someicon",
                "somelist",
                "somelistdesc",
                "1",
                "someobject");

        context.setArgs("new", "list");
        context.callCommand();

        File appHome = new File(appRepoHome, "src/global/app/someapp1");

        String configString = FileUtil.readFileToString(new File(appHome, "config/app.json"));
        JsonNode appConfig = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("someapp1", appConfig.get("title").textValue());
        Assert.assertEquals("", appConfig.get("description").textValue());
        Assert.assertEquals("CAN_APP_SOMEAPP1", appConfig.get("show").textValue());
        Assert.assertEquals("someicon", appConfig.get("icon").textValue());

        String formString = FileUtil.readFileToString(new File(appHome, "list/somelist.json"));
        JsonNode formConfig = JsonUtil.jsonStringToJsonNode(formString);

        Assert.assertEquals("somelist", formConfig.get("title").textValue());
        Assert.assertEquals("somelistdesc", formConfig.get("description").textValue());
        Assert.assertEquals("property.value.expression?from=global/app/someapp1/object/someobject/v1/instance/*", formConfig.get("input").textValue());
        Assert.assertEquals("property.list?filter=global/app/someapp1/object/someobject/v1/schema", formConfig.get("schema").textValue());

        String schemaString = FileUtil.readFileToString(new File(appHome, "object/someobject/v1/schema.json"));
        JsonNode schemaConfig = JsonUtil.jsonStringToJsonNode(schemaString);

        Assert.assertEquals("object", schemaConfig.get("type").textValue());
        Assert.assertEquals("Some Number", schemaConfig.get("properties").get("someNumber").get("title").textValue());
    }

    @Test
    public void testNewListExistingObjectSchema() throws Exception {

        systemInMock.provideLines("0",
                "someapp1",
                null,
                null,
                "someicon", // New app
                "someobject", // New object
                "somelist",
                "somelistdesc",
                "0" // Do you like to load existing objects into your list? -> 0: someobject
        );

        context.setArgs("new", "object");
        context.callCommand();

        context.setArgs("new", "list");
        context.callCommand();

        File appHome = new File(appRepoHome, "src/global/app/someapp1");

        String configString = FileUtil.readFileToString(new File(appHome, "config/app.json"));
        JsonNode appConfig = JsonUtil.jsonStringToJsonNode(configString);

        Assert.assertEquals("someapp1", appConfig.get("title").textValue());
        Assert.assertEquals("", appConfig.get("description").textValue());
        Assert.assertEquals("CAN_APP_SOMEAPP1", appConfig.get("show").textValue());
        Assert.assertEquals("someicon", appConfig.get("icon").textValue());

        String listString = FileUtil.readFileToString(new File(appHome, "list/somelist.json"));
        JsonNode listConfig = JsonUtil.jsonStringToJsonNode(listString);

        Assert.assertEquals("somelist", listConfig.get("title").textValue());
        Assert.assertEquals("somelistdesc", listConfig.get("description").textValue());
        Assert.assertEquals("property.value.expression?from=global/app/someapp1/object/someobject/v1/instance/*", listConfig.get("input").textValue());
        Assert.assertEquals("property.list?filter=global/app/someapp1/object/someobject/v1/schema", listConfig.get("schema").textValue());

        String schemaString = FileUtil.readFileToString(new File(appHome, "object/someobject/v1/schema.json"));
        JsonNode schemaConfig = JsonUtil.jsonStringToJsonNode(schemaString);

        Assert.assertEquals("object", schemaConfig.get("type").textValue());
        Assert.assertEquals("Some Number", schemaConfig.get("properties").get("someNumber").get("title").textValue());
    }

    @Test
    public void testNewPipeline() throws Exception {

        systemInMock.provideLines(
                "someapp1",
                null,
                null,
                "someicon",
                "4",
                "somepipeline");

        context.setArgs("new", "app");
        context.callCommand();

        context.setArgs("new");
        context.callCommand();

        File appHome = new File(appRepoHome, "src/global/app/someapp1");

        String pipelineString = FileUtil.readFileToString(new File(appHome, "pipeline/somepipeline.pi.yaml"));
        JsonNode pipeline = JsonUtil.yamlStringToJsonNode(pipelineString);

        Assert.assertEquals("Hello World", pipeline.get("pipeline").get(0).get("log").get("message").textValue());
    }

    @Test
    public void testNewWorkflow() throws Exception {

        systemInMock.provideLines(
                "someapp1",
                null,
                null,
                "someicon",
                "5",
                "someworkflow");

        context.setArgs("new", "app");
        context.callCommand();

        context.setArgs("new");
        context.callCommand();

        File appHome = new File(appRepoHome, "src/global/app/someapp1");

        String bpmnString = FileUtil.readFileToString(new File(appHome, "workflow/someworkflow.bpmn"));
        Document bpmn = XMLUtil.toDOM(bpmnString);

        Assert.assertNotNull(((Element) bpmn.getDocumentElement().getElementsByTagName("bpmn:process").item(0)).getTagName());
    }

    private File createTestAppRepoHome() {

        File testRepo = new File(System.getProperty("user.home"), "PIPEFORCE_TEST_" + StringUtil.randomString(5));

        File srcFolder = new File(testRepo, "src");
        FileUtil.createFolders(srcFolder);

        File pipeforceFolder = new File(testRepo, ".pipeforce");
        FileUtil.createFolders(pipeforceFolder);

        return testRepo;
    }
}
