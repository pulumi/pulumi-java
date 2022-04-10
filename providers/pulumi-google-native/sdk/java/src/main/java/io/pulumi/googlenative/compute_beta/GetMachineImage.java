// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.compute_beta;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.compute_beta.inputs.GetMachineImageArgs;
import io.pulumi.googlenative.compute_beta.outputs.GetMachineImageResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetMachineImage {
    private GetMachineImage() {}
    /**
         * Returns the specified machine image. Gets a list of available machine images by making a list() request.
     * 
     */
    public static CompletableFuture<GetMachineImageResult> invokeAsync(GetMachineImageArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:compute/beta:getMachineImage", TypeShape.of(GetMachineImageResult.class), args == null ? GetMachineImageArgs.Empty : args, Utilities.withVersion(options));
    }
}