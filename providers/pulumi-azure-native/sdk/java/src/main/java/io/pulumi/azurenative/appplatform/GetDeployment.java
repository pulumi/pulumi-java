// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.appplatform;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.appplatform.inputs.GetDeploymentArgs;
import io.pulumi.azurenative.appplatform.outputs.GetDeploymentResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDeployment {
    private GetDeployment() {}
    /**
         * Deployment resource payload
     * API Version: 2020-07-01.
     * 
     *
         * Deployment resource payload
     * 
     */
    public static CompletableFuture<GetDeploymentResult> invokeAsync(GetDeploymentArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:appplatform:getDeployment", TypeShape.of(GetDeploymentResult.class), args == null ? GetDeploymentArgs.Empty : args, Utilities.withVersion(options));
    }
}