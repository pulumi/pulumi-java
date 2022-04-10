// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.compute_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.compute_v1.inputs.GetTargetTcpProxyArgs;
import io.pulumi.googlenative.compute_v1.outputs.GetTargetTcpProxyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetTargetTcpProxy {
    private GetTargetTcpProxy() {}
    /**
         * Returns the specified TargetTcpProxy resource. Gets a list of available target TCP proxies by making a list() request.
     * 
     */
    public static CompletableFuture<GetTargetTcpProxyResult> invokeAsync(GetTargetTcpProxyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:compute/v1:getTargetTcpProxy", TypeShape.of(GetTargetTcpProxyResult.class), args == null ? GetTargetTcpProxyArgs.Empty : args, Utilities.withVersion(options));
    }
}