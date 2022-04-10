// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.storage;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.storage.inputs.GetBlobServicePropertiesArgs;
import io.pulumi.azurenative.storage.outputs.GetBlobServicePropertiesResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetBlobServiceProperties {
    private GetBlobServiceProperties() {}
    /**
         * The properties of a storage account’s Blob service.
     * API Version: 2021-02-01.
     * 
     *
         * The properties of a storage account’s Blob service.
     * 
     */
    public static CompletableFuture<GetBlobServicePropertiesResult> invokeAsync(GetBlobServicePropertiesArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:storage:getBlobServiceProperties", TypeShape.of(GetBlobServicePropertiesResult.class), args == null ? GetBlobServicePropertiesArgs.Empty : args, Utilities.withVersion(options));
    }
}