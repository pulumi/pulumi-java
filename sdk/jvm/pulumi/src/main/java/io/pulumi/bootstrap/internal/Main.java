package io.pulumi.bootstrap.internal;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import io.pulumi.bootstrap.internal.PulumiPackages.PulumiPackage;
import io.pulumi.core.internal.annotations.InternalUse;

import java.util.Arrays;

@InternalUse
public class Main {

    public static void main(String[] args) {
        var packagesCmd = Arrays.asList(args).contains("packages");
        if (packagesCmd) {
            ImmutableMap<String, PulumiPackage> packages = PulumiPackages.fromClasspath(Main.class);
            var gson = new Gson();
            System.out.println(gson.toJson(packages));
        } else {
            System.exit(1);
        }
    }
}
