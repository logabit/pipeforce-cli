package com.logabit.pipeforce.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for services which must hold a {@link CliContext} reference.
 */
public class BaseCliContextAware implements CliContextAware {

    private CliContext context;

    protected static final Logger LOG = LoggerFactory.getLogger(BaseCliContextAware.class);

    @Override
    public void setContext(CliContext context) {
        this.context = context;
    }

    public CliContext getContext() {
        return context;
    }
}
