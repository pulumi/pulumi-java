package com.pulumi.example.provider;

import com.pulumi.core.annotations.Import;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.Output;
import com.pulumi.random.RandomString;
import com.pulumi.random.RandomStringArgs;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;

class HelloWorldArgs extends ResourceArgs {
    @Import(name="length", required=true)
    private Output<Integer> length;

    public Output<Integer> length() {
        return this.length;
    }

    private HelloWorldArgs() {}
    
    public HelloWorldArgs(Output<Integer> length) {
        this.length = length;
    }
}

class HelloWorld extends ComponentResource {
    @Export(name="value", refs={String.class}, tree="[0]")
    public final Output<String> value;

    public HelloWorld(String name, HelloWorldArgs args, ComponentResourceOptions opts) {
        super("javap:index:HelloWorld", name, null, opts);

        var resOpts = CustomResourceOptions.builder()
            .parent(this)
            .build();
        var randomString = new RandomString(name, 
            RandomStringArgs.builder()
                .length(args.length())
                .special(false)
                .build(), resOpts);
        this.value = randomString.result();
    }
}
