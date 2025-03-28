// *** WARNING: this file was generated by pulumi-language-java. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.simpleinvoke;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Codegen;
import com.pulumi.simpleinvoke.StringResourceArgs;
import com.pulumi.simpleinvoke.Utilities;
import java.lang.String;
import javax.annotation.Nullable;

@ResourceType(type="simple-invoke:index:StringResource")
public class StringResource extends com.pulumi.resources.CustomResource {
    @Export(name="text", refs={String.class}, tree="[0]")
    private Output<String> text;

    public Output<String> text() {
        return this.text;
    }

    /**
     *
     * @param name The _unique_ name of the resulting resource.
     */
    public StringResource(java.lang.String name) {
        this(name, StringResourceArgs.Empty);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     */
    public StringResource(java.lang.String name, StringResourceArgs args) {
        this(name, args, null);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public StringResource(java.lang.String name, StringResourceArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("simple-invoke:index:StringResource", name, makeArgs(args, options), makeResourceOptions(options, Codegen.empty()), false);
    }

    private StringResource(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        super("simple-invoke:index:StringResource", name, null, makeResourceOptions(options, id), false);
    }

    private static StringResourceArgs makeArgs(StringResourceArgs args, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        if (options != null && options.getUrn().isPresent()) {
            return null;
        }
        return args == null ? StringResourceArgs.Empty : args;
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
    public static StringResource get(java.lang.String name, Output<java.lang.String> id, @Nullable com.pulumi.resources.CustomResourceOptions options) {
        return new StringResource(name, id, options);
    }
}
