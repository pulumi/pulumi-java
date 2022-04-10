// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.mediaconnect;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.mediaconnect.inputs.GetFlowSourceArgs;
import io.pulumi.awsnative.mediaconnect.outputs.GetFlowSourceResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetFlowSource {
    private GetFlowSource() {}
    /**
         * Resource schema for AWS::MediaConnect::FlowSource
     * 
     */
    public static CompletableFuture<GetFlowSourceResult> invokeAsync(GetFlowSourceArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:mediaconnect:getFlowSource", TypeShape.of(GetFlowSourceResult.class), args == null ? GetFlowSourceArgs.Empty : args, Utilities.withVersion(options));
    }
}