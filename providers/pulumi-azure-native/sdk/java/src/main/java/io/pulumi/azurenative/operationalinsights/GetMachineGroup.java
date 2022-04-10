// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.operationalinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.operationalinsights.inputs.GetMachineGroupArgs;
import io.pulumi.azurenative.operationalinsights.outputs.GetMachineGroupResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetMachineGroup {
    private GetMachineGroup() {}
    /**
         * A user-defined logical grouping of machines.
     * API Version: 2015-11-01-preview.
     * 
     *
         * A user-defined logical grouping of machines.
     * 
     */
    public static CompletableFuture<GetMachineGroupResult> invokeAsync(GetMachineGroupArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:operationalinsights:getMachineGroup", TypeShape.of(GetMachineGroupResult.class), args == null ? GetMachineGroupArgs.Empty : args, Utilities.withVersion(options));
    }
}