// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.logic;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.logic.inputs.GetWorkflowAccessKeyArgs;
import io.pulumi.azurenative.logic.outputs.GetWorkflowAccessKeyResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetWorkflowAccessKey {
    private GetWorkflowAccessKey() {}
    /**
         * API Version: 2015-02-01-preview.
     * 
     */
    public static CompletableFuture<GetWorkflowAccessKeyResult> invokeAsync(GetWorkflowAccessKeyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:logic:getWorkflowAccessKey", TypeShape.of(GetWorkflowAccessKeyResult.class), args == null ? GetWorkflowAccessKeyArgs.Empty : args, Utilities.withVersion(options));
    }
}