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
import java.util.List;
import java.util.ArrayList;

import static java.util.Objects.requireNonNull;

/**
 * Options to help control the behavior of {@link Deployment#invoke(String, TypeShape, InvokeArgs, InvokeOutputOptions)}.
 */
@ParametersAreNonnullByDefault
public class InvokeOutputOptions extends InvokeOptions {

    public static InvokeOutputOptions Empty = new InvokeOutputOptions();

    private final List<Resource> dependsOn;

    public InvokeOutputOptions(
        @Nullable Resource parent, 
        @Nullable ProviderResource provider, 
        @Nullable String version,
        @Nullable String pluginDownloadURL,
        @Nullable List<Resource> dependsOn) {
        super(parent, provider, version, pluginDownloadURL);
        this.dependsOn = dependsOn;
    }

    public InvokeOutputOptions(
        @Nullable Resource parent, 
        @Nullable ProviderResource provider, 
        @Nullable String version,
        @Nullable List<Resource> dependsOn) {
        this(parent, provider, version, null, dependsOn);
    }

    public InvokeOutputOptions() {
        this(null, null, null, null, null);
    }

    /**
     * Optional resources that this invoke depends on. The invoke will wait for these resources
     * to be resolved before executing.
     */
    public List<Resource> getDependsOn() {
        return this.dependsOn == null ? List.of() : new ArrayList<>(this.dependsOn);
    }

    /**
     * The {@link InvokeOutputOptions} builder.
     */
    public static final class Builder {
        private @Nullable Resource parent;
        private @Nullable ProviderResource provider;
        private @Nullable String version;
        private @Nullable String pluginDownloadURL;
        private @Nullable List<Resource> dependsOn;
    
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
    
        /**
         *  Additional explicit dependencies on other resources.
         */
        public Builder dependsOn(Resource... dependsOn) {
            if (this.dependsOn == null) {
                this.dependsOn = new ArrayList<Resource>();
            }
            for (var r : dependsOn) {
                this.dependsOn.add(r);
            }
            return this;
        }
    
        public InvokeOutputOptions build() {
            return new InvokeOutputOptions(parent, provider, version, pluginDownloadURL, dependsOn);
        }
    }

    /**
     * Internal utility class of {@link InvokeOutputOptions}.
     *
     * @see InvokeOutputOptions
     * @see ProviderResource
     * @see com.pulumi.core.internal.Internal
     */
    @InternalUse
    @ParametersAreNonnullByDefault
    public static final class InvokeOutputOptionsInternal {

        /**
         * The underlying {@link InvokeOutputOptions} instance from which provider and parent information is resolved.
         */
        private final InvokeOutputOptions options;

        /**
         * Creates a new {@code InvokeOutputOptionsInternal} wrapper for the given {@link InvokeOutputOptions}.
         *
         * @param options the {@link InvokeOutputOptions} instance to wrap; must not be null
         * @throws NullPointerException if {@code options} is null
         */
        private InvokeOutputOptionsInternal(InvokeOutputOptions options) {
            this.options = requireNonNull(options);
        }

        /**
         * Creates a new {@code InvokeOutputOptionsInternal} from the specified {@link InvokeOutputOptions}.
         *
         * @param options the {@link InvokeOutputOptions} to wrap
         * @return a new {@code InvokeOutputOptionsInternal} instance
         * @throws NullPointerException if {@code options} is null
         */
        public static InvokeOutputOptionsInternal from(InvokeOutputOptions options) {
            return new InvokeOutputOptionsInternal(options);
        }

        /**
         * Attempts to resolve a {@link ProviderResource} for the given provider token.
         *
         * @param token the provider token to resolve
         * @return an {@link Optional} containing the resolved {@link ProviderResource}, or empty if not found
         *
         * @see ProviderResource
         * @see com.pulumi.core.internal.Internal#from(Resource)
         */
        public Optional<ProviderResource> getNestedProvider(String token) {
            return this.options.getProvider().or(
                    () -> this.options.getParent()
                            .flatMap(p -> Internal.from(p).getProvider(token)));
        }
    }
}
