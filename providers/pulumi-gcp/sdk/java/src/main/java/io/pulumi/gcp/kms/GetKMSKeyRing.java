// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.gcp.kms;

import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.gcp.Utilities;
import io.pulumi.gcp.kms.inputs.GetKMSKeyRingArgs;
import io.pulumi.gcp.kms.outputs.GetKMSKeyRingResult;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetKMSKeyRing {
    private GetKMSKeyRing() {}
    /**
         * Provides access to Google Cloud Platform KMS KeyRing. For more information see
     * [the official documentation](https://cloud.google.com/kms/docs/object-hierarchy#key_ring)
     * and
     * [API](https://cloud.google.com/kms/docs/reference/rest/v1/projects.locations.keyRings).
     * 
     * A KeyRing is a grouping of CryptoKeys for organizational purposes. A KeyRing belongs to a Google Cloud Platform Project
     * and resides in a specific location.
     * 
     * ## Example Usage
     * 
     *
         * A collection of arguments for invoking getKMSKeyRing.
     * 
     *
         * A collection of values returned by getKMSKeyRing.
     * 
     */
    public static CompletableFuture<GetKMSKeyRingResult> invokeAsync(GetKMSKeyRingArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("gcp:kms/getKMSKeyRing:getKMSKeyRing", TypeShape.of(GetKMSKeyRingResult.class), args == null ? GetKMSKeyRingArgs.Empty : args, Utilities.withVersion(options));
    }
}