package com.pulumi.asset;

import com.pulumi.core.internal.Constants;
import com.pulumi.core.internal.annotations.InternalUse;

/**
 * Asset represents a single blob of text or data that is managed as a first class entity.
 */
public abstract class Asset extends AssetOrArchive {
    protected Asset(String propName, Object value) {
        super(Constants.SpecialAssetSig, propName, value);
    }

    /**
     * An InvalidAsset is an internal class for uninitialized or invalid {@link Asset}
     */
    @InternalUse
    public static final class InvalidAsset extends Asset {
        public InvalidAsset() {
            super(Constants.AssetTextName, "");
        }
    }
}
