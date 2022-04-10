// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.apigee_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.apigee_v1.inputs.GetDebugSessionArgs;
import io.pulumi.googlenative.apigee_v1.outputs.GetDebugSessionResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDebugSession {
    private GetDebugSession() {}
    /**
         * Retrieves a debug session.
     * 
     */
    public static CompletableFuture<GetDebugSessionResult> invokeAsync(GetDebugSessionArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:apigee/v1:getDebugSession", TypeShape.of(GetDebugSessionResult.class), args == null ? GetDebugSessionArgs.Empty : args, Utilities.withVersion(options));
    }
}