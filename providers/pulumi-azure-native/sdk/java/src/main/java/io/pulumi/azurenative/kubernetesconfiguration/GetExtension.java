// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.kubernetesconfiguration;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.kubernetesconfiguration.inputs.GetExtensionArgs;
import io.pulumi.azurenative.kubernetesconfiguration.outputs.GetExtensionResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetExtension {
    private GetExtension() {}
    /**
         * The Extension Instance object.
     * API Version: 2020-07-01-preview.
     * 
     *
         * The Extension Instance object.
     * 
     */
    public static CompletableFuture<GetExtensionResult> invokeAsync(GetExtensionArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:kubernetesconfiguration:getExtension", TypeShape.of(GetExtensionResult.class), args == null ? GetExtensionArgs.Empty : args, Utilities.withVersion(options));
    }
}