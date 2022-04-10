// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.cloudfront;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.cloudfront.inputs.GetKeyGroupArgs;
import io.pulumi.awsnative.cloudfront.outputs.GetKeyGroupResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetKeyGroup {
    private GetKeyGroup() {}
    /**
         * Resource Type definition for AWS::CloudFront::KeyGroup
     * 
     */
    public static CompletableFuture<GetKeyGroupResult> invokeAsync(GetKeyGroupArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:cloudfront:getKeyGroup", TypeShape.of(GetKeyGroupResult.class), args == null ? GetKeyGroupArgs.Empty : args, Utilities.withVersion(options));
    }
}