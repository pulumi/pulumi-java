package com.pulumi.provider.internal;

import java.io.IOException;

public class ComponentProviderHost {
    private final Metadata metadata;
    private final Package currentPackage;

    public ComponentProviderHost(Metadata metadata, Package currentPackage) {
        this.metadata = metadata;
        this.currentPackage = currentPackage;
    }

    public void start(String[] args) throws IOException, InterruptedException {
        var engineAddress = getEngineAddress(args);
        var provider = new ComponentProvider(metadata, currentPackage);
        var server = new ResourceProviderService(engineAddress, provider);
        server.startAndBlockUntilShutdown();
    }

    private String getEngineAddress(String[] args) {
        var engineAddress = args.length > 0 ? args[0] : null;
        if (engineAddress == null) {
            System.out.println("No engine address provided in arguments");
            System.exit(1);
        }
        return engineAddress;
    }
}
