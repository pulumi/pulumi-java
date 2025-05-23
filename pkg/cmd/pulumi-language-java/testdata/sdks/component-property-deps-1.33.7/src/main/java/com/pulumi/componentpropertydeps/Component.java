// *** WARNING: this file was generated by pulumi-language-java. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.componentpropertydeps;

import com.pulumi.componentpropertydeps.ComponentArgs;
import com.pulumi.componentpropertydeps.Utilities;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Codegen;
import java.lang.String;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A component resource that accepts a list of resources. The construct request&#39;s property dependencies are returned as an output.
 * 
 */
@ResourceType(type="component-property-deps:index:Component")
public class Component extends com.pulumi.resources.ComponentResource {
    @Export(name="propertyDeps", refs={Map.class,String.class,List.class}, tree="[0,1,[2,1]]")
    private Output<Map<String,List<String>>> propertyDeps;

    public Output<Map<String,List<String>>> propertyDeps() {
        return this.propertyDeps;
    }

    /**
     *
     * @param name The _unique_ name of the resulting resource.
     */
    public Component(java.lang.String name) {
        this(name, ComponentArgs.Empty);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     */
    public Component(java.lang.String name, ComponentArgs args) {
        this(name, args, null);
    }
    /**
     *
     * @param name The _unique_ name of the resulting resource.
     * @param args The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public Component(java.lang.String name, ComponentArgs args, @Nullable com.pulumi.resources.ComponentResourceOptions options) {
        super("component-property-deps:index:Component", name, makeArgs(args, options), makeResourceOptions(options, Codegen.empty()), true);
    }

    private static ComponentArgs makeArgs(ComponentArgs args, @Nullable com.pulumi.resources.ComponentResourceOptions options) {
        if (options != null && options.getUrn().isPresent()) {
            return null;
        }
        return args == null ? ComponentArgs.Empty : args;
    }

    private static com.pulumi.resources.ComponentResourceOptions makeResourceOptions(@Nullable com.pulumi.resources.ComponentResourceOptions options, @Nullable Output<java.lang.String> id) {
        var defaultOptions = com.pulumi.resources.ComponentResourceOptions.builder()
            .version(Utilities.getVersion())
            .build();
        return com.pulumi.resources.ComponentResourceOptions.merge(defaultOptions, options, id);
    }

}
