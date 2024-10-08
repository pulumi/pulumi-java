// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.example;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.ResourceType;
import com.pulumi.core.internal.Codegen;
import com.pulumi.example.ComponentArgs;
import com.pulumi.example.Utilities;
import com.pulumi.example.outputs.Foo;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

@ResourceType(type="example::Component")
public class Component extends com.pulumi.resources.ComponentResource {
    @Export(name="a", refs={Boolean.class}, tree="[0]")
    private Output<Boolean> a;

    public Output<Boolean> a() {
        return this.a;
    }
    @Export(name="b", refs={Boolean.class}, tree="[0]")
    private Output</* @Nullable */ Boolean> b;

    public Output<Optional<Boolean>> b() {
        return Codegen.optional(this.b);
    }
    @Export(name="bar", refs={Foo.class}, tree="[0]")
    private Output</* @Nullable */ Foo> bar;

    public Output<Optional<Foo>> bar() {
        return Codegen.optional(this.bar);
    }
    @Export(name="baz", refs={List.class,Foo.class}, tree="[0,1]")
    private Output</* @Nullable */ List<Foo>> baz;

    public Output<Optional<List<Foo>>> baz() {
        return Codegen.optional(this.baz);
    }
    @Export(name="c", refs={Integer.class}, tree="[0]")
    private Output<Integer> c;

    public Output<Integer> c() {
        return this.c;
    }
    @Export(name="d", refs={Integer.class}, tree="[0]")
    private Output</* @Nullable */ Integer> d;

    public Output<Optional<Integer>> d() {
        return Codegen.optional(this.d);
    }
    @Export(name="e", refs={String.class}, tree="[0]")
    private Output<String> e;

    public Output<String> e() {
        return this.e;
    }
    @Export(name="f", refs={String.class}, tree="[0]")
    private Output</* @Nullable */ String> f;

    public Output<Optional<String>> f() {
        return Codegen.optional(this.f);
    }
    @Export(name="foo", refs={Foo.class}, tree="[0]")
    private Output</* @Nullable */ Foo> foo;

    public Output<Optional<Foo>> foo() {
        return Codegen.optional(this.foo);
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
        super("example::Component", name, makeArgs(args, options), makeResourceOptions(options, Codegen.empty()), true);
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
