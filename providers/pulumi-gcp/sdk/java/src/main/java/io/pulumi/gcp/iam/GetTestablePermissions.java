// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.gcp.iam;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.gcp.Utilities;
import io.pulumi.gcp.iam.inputs.GetTestablePermissionsArgs;
import io.pulumi.gcp.iam.outputs.GetTestablePermissionsResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetTestablePermissions {
    private GetTestablePermissions() {}
    /**
         * Retrieve a list of testable permissions for a resource. Testable permissions mean the permissions that user can add or remove in a role at a given resource. The resource can be referenced either via the full resource name or via a URI.
     * 
     * ## Example Usage
     * 
     *
         * A collection of arguments for invoking getTestablePermissions.
     * 
     *
         * A collection of values returned by getTestablePermissions.
     * 
     */
    public static CompletableFuture<GetTestablePermissionsResult> invokeAsync(GetTestablePermissionsArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("gcp:iam/getTestablePermissions:getTestablePermissions", TypeShape.of(GetTestablePermissionsResult.class), args == null ? GetTestablePermissionsArgs.Empty : args, Utilities.withVersion(options));
    }
}