// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.apigee_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.apigee_v1.inputs.GetKeystoreArgs;
import io.pulumi.googlenative.apigee_v1.outputs.GetKeystoreResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetKeystore {
    private GetKeystore() {}
    /**
         * Gets a keystore or truststore.
     * 
     */
    public static CompletableFuture<GetKeystoreResult> invokeAsync(GetKeystoreArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:apigee/v1:getKeystore", TypeShape.of(GetKeystoreResult.class), args == null ? GetKeystoreArgs.Empty : args, Utilities.withVersion(options));
    }
}