// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.operationalinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.operationalinsights.inputs.GetLinkedStorageAccountArgs;
import io.pulumi.azurenative.operationalinsights.outputs.GetLinkedStorageAccountResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetLinkedStorageAccount {
    private GetLinkedStorageAccount() {}
    /**
         * Linked storage accounts top level resource container.
     * API Version: 2020-08-01.
     * 
     *
         * Linked storage accounts top level resource container.
     * 
     */
    public static CompletableFuture<GetLinkedStorageAccountResult> invokeAsync(GetLinkedStorageAccountArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:operationalinsights:getLinkedStorageAccount", TypeShape.of(GetLinkedStorageAccountResult.class), args == null ? GetLinkedStorageAccountArgs.Empty : args, Utilities.withVersion(options));
    }
}