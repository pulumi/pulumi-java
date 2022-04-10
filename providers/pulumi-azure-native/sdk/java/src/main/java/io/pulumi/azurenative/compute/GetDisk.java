// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.compute;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.compute.inputs.GetDiskArgs;
import io.pulumi.azurenative.compute.outputs.GetDiskResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDisk {
    private GetDisk() {}
    /**
         * Disk resource.
     * API Version: 2020-12-01.
     * 
     *
         * Disk resource.
     * 
     */
    public static CompletableFuture<GetDiskResult> invokeAsync(GetDiskArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:compute:getDisk", TypeShape.of(GetDiskResult.class), args == null ? GetDiskArgs.Empty : args, Utilities.withVersion(options));
    }
}