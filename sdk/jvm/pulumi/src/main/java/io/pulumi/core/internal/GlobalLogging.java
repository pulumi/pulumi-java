package io.pulumi.core.internal;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static io.pulumi.core.internal.Environment.getBooleanEnvironmentVariable;

public final class GlobalLogging {
    public static final Level GlobalLevel = getBooleanEnvironmentVariable("PULUMI_JVM_LOG_VERBOSE").or(false)
            ? Level.FINEST
            : Level.SEVERE;

    // TODO: use different logger, not j.u.l!
    static {
        // Gradle uses org.gradle.api.internal.tasks.testing.junit.JULRedirector to set a ConsoleHandler
        // we want to change the logging levels to this handler to see logs in the pulumi CLI

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(GlobalLevel);
        }

        rootLogger.log(Level.INFO, "Logger initialized with global level: " + rootLogger.getLevel());
    }
}
