package io.pulumi.context.internal;

import io.pulumi.Log;
import io.pulumi.context.LoggingContext;
import io.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.requireNonNull;

@InternalUse
@ParametersAreNonnullByDefault
public class LoggingContextInternal implements LoggingContext {

    private final Log log;

    public LoggingContextInternal(Log log) {
        this.log = requireNonNull(log);
    }

    @Override
    public void debug(String message) {
        this.log.debug(message);
    }

    @Override
    public void debug(String format, Object... args) {
        this.log.debug(String.format(format, args));
    }

    @Override
    public void info(String message) {
        this.log.info(message);
    }

    @Override
    public void info(String format, Object... args) {
        this.log.info(String.format(format, args));
    }

    @Override
    public void warn(String message) {
        this.log.warn(message);
    }

    @Override
    public void warn(String format, Object... args) {
        this.log.warn(String.format(format, args));
    }

    @Override
    public void error(String message) {
        this.log.error(message);
    }

    @Override
    public void error(String format, Object... args) {
        this.log.error(String.format(format, args));
    }
}
