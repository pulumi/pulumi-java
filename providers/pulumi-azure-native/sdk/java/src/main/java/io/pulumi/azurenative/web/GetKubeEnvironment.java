// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.web;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.web.inputs.GetKubeEnvironmentArgs;
import io.pulumi.azurenative.web.outputs.GetKubeEnvironmentResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetKubeEnvironment {
    private GetKubeEnvironment() {}
    /**
         * A Kubernetes cluster specialized for web workloads by Azure App Service
     * API Version: 2021-01-01.
     * 
     *
         * A Kubernetes cluster specialized for web workloads by Azure App Service
     * 
     */
    public static CompletableFuture<GetKubeEnvironmentResult> invokeAsync(GetKubeEnvironmentArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:web:getKubeEnvironment", TypeShape.of(GetKubeEnvironmentResult.class), args == null ? GetKubeEnvironmentArgs.Empty : args, Utilities.withVersion(options));
    }
}