package com.pulumi.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.Maps;
import com.pulumi.core.internal.OutputData;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.exceptions.RunException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *  Stash stores an arbitrary value in the state.
 */
public class Stash extends CustomResource {

    @Export(name = "output", refs = {Object.class})
    private Output<Object> output;

    @Export(name = "input", refs = {Object.class})
    private Output<Object> input;

    /**
     * Create a {@link Stash} resource with the given unique name, arguments.
     *
     * @param name The unique name of the stash resource.
     * @param args The arguments to use to populate this resource's properties.
     * @see Stash#Stash(String, StashArgs, CustomResourceOptions)
     */
    public Stash(String name, StashArgs args) {
        this(name, args, CustomResourceOptions.Empty);
    }

    /**
     * Create a {@link Stash} resource with the given unique name, arguments, and options.
     *
     * @param name    The unique name of the stash resource.
     * @param args    The arguments to use to populate this resource's properties.
     * @param options A bag of options that control this resource's behavior.
     */
    public Stash(String name, @Nullable StashArgs args, @Nullable CustomResourceOptions options) {
        super(
                "pulumi:index:Stash",
                name,
                args,
                options
        );
    }

    /**
     * The value saved in the state for the stash.
     *
     * @return the output value
     */
    public Output<Object> output() {
        return output;
    }

    /**
     * The most recent value passed to the stash resource.
     *
     * @return the input value
     */
    public Output<Object> input() {
        return input;
    }
}