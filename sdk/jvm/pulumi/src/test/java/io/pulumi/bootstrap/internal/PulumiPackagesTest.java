package io.pulumi.bootstrap.internal;

import org.junit.jupiter.api.Test;

class PulumiPackagesTest {

    @Test
    void testFromClasspath() {
        var packages = PulumiPackages.fromClasspath(PulumiPackagesTest.class);
        System.out.println(packages);
    }
}