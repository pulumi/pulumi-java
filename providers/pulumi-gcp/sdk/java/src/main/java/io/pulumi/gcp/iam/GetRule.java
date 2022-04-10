// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.gcp.iam;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.gcp.Utilities;
import io.pulumi.gcp.iam.inputs.GetRuleArgs;
import io.pulumi.gcp.iam.outputs.GetRuleResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRule {
    private GetRule() {}
    /**
         * Use this data source to get information about a Google IAM Role.
     * 
     *
         * A collection of arguments for invoking getRule.
     * 
     *
         * A collection of values returned by getRule.
     * 
     */
    public static CompletableFuture<GetRuleResult> invokeAsync(GetRuleArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("gcp:iam/getRule:getRule", TypeShape.of(GetRuleResult.class), args == null ? GetRuleArgs.Empty : args, Utilities.withVersion(options));
    }
}