// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.devspaces;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.devspaces.inputs.ListControllerConnectionDetailsArgs;
import io.pulumi.azurenative.devspaces.outputs.ListControllerConnectionDetailsResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class ListControllerConnectionDetails {
    private ListControllerConnectionDetails() {}
    /**
         * API Version: 2019-04-01.
     * 
     */
    public static CompletableFuture<ListControllerConnectionDetailsResult> invokeAsync(ListControllerConnectionDetailsArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:devspaces:listControllerConnectionDetails", TypeShape.of(ListControllerConnectionDetailsResult.class), args == null ? ListControllerConnectionDetailsArgs.Empty : args, Utilities.withVersion(options));
    }
}