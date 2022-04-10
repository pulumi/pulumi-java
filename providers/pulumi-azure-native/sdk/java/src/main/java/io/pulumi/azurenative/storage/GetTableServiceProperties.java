// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.storage;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.storage.inputs.GetTableServicePropertiesArgs;
import io.pulumi.azurenative.storage.outputs.GetTableServicePropertiesResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetTableServiceProperties {
    private GetTableServiceProperties() {}
    /**
         * The properties of a storage account’s Table service.
     * API Version: 2021-02-01.
     * 
     *
         * The properties of a storage account’s Table service.
     * 
     */
    public static CompletableFuture<GetTableServicePropertiesResult> invokeAsync(GetTableServicePropertiesArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:storage:getTableServiceProperties", TypeShape.of(GetTableServicePropertiesResult.class), args == null ? GetTableServicePropertiesArgs.Empty : args, Utilities.withVersion(options));
    }
}