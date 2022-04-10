// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.aws.iam;

import io.pulumi.aws.Utilities;
import io.pulumi.aws.iam.inputs.GetPolicyArgs;
import io.pulumi.aws.iam.outputs.GetPolicyResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetPolicy {
    private GetPolicy() {}
    /**
         * This data source can be used to fetch information about a specific
     * IAM policy.
     * 
     * ## Example Usage
     * 
     *
         * A collection of arguments for invoking getPolicy.
     * 
     *
         * A collection of values returned by getPolicy.
     * 
     */
    public static CompletableFuture<GetPolicyResult> invokeAsync(@Nullable GetPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws:iam/getPolicy:getPolicy", TypeShape.of(GetPolicyResult.class), args == null ? GetPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}