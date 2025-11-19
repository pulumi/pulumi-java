package com.pulumi.deployment;

import com.pulumi.core.TypeShape;
import com.pulumi.core.internal.Internal;
import com.pulumi.core.internal.annotations.InternalUse;
import com.pulumi.resources.CallArgs;
import com.pulumi.resources.ProviderResource;
import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Options to help control the behavior of  @see {@link Deployment#call(String, TypeShape, CallArgs, Resource, CallOptions)}.
 */
@ParametersAreNonnullByDefault
public final class CallOptions {

    public static CallOptions Empty = new CallOptions();

    @Nullable
    private final Resource parent;
    @Nullable
    private final ProviderResource provider;
    @Nullable
    private final String version;

    public CallOptions() {
        this(null, null, null);
    }

    public CallOptions(@Nullable Resource parent, @Nullable ProviderResource provider, @Nullable String version) {
        this.parent = parent;
        this.provider = provider;
        this.version = version;
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
     * used when performing this invokes.
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * Internal helper class for resolving the effective {@link ProviderResource} and {@link Resource}
     * options
     */
    @InternalUse
    public static final class CallOptionsInternal {

        private final CallOptions options;

        private CallOptionsInternal(CallOptions options) {
            this.options = requireNonNull(options);
        }

        public static CallOptionsInternal from(CallOptions options) {
            return new CallOptionsInternal(options);
        }

        public Optional<ProviderResource> getNestedProvider(String token) {
            return this.options.getProvider().or(
                    () -> this.options.getParent()
                            .flatMap(p -> Internal.from(p).getProvider(token))
            );
        }
    }
}
