// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.containerregistry;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.containerregistry.inputs.GetRegistryArgs;
import io.pulumi.azurenative.containerregistry.outputs.GetRegistryResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRegistry {
    private GetRegistry() {}
    /**
         * An object that represents a container registry.
     * API Version: 2019-05-01.
     * 
     *
         * An object that represents a container registry.
     * 
     */
    public static CompletableFuture<GetRegistryResult> invokeAsync(GetRegistryArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:containerregistry:getRegistry", TypeShape.of(GetRegistryResult.class), args == null ? GetRegistryArgs.Empty : args, Utilities.withVersion(options));
    }
}