// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.deploymentmanager_v2;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.deploymentmanager_v2.inputs.GetDeploymentIamPolicyArgs;
import io.pulumi.googlenative.deploymentmanager_v2.outputs.GetDeploymentIamPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDeploymentIamPolicy {
    private GetDeploymentIamPolicy() {}
    /**
         * Gets the access control policy for a resource. May be empty if no such policy or resource exists.
     * 
     */
    public static CompletableFuture<GetDeploymentIamPolicyResult> invokeAsync(GetDeploymentIamPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:deploymentmanager/v2:getDeploymentIamPolicy", TypeShape.of(GetDeploymentIamPolicyResult.class), args == null ? GetDeploymentIamPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}