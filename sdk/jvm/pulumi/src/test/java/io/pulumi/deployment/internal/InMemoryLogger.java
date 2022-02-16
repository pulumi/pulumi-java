package io.pulumi.deployment.internal;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class InMemoryLogger extends Logger implements CountingLogger {

    private final Queue<String> messages = new ConcurrentLinkedQueue<>();

    private InMemoryLogger(String name, @Nullable String resourceBundleName) {
        super(name, resourceBundleName);
    }

    public static InMemoryLogger getLogger(String name) {
        return getLogger(Level.INFO, name);
    }

    public static InMemoryLogger getLogger(Level newLevel, String name) {
        var manager = LogManager.getLogManager();
        var logger = manager.getLogger(name);
        if (logger == null) {
            logger = new InMemoryLogger(name, null);
            manager.addLogger(logger);
        } else {
            if (!(logger instanceof InMemoryLogger)) {
                throw new IllegalStateException(String.format(
                        "Expected logger named: '%s' to be of type InMemoryLogger, got: '%s'",
                        name, logger.getClass().getTypeName()
                ));
            }
        }
        logger.setLevel(newLevel);
        return (InMemoryLogger) logger;
    }

    public Collection<String> getMessages() {
        return messages;
    }

    @Override
    public void log(LogRecord record) {
        this.messages.add(String.format("%s %s %s", record.getLevel(), record.getSequenceNumber(), record.getMessage()));
        super.log(record);
    }

    @Override
    public int getErrorCount() {
        return this.messages.size();
    }

    @Override
    public boolean hasLoggedErrors() {
        return this.messages.size() > 0;
    }
}
