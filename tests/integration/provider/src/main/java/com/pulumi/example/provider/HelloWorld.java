package com.pulumi.example.provider;

import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;

class HelloWorld extends ComponentResource {
    public HelloWorld(String name, ComponentResourceOptions opts) {
        super("javap:index:HelloWorld", name, null, opts);
    }
}