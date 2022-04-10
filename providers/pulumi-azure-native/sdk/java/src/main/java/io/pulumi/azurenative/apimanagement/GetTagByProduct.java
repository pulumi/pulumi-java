// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.apimanagement;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.apimanagement.inputs.GetTagByProductArgs;
import io.pulumi.azurenative.apimanagement.outputs.GetTagByProductResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetTagByProduct {
    private GetTagByProduct() {}
    /**
         * Tag Contract details.
     * API Version: 2020-12-01.
     * 
     *
         * Tag Contract details.
     * 
     */
    public static CompletableFuture<GetTagByProductResult> invokeAsync(GetTagByProductArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:apimanagement:getTagByProduct", TypeShape.of(GetTagByProductResult.class), args == null ? GetTagByProductArgs.Empty : args, Utilities.withVersion(options));
    }
}