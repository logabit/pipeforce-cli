package com.logabit.pipeforce.cli;

public class CliException extends RuntimeException {

    public CliException(String message) {
        super(message);
    }

    public CliException(String message, Throwable e) {
        super(message, e);
    }
}
