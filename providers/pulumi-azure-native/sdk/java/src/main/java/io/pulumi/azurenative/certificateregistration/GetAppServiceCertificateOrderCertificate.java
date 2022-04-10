// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.azurenative.certificateregistration;

import io.pulumi.azurenative.Utilities;
import io.pulumi.azurenative.certificateregistration.inputs.GetAppServiceCertificateOrderCertificateArgs;
import io.pulumi.azurenative.certificateregistration.outputs.GetAppServiceCertificateOrderCertificateResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetAppServiceCertificateOrderCertificate {
    private GetAppServiceCertificateOrderCertificate() {}
    /**
         * Key Vault container ARM resource for a certificate that is purchased through Azure.
     * API Version: 2020-10-01.
     * 
     *
         * Key Vault container ARM resource for a certificate that is purchased through Azure.
     * 
     */
    public static CompletableFuture<GetAppServiceCertificateOrderCertificateResult> invokeAsync(GetAppServiceCertificateOrderCertificateArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("azure-native:certificateregistration:getAppServiceCertificateOrderCertificate", TypeShape.of(GetAppServiceCertificateOrderCertificateResult.class), args == null ? GetAppServiceCertificateOrderCertificateArgs.Empty : args, Utilities.withVersion(options));
    }
}