package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.pipeline.PipelineRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link CommandCliCommand}.
 *
 * @author sniederm
 * @since 2.8
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandCliCommandTest {

    @InjectMocks
    private final CliContext cliContext = new CliContext();

    @Mock
    private PipelineRunner pipelineRunner;

    @Test
    public void testRunCommand() throws Exception {

        CommandCliCommand remoteRun = (CommandCliCommand) cliContext.createCommandInstance("command");
        remoteRun.call(new CommandArgs("cmdName", "message=FOO", "longtext='text inside ticks'"));

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(pipelineRunner, times(1)).executePipelineJsonNode(nodeCaptor.capture());

        JsonNode pipelineNode = nodeCaptor.getValue();
        Assert.assertEquals("FOO", pipelineNode.get("pipeline").get(0).get("cmdName").get("message").textValue());
        Assert.assertEquals("text inside ticks", pipelineNode.get("pipeline").get(0).get("cmdName").get("longtext").textValue());
    }
}
