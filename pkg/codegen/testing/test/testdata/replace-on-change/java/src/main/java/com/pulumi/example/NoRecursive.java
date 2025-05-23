// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.example;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Codegen;
import com.pulumi.example.NoRecursiveArgs;
import com.pulumi.example.Utilities;
import com.pulumi.example.outputs.Rec;
import java.lang.String;
import java.util.Optional;
import javax.annotation.Nullable;

@ResourceType(type="example::NoRecursive")
public class NoRecursive extends com.pulumi.resources.CustomResource {
    @Export(name="rec", refs={Rec.class}, tree="[0]")
    private Output</* @Nullable */ Rec> rec;

    public Output<Optional<Rec>> rec() {
        return Codegen.optional(this.rec);
    }
    @Export(name="replaceMe", refs={String.class}, tree="[0]")
    private Output</* @Nullable */ String> replaceMe;

    public Output<Optional<String>> replaceMe() {
        return Codegen.optional(this.replaceMe);
    }

    /**
     *
     * @param name The _unique_ name of the resulting resource.
     */
    public NoRecursive(java.lang.String name) {
        this(name, NoRecursiveArgs.Empty);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     */
    public NoRecursive(java.lang.String name, @Nullable NoRecursiveArgs args) {
        this(name, args, null);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public NoRecursive(java.lang.String name, @Nullable NoRecursiveArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("example::NoRecursive", name, makeArgs(args, options), makeResourceOptions(options, Codegen.empty()), false);
    }

    private NoRecursive(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("example::NoRecursive", name, null, makeResourceOptions(options, id), false);
    }

    private static NoRecursiveArgs makeArgs(@Nullable NoRecursiveArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        if (options != null && options.getUrn().isPresent()) {
            return null;
        }
        return args == null ? NoRecursiveArgs.Empty : args;
    }

    private static com.pulumi.resources.CustomResourceOptions makeResourceOptions(@Nullable com.pulumi.resources.CustomResourceOptions options, @Nullable Output<java.lang.String> id) {
        var defaultOptions = com.pulumi.resources.CustomResourceOptions.builder()
            .version(Utilities.getVersion())
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
    public static NoRecursive get(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        return new NoRecursive(name, id, options);
    }
}
