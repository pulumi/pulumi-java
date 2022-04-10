// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.web;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.web.inputs.GetStaticSiteUserProvidedFunctionAppForStaticSiteBuildArgs;
import io.pulumi.azurenative.web.outputs.GetStaticSiteUserProvidedFunctionAppForStaticSiteBuildResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetStaticSiteUserProvidedFunctionAppForStaticSiteBuild {
    private GetStaticSiteUserProvidedFunctionAppForStaticSiteBuild() {}
    /**
         * Static Site User Provided Function App ARM resource.
     * API Version: 2020-12-01.
     * 
     *
         * Static Site User Provided Function App ARM resource.
     * 
     */
    public static CompletableFuture<GetStaticSiteUserProvidedFunctionAppForStaticSiteBuildResult> invokeAsync(GetStaticSiteUserProvidedFunctionAppForStaticSiteBuildArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:web:getStaticSiteUserProvidedFunctionAppForStaticSiteBuild", TypeShape.of(GetStaticSiteUserProvidedFunctionAppForStaticSiteBuildResult.class), args == null ? GetStaticSiteUserProvidedFunctionAppForStaticSiteBuildArgs.Empty : args, Utilities.withVersion(options));
    }
}