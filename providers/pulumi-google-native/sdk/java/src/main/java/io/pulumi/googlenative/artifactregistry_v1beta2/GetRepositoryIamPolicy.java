// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.artifactregistry_v1beta2;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.artifactregistry_v1beta2.inputs.GetRepositoryIamPolicyArgs;
import io.pulumi.googlenative.artifactregistry_v1beta2.outputs.GetRepositoryIamPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRepositoryIamPolicy {
    private GetRepositoryIamPolicy() {}
    /**
         * Gets the IAM policy for a given resource.
     * 
     */
    public static CompletableFuture<GetRepositoryIamPolicyResult> invokeAsync(GetRepositoryIamPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:artifactregistry/v1beta2:getRepositoryIamPolicy", TypeShape.of(GetRepositoryIamPolicyResult.class), args == null ? GetRepositoryIamPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}