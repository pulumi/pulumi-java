// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.akamai.properties;

import com.pulumi.akamai.Utilities;
import com.pulumi.akamai.properties.inputs.GetActivationArgs;
import com.pulumi.akamai.properties.inputs.GetActivationPlainArgs;
import com.pulumi.akamai.properties.inputs.GetCpCodeArgs;
import com.pulumi.akamai.properties.inputs.GetCpCodePlainArgs;
import com.pulumi.akamai.properties.inputs.GetPropertyArgs;
import com.pulumi.akamai.properties.inputs.GetPropertyPlainArgs;
import com.pulumi.akamai.properties.inputs.GetPropertyRulesArgs;
import com.pulumi.akamai.properties.inputs.GetPropertyRulesPlainArgs;
import com.pulumi.akamai.properties.outputs.GetActivationResult;
import com.pulumi.akamai.properties.outputs.GetCpCodeResult;
import com.pulumi.akamai.properties.outputs.GetPropertyResult;
import com.pulumi.akamai.properties.outputs.GetPropertyRulesResult;
import com.pulumi.core.Output;
import com.pulumi.core.TypeShape;
import com.pulumi.deployment.Deployment;
import com.pulumi.deployment.InvokeOptions;
import com.pulumi.deployment.InvokeOutputOptions;
import java.util.concurrent.CompletableFuture;

public final class PropertiesFunctions {
    /**
     * 
     * @deprecated
     * akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation
     * 
     */
    @Deprecated /* akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation */
    public static Output<GetActivationResult> getActivation(GetActivationArgs args) {
        return getActivation(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation
     * 
     */
    @Deprecated /* akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation */
    public static CompletableFuture<GetActivationResult> getActivationPlain(GetActivationPlainArgs args) {
        return getActivationPlain(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation
     * 
     */
    @Deprecated /* akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation */
    public static Output<GetActivationResult> getActivation(GetActivationArgs args, InvokeOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getActivation:getActivation", TypeShape.of(GetActivationResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation
     * 
     */
    @Deprecated /* akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation */
    public static Output<GetActivationResult> getActivation(GetActivationArgs args, InvokeOutputOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getActivation:getActivation", TypeShape.of(GetActivationResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation
     * 
     */
    @Deprecated /* akamai.properties/getactivation.getActivation has been deprecated in favor of akamai.index/getpropertyactivation.getPropertyActivation */
    public static CompletableFuture<GetActivationResult> getActivationPlain(GetActivationPlainArgs args, InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("akamai:properties/getActivation:getActivation", TypeShape.of(GetActivationResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode
     * 
     */
    @Deprecated /* akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode */
    public static Output<GetCpCodeResult> getCpCode(GetCpCodeArgs args) {
        return getCpCode(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode
     * 
     */
    @Deprecated /* akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode */
    public static CompletableFuture<GetCpCodeResult> getCpCodePlain(GetCpCodePlainArgs args) {
        return getCpCodePlain(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode
     * 
     */
    @Deprecated /* akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode */
    public static Output<GetCpCodeResult> getCpCode(GetCpCodeArgs args, InvokeOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getCpCode:getCpCode", TypeShape.of(GetCpCodeResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode
     * 
     */
    @Deprecated /* akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode */
    public static Output<GetCpCodeResult> getCpCode(GetCpCodeArgs args, InvokeOutputOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getCpCode:getCpCode", TypeShape.of(GetCpCodeResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode
     * 
     */
    @Deprecated /* akamai.properties/getcpcode.getCpCode has been deprecated in favor of akamai.index/getcpcode.getCpCode */
    public static CompletableFuture<GetCpCodeResult> getCpCodePlain(GetCpCodePlainArgs args, InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("akamai:properties/getCpCode:getCpCode", TypeShape.of(GetCpCodeResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty
     * 
     */
    @Deprecated /* akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty */
    public static Output<GetPropertyResult> getProperty(GetPropertyArgs args) {
        return getProperty(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty
     * 
     */
    @Deprecated /* akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty */
    public static CompletableFuture<GetPropertyResult> getPropertyPlain(GetPropertyPlainArgs args) {
        return getPropertyPlain(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty
     * 
     */
    @Deprecated /* akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty */
    public static Output<GetPropertyResult> getProperty(GetPropertyArgs args, InvokeOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getProperty:getProperty", TypeShape.of(GetPropertyResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty
     * 
     */
    @Deprecated /* akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty */
    public static Output<GetPropertyResult> getProperty(GetPropertyArgs args, InvokeOutputOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getProperty:getProperty", TypeShape.of(GetPropertyResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty
     * 
     */
    @Deprecated /* akamai.properties/getproperty.getProperty has been deprecated in favor of akamai.index/getproperty.getProperty */
    public static CompletableFuture<GetPropertyResult> getPropertyPlain(GetPropertyPlainArgs args, InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("akamai:properties/getProperty:getProperty", TypeShape.of(GetPropertyResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules
     * 
     */
    @Deprecated /* akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules */
    public static Output<GetPropertyRulesResult> getPropertyRules(GetPropertyRulesArgs args) {
        return getPropertyRules(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules
     * 
     */
    @Deprecated /* akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules */
    public static CompletableFuture<GetPropertyRulesResult> getPropertyRulesPlain(GetPropertyRulesPlainArgs args) {
        return getPropertyRulesPlain(args, InvokeOptions.Empty);
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules
     * 
     */
    @Deprecated /* akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules */
    public static Output<GetPropertyRulesResult> getPropertyRules(GetPropertyRulesArgs args, InvokeOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getPropertyRules:getPropertyRules", TypeShape.of(GetPropertyRulesResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules
     * 
     */
    @Deprecated /* akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules */
    public static Output<GetPropertyRulesResult> getPropertyRules(GetPropertyRulesArgs args, InvokeOutputOptions options) {
        return Deployment.getInstance().invoke("akamai:properties/getPropertyRules:getPropertyRules", TypeShape.of(GetPropertyRulesResult.class), args, Utilities.withVersion(options));
    }
    /**
     * 
     * @deprecated
     * akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules
     * 
     */
    @Deprecated /* akamai.properties/getpropertyrules.getPropertyRules has been deprecated in favor of akamai.index/getpropertyrules.getPropertyRules */
    public static CompletableFuture<GetPropertyRulesResult> getPropertyRulesPlain(GetPropertyRulesPlainArgs args, InvokeOptions options) {
        return Deployment.getInstance().invokeAsync("akamai:properties/getPropertyRules:getPropertyRules", TypeShape.of(GetPropertyRulesResult.class), args, Utilities.withVersion(options));
    }
}
