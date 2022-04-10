// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.managedidentities_v1alpha1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.managedidentities_v1alpha1.inputs.GetDomainBackupIamPolicyArgs;
import io.pulumi.googlenative.managedidentities_v1alpha1.outputs.GetDomainBackupIamPolicyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDomainBackupIamPolicy {
    private GetDomainBackupIamPolicy() {}
    /**
         * Gets the access control policy for a resource. Returns an empty policy if the resource exists and does not have a policy set.
     * 
     */
    public static CompletableFuture<GetDomainBackupIamPolicyResult> invokeAsync(GetDomainBackupIamPolicyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:managedidentities/v1alpha1:getDomainBackupIamPolicy", TypeShape.of(GetDomainBackupIamPolicyResult.class), args == null ? GetDomainBackupIamPolicyArgs.Empty : args, Utilities.withVersion(options));
    }
}