// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.storage_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.storage_v1.inputs.GetObjectIamPolicyArgs;
import io.pulumi.googlenative.storage_v1.outputs.GetObjectIamPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetObjectIamPolicy {
    private GetObjectIamPolicy() {}
    /**
         * Returns an IAM policy for the specified object.
     * 
     */
    public static CompletableFuture<GetObjectIamPolicyResult> invokeAsync(GetObjectIamPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:storage/v1:getObjectIamPolicy", TypeShape.of(GetObjectIamPolicyResult.class), args == null ? GetObjectIamPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}