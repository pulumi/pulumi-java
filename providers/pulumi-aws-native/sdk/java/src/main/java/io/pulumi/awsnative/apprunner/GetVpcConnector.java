// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.apprunner;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.apprunner.inputs.GetVpcConnectorArgs;
import io.pulumi.awsnative.apprunner.outputs.GetVpcConnectorResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetVpcConnector {
    private GetVpcConnector() {}
    /**
         * The AWS::AppRunner::VpcConnector resource specifies an App Runner VpcConnector.
     * 
     */
    public static CompletableFuture<GetVpcConnectorResult> invokeAsync(GetVpcConnectorArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:apprunner:getVpcConnector", TypeShape.of(GetVpcConnectorResult.class), args == null ? GetVpcConnectorArgs.Empty : args, Utilities.withVersion(options));
    }
}