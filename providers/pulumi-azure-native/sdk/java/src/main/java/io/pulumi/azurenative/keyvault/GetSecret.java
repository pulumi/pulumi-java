// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.keyvault;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.keyvault.inputs.GetSecretArgs;
import io.pulumi.azurenative.keyvault.outputs.GetSecretResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetSecret {
    private GetSecret() {}
    /**
         * Resource information with extended details.
     * API Version: 2019-09-01.
     * 
     *
         * Resource information with extended details.
     * 
     */
    public static CompletableFuture<GetSecretResult> invokeAsync(GetSecretArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:keyvault:getSecret", TypeShape.of(GetSecretResult.class), args == null ? GetSecretArgs.Empty : args, Utilities.withVersion(options));
    }
}