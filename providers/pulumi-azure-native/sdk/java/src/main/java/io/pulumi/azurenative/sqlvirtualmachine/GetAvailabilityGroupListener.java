// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.sqlvirtualmachine;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.sqlvirtualmachine.inputs.GetAvailabilityGroupListenerArgs;
import io.pulumi.azurenative.sqlvirtualmachine.outputs.GetAvailabilityGroupListenerResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetAvailabilityGroupListener {
    private GetAvailabilityGroupListener() {}
    /**
         * A SQL Server availability group listener.
     * API Version: 2017-03-01-preview.
     * 
     *
         * A SQL Server availability group listener.
     * 
     */
    public static CompletableFuture<GetAvailabilityGroupListenerResult> invokeAsync(GetAvailabilityGroupListenerArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:sqlvirtualmachine:getAvailabilityGroupListener", TypeShape.of(GetAvailabilityGroupListenerResult.class), args == null ? GetAvailabilityGroupListenerArgs.Empty : args, Utilities.withVersion(options));
    }
}