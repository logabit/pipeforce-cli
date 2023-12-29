package com.logabit.pipeforce.cli.command;

/**
 * Represents the result after a command has been executed.
 *
 * @deprecated Merge with {@link com.logabit.pipeforce.common.pipeline.Result}.
 */
@Deprecated
public class CommandResult {

    private int statusCode;

    private String message;

    private Object resultValue;

    public CommandResult(int statusCode, String message, Object resultValue) {
        this.statusCode = statusCode;
        this.message = message;
        this.resultValue = resultValue;
    }

    public String getMessage() {
        return message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Object getResultValue() {
        return resultValue;
    }
}
