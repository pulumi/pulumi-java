// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.metastore_v1alpha;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.metastore_v1alpha.inputs.GetServiceDatabaseTableIamPolicyArgs;
import io.pulumi.googlenative.metastore_v1alpha.outputs.GetServiceDatabaseTableIamPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetServiceDatabaseTableIamPolicy {
    private GetServiceDatabaseTableIamPolicy() {}
    /**
         * Gets the access control policy for a resource. Returns an empty policy if the resource exists and does not have a policy set.
     * 
     */
    public static CompletableFuture<GetServiceDatabaseTableIamPolicyResult> invokeAsync(GetServiceDatabaseTableIamPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:metastore/v1alpha:getServiceDatabaseTableIamPolicy", TypeShape.of(GetServiceDatabaseTableIamPolicyResult.class), args == null ? GetServiceDatabaseTableIamPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}