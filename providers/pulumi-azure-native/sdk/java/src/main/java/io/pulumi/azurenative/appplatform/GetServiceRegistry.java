// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.appplatform;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.appplatform.inputs.GetServiceRegistryArgs;
import io.pulumi.azurenative.appplatform.outputs.GetServiceRegistryResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetServiceRegistry {
    private GetServiceRegistry() {}
    /**
         * Service Registry resource
     * API Version: 2022-01-01-preview.
     * 
     *
         * Service Registry resource
     * 
     */
    public static CompletableFuture<GetServiceRegistryResult> invokeAsync(GetServiceRegistryArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:appplatform:getServiceRegistry", TypeShape.of(GetServiceRegistryResult.class), args == null ? GetServiceRegistryArgs.Empty : args, Utilities.withVersion(options));
    }
}