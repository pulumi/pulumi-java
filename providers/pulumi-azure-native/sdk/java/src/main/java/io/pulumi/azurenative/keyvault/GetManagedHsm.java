// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.keyvault;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.keyvault.inputs.GetManagedHsmArgs;
import io.pulumi.azurenative.keyvault.outputs.GetManagedHsmResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetManagedHsm {
    private GetManagedHsm() {}
    /**
         * Resource information with extended details.
     * API Version: 2021-06-01-preview.
     * 
     *
         * Resource information with extended details.
     * 
     */
    public static CompletableFuture<GetManagedHsmResult> invokeAsync(GetManagedHsmArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:keyvault:getManagedHsm", TypeShape.of(GetManagedHsmResult.class), args == null ? GetManagedHsmArgs.Empty : args, Utilities.withVersion(options));
    }
}