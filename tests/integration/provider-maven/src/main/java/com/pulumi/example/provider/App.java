package com.pulumi.example.provider;

import java.io.IOException;
import java.util.logging.Logger;
import com.pulumi.provider.internal.ResourceProviderService;

public class App {
    private static final Logger logger = Logger.getLogger(App.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        // First argument should be the engine address
        var engineAddress = args.length > 0 ? args[0] : null;
        if (engineAddress == null) {
            logger.warning("No engine address provided in arguments");
        }
        var server = new ResourceProviderService(engineAddress, new ExampleProvider());
        server.startAndBlockUntilShutdown();
    }
}

