package com.pulumi.example.provider;

import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.random.RandomString;
import com.pulumi.random.RandomStringArgs;

class HelloWorld extends ComponentResource {
    public HelloWorld(String name, ComponentResourceOptions opts) {
        super("javap:index:HelloWorld", name, null, opts);

        // Create a RandomString resource
        var resOpts = CustomResourceOptions.builder()
            .parent(this)
            .build();
        var randomString = new com.pulumi.random.RandomString(name, 
            com.pulumi.random.RandomStringArgs.builder()
                .length(16)
                .special(false)
                .build(), resOpts);
    }
}
