// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.mixedreality;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.mixedreality.inputs.GetObjectAnchorsAccountArgs;
import io.pulumi.azurenative.mixedreality.outputs.GetObjectAnchorsAccountResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetObjectAnchorsAccount {
    private GetObjectAnchorsAccount() {}
    /**
         * ObjectAnchorsAccount Response.
     * API Version: 2021-03-01-preview.
     * 
     *
         * ObjectAnchorsAccount Response.
     * 
     */
    public static CompletableFuture<GetObjectAnchorsAccountResult> invokeAsync(GetObjectAnchorsAccountArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:mixedreality:getObjectAnchorsAccount", TypeShape.of(GetObjectAnchorsAccountResult.class), args == null ? GetObjectAnchorsAccountArgs.Empty : args, Utilities.withVersion(options));
    }
}