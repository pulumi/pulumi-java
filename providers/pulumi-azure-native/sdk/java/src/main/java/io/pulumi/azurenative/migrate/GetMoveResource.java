// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.migrate;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.migrate.inputs.GetMoveResourceArgs;
import io.pulumi.azurenative.migrate.outputs.GetMoveResourceResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetMoveResource {
    private GetMoveResource() {}
    /**
         * Defines the move resource.
     * API Version: 2021-01-01.
     * 
     *
         * Defines the move resource.
     * 
     */
    public static CompletableFuture<GetMoveResourceResult> invokeAsync(GetMoveResourceArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:migrate:getMoveResource", TypeShape.of(GetMoveResourceResult.class), args == null ? GetMoveResourceArgs.Empty : args, Utilities.withVersion(options));
    }
}