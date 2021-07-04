package com.logabit.pipeforce.cli;

/**
 * To be thrown in case something went wrong during CLI execution.
 */
public class CliException extends RuntimeException {

    public CliException(String message) {
        super(message);
    }

    public CliException(String message, Throwable e) {
        super(message, e);
    }
}
