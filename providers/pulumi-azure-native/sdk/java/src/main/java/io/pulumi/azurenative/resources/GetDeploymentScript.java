// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.resources;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.resources.inputs.GetDeploymentScriptArgs;
import io.pulumi.azurenative.resources.outputs.GetDeploymentScriptResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

@Deprecated /* Please use one of the variants: AzureCliScript, AzurePowerShellScript. */
public class GetDeploymentScript {
    private GetDeploymentScript() {}
    /**
         * Deployment script object.
     * API Version: 2020-10-01.
     * 
     *
         * Deployment script object.
     * 
     * @Deprecated
         * Please use one of the variants: AzureCliScript, AzurePowerShellScript.
     * 
     */
    @Deprecated /* Please use one of the variants: AzureCliScript, AzurePowerShellScript. */
    public static CompletableFuture<GetDeploymentScriptResult> invokeAsync(GetDeploymentScriptArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:resources:getDeploymentScript", TypeShape.of(GetDeploymentScriptResult.class), args == null ? GetDeploymentScriptArgs.Empty : args, Utilities.withVersion(options));
    }
}