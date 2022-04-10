// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.connectedvmwarevsphere;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.connectedvmwarevsphere.inputs.GetVirtualMachineArgs;
import io.pulumi.azurenative.connectedvmwarevsphere.outputs.GetVirtualMachineResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetVirtualMachine {
    private GetVirtualMachine() {}
    /**
         * Define the virtualMachine.
     * API Version: 2020-10-01-preview.
     * 
     *
         * Define the virtualMachine.
     * 
     */
    public static CompletableFuture<GetVirtualMachineResult> invokeAsync(GetVirtualMachineArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:connectedvmwarevsphere:getVirtualMachine", TypeShape.of(GetVirtualMachineResult.class), args == null ? GetVirtualMachineArgs.Empty : args, Utilities.withVersion(options));
    }
}