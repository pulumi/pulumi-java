package io.pulumi.deployment;

import io.pulumi.core.internal.Internal.Field;
import io.pulumi.core.internal.Reflection;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.resources.CallArgs;
import io.pulumi.resources.ProviderResource;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

/**
 * Options to help control the behavior of  @see {@link Deployment#call(String, Reflection.TypeShape, CallArgs, Resource, CallOptions)}"/>.
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

    @Field
    public final Internal internal = new Internal();

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
     * used when performing this invoke.
     */
    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

    @InternalUse
    public final class Internal {

        private Internal() {
            /* Empty */
        }

        public Optional<ProviderResource> getNestedProvider(String token) {
            return CallOptions.this.getProvider().or(
                    () -> CallOptions.this.getParent().map(p -> Resource.internalGetProvider(p, token))
            );
        }
    }
}
