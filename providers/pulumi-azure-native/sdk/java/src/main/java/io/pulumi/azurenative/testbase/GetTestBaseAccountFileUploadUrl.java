// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.testbase;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.testbase.inputs.GetTestBaseAccountFileUploadUrlArgs;
import io.pulumi.azurenative.testbase.outputs.GetTestBaseAccountFileUploadUrlResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetTestBaseAccountFileUploadUrl {
    private GetTestBaseAccountFileUploadUrl() {}
    /**
         * The URL response
     * API Version: 2020-12-16-preview.
     * 
     *
         * The URL response
     * 
     */
    public static CompletableFuture<GetTestBaseAccountFileUploadUrlResult> invokeAsync(GetTestBaseAccountFileUploadUrlArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:testbase:getTestBaseAccountFileUploadUrl", TypeShape.of(GetTestBaseAccountFileUploadUrlResult.class), args == null ? GetTestBaseAccountFileUploadUrlArgs.Empty : args, Utilities.withVersion(options));
    }
}