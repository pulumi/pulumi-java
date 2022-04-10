// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.securityinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.securityinsights.inputs.GetEyesOnArgs;
import io.pulumi.azurenative.securityinsights.outputs.GetEyesOnResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetEyesOn {
    private GetEyesOn() {}
    /**
         * Settings with single toggle.
     * API Version: 2021-03-01-preview.
     * 
     *
         * Settings with single toggle.
     * 
     */
    public static CompletableFuture<GetEyesOnResult> invokeAsync(GetEyesOnArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:securityinsights:getEyesOn", TypeShape.of(GetEyesOnResult.class), args == null ? GetEyesOnArgs.Empty : args, Utilities.withVersion(options));
    }
}