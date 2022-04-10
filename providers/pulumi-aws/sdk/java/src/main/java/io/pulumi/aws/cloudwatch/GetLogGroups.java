// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.aws.cloudwatch;

import io.pulumi.aws.Utilities;
import io.pulumi.aws.cloudwatch.inputs.GetLogGroupsArgs;
import io.pulumi.aws.cloudwatch.outputs.GetLogGroupsResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetLogGroups {
    private GetLogGroups() {}
    /**
         * Use this data source to get a list of AWS Cloudwatch Log Groups
     * 
     * ## Example Usage
     * 
     *
         * A collection of arguments for invoking getLogGroups.
     * 
     *
         * A collection of values returned by getLogGroups.
     * 
     */
    public static CompletableFuture<GetLogGroupsResult> invokeAsync(GetLogGroupsArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws:cloudwatch/getLogGroups:getLogGroups", TypeShape.of(GetLogGroupsResult.class), args == null ? GetLogGroupsArgs.Empty : args, Utilities.withVersion(options));
    }
}