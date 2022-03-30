package io.pulumi.bootstrap.internal;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import io.pulumi.bootstrap.internal.PulumiPackages.PulumiPackage;
import io.pulumi.core.internal.annotations.InternalUse;

import java.util.Arrays;

@InternalUse
public class Main {

    private static final String PACKAGES_CMD = "packages";

    public static void main(String[] args) {
        var packagesCmd = Arrays.asList(args).contains(PACKAGES_CMD);
        if (packagesCmd) {
            ImmutableMap<String, PulumiPackage> packages = PulumiPackages.fromClasspath(Main.class);
            var gson = new Gson();
            System.out.println(gson.toJson(packages.values()));
        } else {
            System.err.printf("Unknown command '%s', available commands: %s\n",
                    String.join(" ", args), String.join(", ", PACKAGES_CMD)
            );
            System.exit(1);
        }
    }
}
