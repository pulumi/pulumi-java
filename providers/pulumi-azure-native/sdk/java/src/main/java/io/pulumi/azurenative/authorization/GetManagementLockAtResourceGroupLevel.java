// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.authorization;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.authorization.inputs.GetManagementLockAtResourceGroupLevelArgs;
import io.pulumi.azurenative.authorization.outputs.GetManagementLockAtResourceGroupLevelResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetManagementLockAtResourceGroupLevel {
    private GetManagementLockAtResourceGroupLevel() {}
    /**
         * The lock information.
     * API Version: 2017-04-01.
     * 
     *
         * The lock information.
     * 
     */
    public static CompletableFuture<GetManagementLockAtResourceGroupLevelResult> invokeAsync(GetManagementLockAtResourceGroupLevelArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:authorization:getManagementLockAtResourceGroupLevel", TypeShape.of(GetManagementLockAtResourceGroupLevelResult.class), args == null ? GetManagementLockAtResourceGroupLevelArgs.Empty : args, Utilities.withVersion(options));
    }
}