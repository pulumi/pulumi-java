// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.customerinsights;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.customerinsights.inputs.GetImageUploadUrlForDataArgs;
import io.pulumi.azurenative.customerinsights.outputs.GetImageUploadUrlForDataResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetImageUploadUrlForData {
    private GetImageUploadUrlForData() {}
    /**
         * The image definition.
     * API Version: 2017-04-26.
     * 
     *
         * The image definition.
     * 
     */
    public static CompletableFuture<GetImageUploadUrlForDataResult> invokeAsync(GetImageUploadUrlForDataArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:customerinsights:getImageUploadUrlForData", TypeShape.of(GetImageUploadUrlForDataResult.class), args == null ? GetImageUploadUrlForDataArgs.Empty : args, Utilities.withVersion(options));
    }
}