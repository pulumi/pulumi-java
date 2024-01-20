package com.pulumi.automation.internal;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public class LanguageRuntimeServer {

    private static final int SHUTDOWN_TIMEOUT_IN_SECONDS = 30;

    private final Logger logger;
    private final Server server;

    public LanguageRuntimeServer(Logger logger, LanguageRuntimeService service) {
        this.logger = requireNonNull(logger);
        this.server = Grpc.newServerBuilderForPort(0 /* random port */, InsecureServerCredentials.create())
                .addService(service)
                .build();
    }

    public int port() {
        var port = server.getPort();
        if (port == -1) {
            throw new UnsupportedOperationException("Cannot get LanguageRuntimeServer port, got -1");
        }
        return port;
    }

    public int start() {
        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot start LanguageRuntimeServer", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("Shutting down LanguageRuntimeServer since JVM is shutting down");
            LanguageRuntimeServer.this.shutdown();
        }));

        var port = server.getPort();
        logger.finest(String.format("LanguageRuntimeServer started, listening on %d", port));
        return port;
    }

    public void shutdown() {
        if (server != null) {
            try {
                server.shutdown().awaitTermination(SHUTDOWN_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Error while awaiting for termination of LanguageRuntimeServer", e);
            }
        }
        logger.finest("LanguageRuntimeServer shut down");
    }
}
