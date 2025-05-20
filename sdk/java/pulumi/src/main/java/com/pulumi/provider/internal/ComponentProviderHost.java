package com.pulumi.provider.internal;

import java.io.IOException;
import java.util.ArrayList;

public class ComponentProviderHost {
    private final Metadata metadata;
    private final Package currentPackage;

    public ComponentProviderHost(String name, Package currentPackage) {
        this.metadata = new Metadata(name);
        this.currentPackage = currentPackage;
    }

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
