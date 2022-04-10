// *** WARNING: this file was generated by pulumi-java-gen. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package io.pulumi.awsnative.networkmanager;

import io.pulumi.awsnative.Utilities;
import io.pulumi.awsnative.networkmanager.inputs.GetDeviceArgs;
import io.pulumi.awsnative.networkmanager.outputs.GetDeviceResult;
import io.pulumi.core.TypeShape;
import io.pulumi.deployment.Deployment;
import io.pulumi.deployment.InvokeOptions;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

public class GetDevice {
    private GetDevice() {}
    /**
         * The AWS::NetworkManager::Device type describes a device.
     * 
     */
    public static CompletableFuture<GetDeviceResult> invokeAsync(GetDeviceArgs args, @Nullable InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("aws-native:networkmanager:getDevice", TypeShape.of(GetDeviceResult.class), args == null ? GetDeviceArgs.Empty : args, Utilities.withVersion(options));
    }
}