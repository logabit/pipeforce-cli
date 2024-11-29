package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.CommandArgs;
import com.logabit.pipeforce.common.net.ClientPipeforceURIResolver;
import com.logabit.pipeforce.common.net.Request;
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
    protected ClientPipeforceURIResolver resolver;

    @Test
    public void testRunCommand() throws Exception {

        CommandCliCommand remoteRun = (CommandCliCommand) cliContext.createCommandInstance("command");
        remoteRun.call(new CommandArgs("cmdName", "message=FOO", "longtext='text inside ticks'"));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<Class> typeCaptor = ArgumentCaptor.forClass(Class.class);
        verify(resolver, times(1)).resolve(requestCaptor.capture(), typeCaptor.capture());

        Request request = requestCaptor.getValue();

        /**
         *  The command names + params are passed as cli args so they are dynamic.
         *  Also, abstract class cannot be passed here. So we need to keep $uri string here.
         */
        Assert.assertEquals("$uri:command:cmdName", request.getUri());
        Assert.assertEquals("FOO", request.getParams().get("message"));
        Assert.assertEquals("text inside ticks", request.getParams().get("longtext"));
    }
}
