package com.pulumi.asset;

import com.pulumi.core.internal.Constants;
import com.pulumi.core.internal.annotations.InternalUse;

import java.util.Map;

/**
 * An Archive represents a collection of named assets.
 */
public abstract class Archive extends AssetOrArchive {
    protected Archive(String propName, Object value) {
        super(Constants.SpecialArchiveSig, propName, value);
    }

    /**
     * An InvalidArchive is internal class for uninitialized or invalid {@link Archive}
     */
    @InternalUse
    public static final class InvalidArchive extends Archive {
        public InvalidArchive() {
            super(Constants.ArchiveAssetsName, Map.<String, AssetOrArchive>of());
        }
    }
}
