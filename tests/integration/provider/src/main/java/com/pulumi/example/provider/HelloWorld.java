package com.pulumi.example.provider;

import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.core.Output;
import com.pulumi.random.RandomString;
import com.pulumi.random.RandomStringArgs;
import javax.annotation.Nullable;
import com.pulumi.core.annotations.Import;


class HelloWorldArgs extends ResourceArgs {
    @Import(name = "length")
    @Nullable
    public final Output<Integer> length;

    public HelloWorldArgs(@Nullable Output<Integer> length) {
        this.length = length;
    }
}

class HelloWorld extends ComponentResource {
    public final Output<String> value;

    public HelloWorld(String name, HelloWorldArgs args, ComponentResourceOptions opts) {
        super("javap:index:HelloWorld", name, null, opts);

        var resOpts = CustomResourceOptions.builder()
            .parent(this)
            .build();
        var randomString = new com.pulumi.random.RandomString(name, 
            com.pulumi.random.RandomStringArgs.builder()
                .length(args.length)
                .special(false)
                .build(), resOpts);
        this.value = randomString.result();
    }
}
