package com.logabit.pipeforce.cli;

/**
 * Marker interface for all components requiring access to {@link CliContext}.
 *
 * @author sniederm
 * @since 7.0
 */
public interface CliContextAware {

    /**
     * Sets the context to be stored internally as a member.
     *
     * @param context
     */
    void setContext(CliContext context);
}
