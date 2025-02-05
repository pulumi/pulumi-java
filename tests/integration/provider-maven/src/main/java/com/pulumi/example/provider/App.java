package com.pulumi.example.provider;

import java.io.IOException;
import com.pulumi.provider.internal.ResourceProviderService;

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        var server = new ResourceProviderService(new ExampleProvider());
        server.startAndBlockUntilShutdown();
    }
}

