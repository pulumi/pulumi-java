package com.pulumi.bootstrap.internal;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.provider.internal.AnalyzerService;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

import static com.pulumi.core.internal.Environment.getEnvironmentVariable;

@InternalUse
public class Main {

    private static final String PACKAGES_CMD = "packages";
    private static final String ANALYZER_CMD = "analyzer";

    public static void main(String[] args) throws IOException, InterruptedException {
        var argsAsList = Arrays.asList(args);
        if (argsAsList.contains(PACKAGES_CMD)) {
            ImmutableMap<String, PulumiPlugin> packages = PulumiPlugins.fromClasspath(Main.class);
            var gson = new Gson();
            System.out.println(gson.toJson(packages.values()));
        } else if (argsAsList.contains(ANALYZER_CMD)) {
            Function<RuntimeException, RuntimeException> startErrorSupplier =
                    e -> new IllegalArgumentException(
                            "Program run without the Pulumi engine available; re-run using the `pulumi` CLI", e
                    );

            var engineTarget = getEnvironmentVariable("PULUMI_ENGINE").orThrow(startErrorSupplier);
            var server = new AnalyzerService(engineTarget);
            server.startAndBlockUntilShutdown();
        } else {
            System.err.printf("Unknown command '%s', available commands: %s\n",
                    String.join(" ", args), String.join(", ", PACKAGES_CMD, ANALYZER_CMD)
            );
            System.exit(1);
        }
    }
}
