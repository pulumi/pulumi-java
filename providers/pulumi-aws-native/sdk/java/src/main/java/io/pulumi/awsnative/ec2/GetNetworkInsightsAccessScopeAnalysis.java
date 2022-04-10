// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.ec2;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.ec2.inputs.GetNetworkInsightsAccessScopeAnalysisArgs;
import io.pulumi.awsnative.ec2.outputs.GetNetworkInsightsAccessScopeAnalysisResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetNetworkInsightsAccessScopeAnalysis {
    private GetNetworkInsightsAccessScopeAnalysis() {}
    /**
         * Resource schema for AWS::EC2::NetworkInsightsAccessScopeAnalysis
     * 
     */
    public static CompletableFuture<GetNetworkInsightsAccessScopeAnalysisResult> invokeAsync(GetNetworkInsightsAccessScopeAnalysisArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:ec2:getNetworkInsightsAccessScopeAnalysis", TypeShape.of(GetNetworkInsightsAccessScopeAnalysisResult.class), args == null ? GetNetworkInsightsAccessScopeAnalysisArgs.Empty : args, Utilities.withVersion(options));
    }
}