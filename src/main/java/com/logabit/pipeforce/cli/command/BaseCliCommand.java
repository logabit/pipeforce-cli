package com.logabit.pipeforce.cli.command;

import com.logabit.pipeforce.cli.CliContext;
import com.logabit.pipeforce.cli.service.ConfigCliService;
import com.logabit.pipeforce.cli.service.OutputCliService;

/**
 * Base class for all CLI commands.
 *
 * @author sniederm
 * @since 7.0
 */
public abstract class BaseCliCommand implements ICliCommand {

    private CliContext context;

    protected OutputCliService out;

    protected ConfigCliService config;

    public void setContext(CliContext context) {
        this.context = context;
        this.out = context.getOutputService();
        this.config = context.getConfigService();
    }

    public CliContext getContext() {
        return context;
    }
}
