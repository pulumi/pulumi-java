// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.autoscaling;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.autoscaling.inputs.GetLaunchConfigurationArgs;
import io.pulumi.awsnative.autoscaling.outputs.GetLaunchConfigurationResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetLaunchConfiguration {
    private GetLaunchConfiguration() {}
    /**
         * The AWS::AutoScaling::LaunchConfiguration resource specifies the launch configuration that can be used by an Auto Scaling group to configure Amazon EC2 instances.
     * 
     */
    public static CompletableFuture<GetLaunchConfigurationResult> invokeAsync(GetLaunchConfigurationArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:autoscaling:getLaunchConfiguration", TypeShape.of(GetLaunchConfigurationResult.class), args == null ? GetLaunchConfigurationArgs.Empty : args, Utilities.withVersion(options));
    }
}