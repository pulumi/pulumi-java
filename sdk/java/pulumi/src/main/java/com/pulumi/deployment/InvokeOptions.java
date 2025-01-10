package com.pulumi.deployment;

import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.InvokeArgs;
import com.pulumi.resources.ProviderResource;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Options to help control the behavior of @see {@link Deployment#invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}.
 */
@ParametersAreNonnullByDefault
public class InvokeOptions {

    public static InvokeOptions Empty = new InvokeOptions();

    @Nullable
    private final Resource parent;
    @Nullable
    private final ProviderResource provider;
    @Nullable
    private final String version;
    @Nullable
    private final String pluginDownloadURL;

    public InvokeOptions() {
        this(null, null, null, null);
    }

    public InvokeOptions(
        @Nullable Resource parent, 
        @Nullable ProviderResource provider, 
        @Nullable String version) {
        this.parent = parent;
        this.provider = provider;
        this.version = version;
        this.pluginDownloadURL = null;
    }

    public InvokeOptions(
        @Nullable Resource parent, 
        @Nullable ProviderResource provider, 
        @Nullable String version,
        @Nullable String pluginDownloadURL) {
        this.parent = parent;
        this.provider = provider;
        this.version = version;
        this.pluginDownloadURL = pluginDownloadURL;
    }

    /**
     * An optional parent to use for default options for this invoke (e.g. the default provider
     * to use).
     */
    public Optional<Resource> getParent() {
        return Optional.ofNullable(parent);
    }

    /*
     * An optional provider to use for this invocation. If no provider is supplied, the default
     * provider for the invoked function's package will be used.
     */
    public Optional<ProviderResource> getProvider() {
        return Optional.ofNullable(this.provider);
    }

    /**
     * An optional version, corresponding to the version of the provider plugin that should be
     * used when performing this invoke.
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * An optional URL, corresponding to the url from which the provider plugin that should be
     * used when operating on this resource is downloaded from. This URL overrides the download URL
     * inferred from the current package and should rarely be used.
     */
    public Optional<String> getPluginDownloadURL() {
        return Optional.ofNullable(pluginDownloadURL);
    }

    public static InvokeOptionsBuilder builder() {
        return new InvokeOptionsBuilder();
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class InvokeOptionsInternal {

        private final InvokeOptions options;

        private InvokeOptionsInternal(InvokeOptions options) {
            this.options = requireNonNull(options);
        }

        public static InvokeOptionsInternal from(InvokeOptions options) {
            return new InvokeOptionsInternal(options);
        }

        public Optional<ProviderResource> getNestedProvider(String token) {
            return this.options.getProvider().or(
                    () -> this.options.getParent()
                            .flatMap(p -> Internal.from(p).getProvider(token))
            );
        }
    }
}
