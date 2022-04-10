// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.servicemanagement_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.servicemanagement_v1.inputs.GetServiceConsumerIamPolicyArgs;
import io.pulumi.googlenative.servicemanagement_v1.outputs.GetServiceConsumerIamPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetServiceConsumerIamPolicy {
    private GetServiceConsumerIamPolicy() {}
    /**
         * Gets the access control policy for a resource. Returns an empty policy if the resource exists and does not have a policy set.
     * 
     */
    public static CompletableFuture<GetServiceConsumerIamPolicyResult> invokeAsync(GetServiceConsumerIamPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:servicemanagement/v1:getServiceConsumerIamPolicy", TypeShape.of(GetServiceConsumerIamPolicyResult.class), args == null ? GetServiceConsumerIamPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}