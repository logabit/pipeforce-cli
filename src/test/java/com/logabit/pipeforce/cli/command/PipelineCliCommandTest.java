package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.service.OutputCliService;
import com.logabit.pipeforce.common.model.WorkspaceConfig;
import com.logabit.pipeforce.common.pipeline.PipelineRunner;
import com.logabit.pipeforce.common.util.FileUtil;
import com.logabit.pipeforce.common.util.JsonUtil;
import com.logabit.pipeforce.common.util.StringUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.List;

import static com.logabit.pipeforce.common.property.IProperty.FIELD_PATH;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link PipelineCliCommand}.
 *
 * @author sniederm
 * @since 2.7
 */
@RunWith(MockitoJUnitRunner.class)
public class PipelineCliCommandTest {

    @InjectMocks
    private final CliContext cliContext = new CliContext();

    @Mock
    private PipelineRunner pipelineRunner;

    @Mock
    private OutputCliService out;

    @Mock
    private ConfigCliService configService;

    private File homeRepo;

    @Before
    public void setUp() {

        WorkspaceConfig config = new WorkspaceConfig();
        Mockito.when(configService.getWorkspaceConfig()).thenReturn(config);
        this.homeRepo = createTestAppRepoHome();
        cliContext.setCurrentWorkDir(homeRepo);
    }

    @Test
    public void testRunRemote() throws Exception {

        PipelineCliCommand remoteRun = (PipelineCliCommand) cliContext.createCommandInstance("pipeline");
        cliContext.setArgs("pipeline", "global/app/myapp/pipeline/hello");
        cliContext.callCommand();

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(pipelineRunner, times(1)).executePipelineUri(uriCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals("call?uri=property:global/app/myapp/pipeline/hello", values.get(0));
    }

    @Test
    public void testRunFile() throws Exception {
        //TODO expect this but system specific "/some/home/pipeforce/properties/global/app/myapp/pipeline/hello.pi.yaml"
        when(out.readFileToString(Mockito.anyString())).thenReturn("pipeline:");

        PipelineCliCommand localRun = (PipelineCliCommand) cliContext.createCommandInstance("pipeline");
        localRun.call(new CommandArgs("properties/global/app/myapp/pipeline/hello.pi.yaml"));

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(pipelineRunner, times(1)).executePipelineJsonNode(nodeCaptor.capture());

        Assert.assertEquals(NullNode.getInstance(), nodeCaptor.getValue().get("pipeline"));
    }

    @Test
    public void testRunPipelineUri() throws Exception {

        String foundProperties = "[\n" +
                "  {\n" +
                "    \"path\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop1\",\n" +
                "    \"uuid\": \"a656bc2d-9a2f-40b5-9eb7-fb0f7cc78b94\",\n" +
                "    \"value\": \"someValue1\",\n" +
                "    \"defaultValue\": null,\n" +
                "    \"type\": \"application/yaml; type=pipeline\",\n" +
                "    \"created\": 1613460723183,\n" +
                "    \"updated\": null,\n" +
                "    \"timeToLive\": null\n" +
                "  },\n" +
                "  {\n" +
                "    \"path\": \"/pipeforce/enterprise/global/app/myapp/pipeline/prop2\",\n" +
                "    \"uuid\": \"f9e714a1-dcaf-4da6-908b-2571b7dcd8c7\",\n" +
                "    \"value\": \"someValue2\",\n" +
                "    \"defaultValue\": null,\n" +
                "    \"type\": \"application/yaml; type=pipeline\",\n" +
                "    \"created\": 1613460723360,\n" +
                "    \"updated\": null,\n" +
                "    \"timeToLive\": null\n" +
                "  }\n" +
                "]";

        JsonNode foundPropsNode = JsonUtil.jsonStringToJsonNode(foundProperties);
        when(pipelineRunner.executePipelineUri("log?message=FOO|drive.read?path=file.pdf")).thenReturn(foundPropsNode);

        PipelineCliCommand uriCmd = (PipelineCliCommand) cliContext.createCommandInstance("pipeline");
        uriCmd.call(new CommandArgs("log?message=FOO|drive.read?path=file.pdf"));

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        verify(pipelineRunner, times(1)).executePipelineUri(uriCaptor.capture());

        List<String> values = uriCaptor.getAllValues();
        Assert.assertEquals("log?message=FOO|drive.read?path=file.pdf", values.get(0));

        ArgumentCaptor<Object> resultCaptor = ArgumentCaptor.forClass(Object.class);
        verify(out, times(1)).printResult(resultCaptor.capture());

        List<Object> allValues = resultCaptor.getAllValues();
        ArrayNode result = (ArrayNode) allValues.get(0);
        Assert.assertEquals("/pipeforce/enterprise/global/app/myapp/pipeline/prop1", result.get(0).get(FIELD_PATH).textValue());
        Assert.assertEquals("/pipeforce/enterprise/global/app/myapp/pipeline/prop2", result.get(1).get(FIELD_PATH).textValue());
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
