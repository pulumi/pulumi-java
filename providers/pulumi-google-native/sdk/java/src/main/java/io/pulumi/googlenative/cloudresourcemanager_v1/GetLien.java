// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.cloudresourcemanager_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.cloudresourcemanager_v1.inputs.GetLienArgs;
import io.pulumi.googlenative.cloudresourcemanager_v1.outputs.GetLienResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetLien {
    private GetLien() {}
    /**
         * Retrieve a Lien by `name`. Callers of this method will require permission on the `parent` resource. For example, a Lien with a `parent` of `projects/1234` requires permission `resourcemanager.projects.get`
     * 
     */
    public static CompletableFuture<GetLienResult> invokeAsync(GetLienArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:cloudresourcemanager/v1:getLien", TypeShape.of(GetLienResult.class), args == null ? GetLienArgs.Empty : args, Utilities.withVersion(options));
    }
}