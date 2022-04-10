// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.ssm;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.ssm.inputs.GetAssociationArgs;
import io.pulumi.awsnative.ssm.outputs.GetAssociationResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetAssociation {
    private GetAssociation() {}
    /**
         * The AWS::SSM::Association resource associates an SSM document in AWS Systems Manager with EC2 instances that contain a configuration agent to process the document.
     * 
     */
    public static CompletableFuture<GetAssociationResult> invokeAsync(GetAssociationArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:ssm:getAssociation", TypeShape.of(GetAssociationResult.class), args == null ? GetAssociationArgs.Empty : args, Utilities.withVersion(options));
    }
}