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
        this(parent, provider, version, null);
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

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The {@link InvokeOptions} builder.
     */
    public static final class Builder {
        private @Nullable Resource parent;
        private @Nullable ProviderResource provider;
        private @Nullable String version;
        private @Nullable String pluginDownloadURL;

        /**
         * An optional parent resource to which this invoke belongs.
         */
        public Builder parent(Resource parent) {
            this.parent = parent;
            return this;
        }
    
        /**
         * An optional provider to use for this invoke. If no provider is
         * supplied, the default provider for the invoke package will be used.
         */
        public Builder provider(ProviderResource provider) {
            this.provider = provider;
            return this;
        }

        /**
         * An optional version, corresponding to the version of the provider plugin that should be
         * used when operating on this invoke. This version overrides the version information
         * inferred from the current package and should rarely be used.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * An optional URL, corresponding to the url from which the provider plugin that should be
         * used when operating on this invoke is downloaded from. This URL overrides the download URL
         * inferred from the current package and should rarely be used.
         */
        public Builder pluginDownloadURL(String pluginDownloadURL) {
            this.pluginDownloadURL = pluginDownloadURL;
            return this;
        }
    
        public InvokeOptions build() {
            return new InvokeOptions(parent, provider, version, pluginDownloadURL);
        }
    }

    /**
     * Internal utility class for advanced provider resolution logic within {@link InvokeOptions}.
     *
     * @see InvokeOptions
     */
    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class InvokeOptionsInternal {

        /**
         * The underlying {@link InvokeOptions} instance from which provider and parent information is resolved.
         */
        private final InvokeOptions options;

        /**
         * Creates a new {@code InvokeOptionsInternal} wrapper for the given {@link InvokeOptions}.
         *
         * @param options the {@link InvokeOptions} instance to wrap; must not be null
         * @throws NullPointerException if {@code options} is null
         */
        private InvokeOptionsInternal(InvokeOptions options) {
            this.options = requireNonNull(options);
        }

        /**
         * Creates a new {@code InvokeOptionsInternal} from the specified {@link InvokeOptions}.
         *
         * @param options the {@link InvokeOptions} to wrap
         * @return a new {@code InvokeOptionsInternal} instance
         * @throws NullPointerException if {@code options} is null
         */
        public static InvokeOptionsInternal from(InvokeOptions options) {
            return new InvokeOptionsInternal(options);
        }

        /**
         * Attempts to resolve a {@link ProviderResource} for the given provider token.
         *
         * @param token the provider token to resolve
         * @return an {@link Optional} containing the resolved {@link ProviderResource}, or empty if not found
         *
         * @see ProviderResource
         */
        public Optional<ProviderResource> getNestedProvider(String token) {
            return this.options.getProvider().or(
                    () -> this.options.getParent()
                            .flatMap(p -> Internal.from(p).getProvider(token))
            );
        }
    }
}
