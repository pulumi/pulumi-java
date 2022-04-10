// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.web;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.web.inputs.GetWebAppDeploymentSlotArgs;
import io.pulumi.azurenative.web.outputs.GetWebAppDeploymentSlotResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetWebAppDeploymentSlot {
    private GetWebAppDeploymentSlot() {}
    /**
         * User credentials used for publishing activity.
     * API Version: 2020-12-01.
     * 
     *
         * User credentials used for publishing activity.
     * 
     */
    public static CompletableFuture<GetWebAppDeploymentSlotResult> invokeAsync(GetWebAppDeploymentSlotArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:web:getWebAppDeploymentSlot", TypeShape.of(GetWebAppDeploymentSlotResult.class), args == null ? GetWebAppDeploymentSlotArgs.Empty : args, Utilities.withVersion(options));
    }
}