package com.logabit.pipeforce.cli.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.cli.uri.CliPipeforceURIResolver;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.logabit.pipeforce.cli.uri.CliPipeforceURIResolver.Method.GET;
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
    protected CliPipeforceURIResolver resolver;

    @Test
    public void testRunCommand() throws Exception {

        CommandCliCommand remoteRun = (CommandCliCommand) cliContext.createCommandInstance("command");
        remoteRun.call(new CommandArgs("cmdName", "message=FOO", "longtext='text inside ticks'"));

        ArgumentCaptor<JsonNode> nodeCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(resolver, times(1)).resolveToObject(
                GET, "$uri:command:cmdName?message=FOO&longtext=text+inside+ticks", String.class);
    }
}
