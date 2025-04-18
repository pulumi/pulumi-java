// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.example;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Codegen;
import com.pulumi.example.CatArgs;
import com.pulumi.example.God;
import com.pulumi.example.Utilities;
import com.pulumi.example.outputs.Toy;
import java.lang.String;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

@ResourceType(type="example::Cat")
public class Cat extends com.pulumi.resources.CustomResource {
    @Export(name="foes", refs={Map.class,String.class,Toy.class}, tree="[0,1,2]")
    private Output</* @Nullable */ Map<String,Toy>> foes;

    public Output<Optional<Map<String,Toy>>> foes() {
        return Codegen.optional(this.foes);
    }
    @Export(name="friends", refs={List.class,Toy.class}, tree="[0,1]")
    private Output</* @Nullable */ List<Toy>> friends;

    public Output<Optional<List<Toy>>> friends() {
        return Codegen.optional(this.friends);
    }
    @Export(name="name", refs={String.class}, tree="[0]")
    private Output</* @Nullable */ String> name;

    public Output<Optional<String>> name() {
        return Codegen.optional(this.name);
    }
    @Export(name="other", refs={God.class}, tree="[0]")
    private Output</* @Nullable */ God> other;

    public Output<Optional<God>> other() {
        return Codegen.optional(this.other);
    }
    @Export(name="toy", refs={Toy.class}, tree="[0]")
    private Output</* @Nullable */ Toy> toy;

    public Output<Optional<Toy>> toy() {
        return Codegen.optional(this.toy);
    }

    /**
     *
     * @param name The _unique_ name of the resulting resource.
     */
    public Cat(java.lang.String name) {
        this(name, CatArgs.Empty);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     */
    public Cat(java.lang.String name, @Nullable CatArgs args) {
        this(name, args, null);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public Cat(java.lang.String name, @Nullable CatArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("example::Cat", name, makeArgs(args, options), makeResourceOptions(options, Codegen.empty()), false);
    }

    private Cat(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("example::Cat", name, null, makeResourceOptions(options, id), false);
    }

    private static CatArgs makeArgs(@Nullable CatArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        if (options != null && options.getUrn().isPresent()) {
            return null;
        }
        return args == null ? CatArgs.Empty : args;
    }

    private static com.pulumi.resources.CustomResourceOptions makeResourceOptions(@Nullable com.pulumi.resources.CustomResourceOptions options, @Nullable Output<java.lang.String> id) {
        var defaultOptions = com.pulumi.resources.CustomResourceOptions.builder()
            .version(Utilities.getVersion())
            .additionalSecretOutputs(List.of(
                "name"
            ))
            .build();
        return com.pulumi.resources.CustomResourceOptions.merge(defaultOptions, options, id);
    }

    /**
     * Get an existing Host resource's state with the given name, ID, and optional extra
     * properties used to qualify the lookup.
     *
     * @param name The _unique_ name of the resulting resource.
     * @param id The _unique_ provider ID of the resource to lookup.
     * @param options Optional settings to control the behavior of the CustomResource.
     */
    public static Cat get(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        return new Cat(name, id, options);
    }
}
