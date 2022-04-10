// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.sso;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.sso.inputs.GetPermissionSetArgs;
import io.pulumi.awsnative.sso.outputs.GetPermissionSetResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetPermissionSet {
    private GetPermissionSet() {}
    /**
         * Resource Type definition for SSO PermissionSet
     * 
     */
    public static CompletableFuture<GetPermissionSetResult> invokeAsync(GetPermissionSetArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:sso:getPermissionSet", TypeShape.of(GetPermissionSetResult.class), args == null ? GetPermissionSetArgs.Empty : args, Utilities.withVersion(options));
    }
}