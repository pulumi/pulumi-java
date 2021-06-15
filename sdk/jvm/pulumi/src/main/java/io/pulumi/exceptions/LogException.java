package io.pulumi.exceptions;

/**
 * Special exception we throw if we had a problem actually logging a message to the engine
 * error rpc endpoint. In this case, we have no choice but to tear ourselves down reporting
 * whatever we can to the console instead.
 */
public class LogException extends RuntimeException {
    public LogException(Throwable cause) {
        super("Error occurred during logging", cause);
    }
}
