// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.datafactory;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.datafactory.inputs.GetIntegrationRuntimeStatusArgs;
import io.pulumi.azurenative.datafactory.outputs.GetIntegrationRuntimeStatusResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetIntegrationRuntimeStatus {
    private GetIntegrationRuntimeStatus() {}
    /**
         * Integration runtime status response.
     * API Version: 2018-06-01.
     * 
     *
         * Integration runtime status response.
     * 
     */
    public static CompletableFuture<GetIntegrationRuntimeStatusResult> invokeAsync(GetIntegrationRuntimeStatusArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:datafactory:getIntegrationRuntimeStatus", TypeShape.of(GetIntegrationRuntimeStatusResult.class), args == null ? GetIntegrationRuntimeStatusArgs.Empty : args, Utilities.withVersion(options));
    }
}