package io.pulumi.deployment;

import io.pulumi.core.TypeShape;
import io.pulumi.core.internal.Internal.InternalField;
import io.pulumi.core.internal.annotations.InternalUse;
import io.pulumi.resources.InvokeArgs;
import io.pulumi.resources.ProviderResource;
import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

/**
 * Options to help control the behavior of @see {@link Deployment#invokeAsync(String, TypeShape, InvokeArgs, InvokeOptions)}.
 */
@ParametersAreNonnullByDefault
public final class InvokeOptions {

    public static InvokeOptions Empty = new InvokeOptions();

    @Nullable
    private final Resource parent;
    @Nullable
    private final ProviderResource provider;
    @Nullable
    private final String version;

    @SuppressWarnings("unused")
    @InternalField
    private final InvokeOptionsInternal internal = new InvokeOptionsInternal();

    public InvokeOptions() {
        this(null, null, null);
    }

    public InvokeOptions(@Nullable Resource parent, @Nullable ProviderResource provider, @Nullable String version) {
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
    @ParametersAreNonnullByDefault
    public final class InvokeOptionsInternal {

        private InvokeOptionsInternal() {
            /* Empty */
        }

        public Optional<ProviderResource> getNestedProvider(String token) {
            return InvokeOptions.this.getProvider().or(
                    () -> InvokeOptions.this.getParent().flatMap(p ->
                            io.pulumi.core.internal.Internal.from(p).getProvider(token))
            );
        }
    }
}
