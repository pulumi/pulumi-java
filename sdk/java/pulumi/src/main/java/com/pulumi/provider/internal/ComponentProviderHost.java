package com.pulumi.provider.internal;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A host for a component provider,  managing its lifecycle and integration.
 */
public class ComponentProviderHost {
    /**
     * Metadata describing the provider, such as its name and version.
     */
    private final Metadata metadata;
    /**
     * The Java package containing the component resources for this provider.
     */
    private final Package currentPackage;

    /**
     * Constructs a new {@code ComponentProviderHost} with the specified provider name and package.
     *
     * @param name the name of the provider
     * @param currentPackage the Java package containing the component resources
     */
    public ComponentProviderHost(String name, Package currentPackage) {
        this.metadata = new Metadata(name);
        this.currentPackage = currentPackage;
    }

    /**
     * Starts the component provider host, initializing the provider and gRPC server.
     *
     * @param args the command-line arguments passed to the provider
     * @throws IOException if an I/O error occurs while starting the server
     * @throws InterruptedException if the server is interrupted while running
     */
    public void start(String[] args) throws IOException, InterruptedException {
        String engineAddress;
        try {
            engineAddress = getEngineAddress(args);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            System.out.printf("Arguments received: %s%n", String.join(", ", args));
            System.exit(1);
            return; // unreachable but makes the compiler happy about engineAddress being definitely assigned
        }

        var provider = new ComponentProvider(metadata, currentPackage);
        var server = new ResourceProviderService(engineAddress, provider);
        server.startAndBlockUntilShutdown();
    }

    /**
     * Extracts and validates the engine address from the command-line arguments.
     *
     * @param args the command-line arguments
     * @return the engine address as a string
     * @throws IllegalArgumentException if no engine address is provided or multiple non-logging arguments are found
     */
    static String getEngineAddress(String[] args) {
        var cleanArgs = new ArrayList<String>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--logtostderr")) {
                continue;
            }
            if (arg.startsWith("-v=")) {
                continue;
            }
            if (arg.equals("--tracing")) {
                i++; // Skip the next argument which is the tracing value
                continue;
            }
            if (arg.equals("--logflow")) {
                continue;
            }
            cleanArgs.add(arg);
        }

        if (cleanArgs.isEmpty()) {
            throw new IllegalArgumentException("No engine address provided in arguments");
        }
        if (cleanArgs.size() > 1) {
            throw new IllegalArgumentException(
                String.format("Expected exactly one engine address argument, but got %d non-logging arguments", cleanArgs.size())
            );
        }
        return cleanArgs.get(0);
    }
}
