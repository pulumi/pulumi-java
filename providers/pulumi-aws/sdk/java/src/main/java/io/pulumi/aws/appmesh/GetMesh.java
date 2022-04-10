// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.aws.appmesh;

import io.pulumi.aws.Utilities;
import io.pulumi.aws.appmesh.inputs.GetMeshArgs;
import io.pulumi.aws.appmesh.outputs.GetMeshResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetMesh {
    private GetMesh() {}
    /**
         * The App Mesh Mesh data source allows details of an App Mesh Mesh to be retrieved by its name and optionally the mesh_owner.
     * 
     * ## Example Usage
     * 
     *
         * A collection of arguments for invoking getMesh.
     * 
     *
         * A collection of values returned by getMesh.
     * 
     */
    public static CompletableFuture<GetMeshResult> invokeAsync(GetMeshArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws:appmesh/getMesh:getMesh", TypeShape.of(GetMeshResult.class), args == null ? GetMeshArgs.Empty : args, Utilities.withVersion(options));
    }
}