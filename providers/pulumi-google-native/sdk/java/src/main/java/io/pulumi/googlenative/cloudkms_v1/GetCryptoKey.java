// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.googlenative.cloudkms_v1;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.googlenative.Utilities;
import io.pulumi.googlenative.cloudkms_v1.inputs.GetCryptoKeyArgs;
import io.pulumi.googlenative.cloudkms_v1.outputs.GetCryptoKeyResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetCryptoKey {
    private GetCryptoKey() {}
    /**
         * Returns metadata for a given CryptoKey, as well as its primary CryptoKeyVersion.
     * 
     */
    public static CompletableFuture<GetCryptoKeyResult> invokeAsync(GetCryptoKeyArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("google-native:cloudkms/v1:getCryptoKey", TypeShape.of(GetCryptoKeyResult.class), args == null ? GetCryptoKeyArgs.Empty : args, Utilities.withVersion(options));
    }
}