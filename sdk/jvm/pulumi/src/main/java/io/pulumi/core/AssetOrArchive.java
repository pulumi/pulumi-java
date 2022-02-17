package io.pulumi.core;

import io.pulumi.core.internal.annotations.InternalUse;

import java.util.Objects;

/**
 * Base class of @see {@link Asset}s and @see {@link Archive}s.
 */
public abstract class AssetOrArchive {
    protected final String sigKey;
    protected final String propName;
    protected final Object value;

    protected AssetOrArchive(String sigKey, String propName, Object value) {
        this.sigKey = Objects.requireNonNull(sigKey);
        this.propName = Objects.requireNonNull(propName);
        this.value = Objects.requireNonNull(value);
    }

    @InternalUse
    public String getSigKey() {
        return sigKey;
    }

    @InternalUse
    public Object getValue() {
        return value;
    }

    @InternalUse
    public String getPropName() {
        return propName;
    }
}
