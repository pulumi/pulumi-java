// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.example;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Codegen;
import com.pulumi.example.BasicResourceArgs;
import com.pulumi.example.Utilities;
import java.lang.String;
import javax.annotation.Nullable;

@ResourceType(type="example:index:BasicResource")
public class BasicResource extends com.pulumi.resources.CustomResource {
    @Export(name="bar", refs={String.class}, tree="[0]")
    private Output<String> bar;

    public Output<String> bar() {
        return this.bar;
    }

    /**
     *
     * @param name The _unique_ name of the resulting resource.
     */
    public BasicResource(java.lang.String name) {
        this(name, BasicResourceArgs.Empty);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     */
    public BasicResource(java.lang.String name, BasicResourceArgs args) {
        this(name, args, null);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public BasicResource(java.lang.String name, BasicResourceArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("example:index:BasicResource", name, makeArgs(args, options), makeResourceOptions(options, Codegen.empty()), false);
    }

    private BasicResource(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("example:index:BasicResource", name, null, makeResourceOptions(options, id), false);
    }

    private static BasicResourceArgs makeArgs(BasicResourceArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        if (options != null && options.getUrn().isPresent()) {
            return null;
        }
        return args == null ? BasicResourceArgs.Empty : args;
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
    public static BasicResource get(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        return new BasicResource(name, id, options);
    }
}
