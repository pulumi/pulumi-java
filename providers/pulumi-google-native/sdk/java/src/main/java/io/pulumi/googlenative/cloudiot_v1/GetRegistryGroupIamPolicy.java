// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.cloudiot_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.cloudiot_v1.inputs.GetRegistryGroupIamPolicyArgs;
import io.pulumi.googlenative.cloudiot_v1.outputs.GetRegistryGroupIamPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetRegistryGroupIamPolicy {
    private GetRegistryGroupIamPolicy() {}
    /**
         * Gets the access control policy for a resource. Returns an empty policy if the resource exists and does not have a policy set.
     * 
     */
    public static CompletableFuture<GetRegistryGroupIamPolicyResult> invokeAsync(GetRegistryGroupIamPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:cloudiot/v1:getRegistryGroupIamPolicy", TypeShape.of(GetRegistryGroupIamPolicyResult.class), args == null ? GetRegistryGroupIamPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}