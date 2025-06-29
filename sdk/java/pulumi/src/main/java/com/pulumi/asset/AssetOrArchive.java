package com.pulumi.asset;

import com.pulumi.core.internal.annotations.InternalUse;

import static java.util.Objects.requireNonNull;

/**
 * Base class of @see {@link Asset}s and @see {@link Archive}s.
 */
public abstract class AssetOrArchive {

    protected final String sigKey;
    protected final String propName;
    protected final Object value;

    protected AssetOrArchive(String sigKey, String propName, Object value) {
        this.sigKey = requireNonNull(sigKey);
        this.propName = requireNonNull(propName);
        this.value = requireNonNull(value);
    }

    /**
     * An AssetOrArchiveInternal is internal helper class of {@link AssetOrArchive}.
     */
    public static final class AssetOrArchiveInternal {

        private final AssetOrArchive assetOrArchive;

        private AssetOrArchiveInternal(AssetOrArchive assetOrArchive) {
            this.assetOrArchive = requireNonNull(assetOrArchive);
        }

        public static AssetOrArchiveInternal from(AssetOrArchive assetOrArchive) {
            return new AssetOrArchiveInternal(assetOrArchive);
        }

        @InternalUse
        public String getSigKey() {
            return this.assetOrArchive.sigKey;
        }

        @InternalUse
        public Object getValue() {
            return this.assetOrArchive.value;
        }

        @InternalUse
        public String getPropName() {
            return this.assetOrArchive.propName;
        }
    }
}
