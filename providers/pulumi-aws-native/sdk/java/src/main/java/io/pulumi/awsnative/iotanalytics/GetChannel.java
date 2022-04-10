// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.iotanalytics;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.iotanalytics.inputs.GetChannelArgs;
import io.pulumi.awsnative.iotanalytics.outputs.GetChannelResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetChannel {
    private GetChannel() {}
    /**
         * Resource Type definition for AWS::IoTAnalytics::Channel
     * 
     */
    public static CompletableFuture<GetChannelResult> invokeAsync(GetChannelArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:iotanalytics:getChannel", TypeShape.of(GetChannelResult.class), args == null ? GetChannelArgs.Empty : args, Utilities.withVersion(options));
    }
}