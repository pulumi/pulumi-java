package io.pulumi.asset;

import io.pulumi.core.internal.Constants;

import java.util.Map;

/**
 * An AssetArchive is an archive created from an in-memory collection of named assets or other
 * archives.
 */
public final class AssetArchive extends Archive {
    public AssetArchive(Map<String, AssetOrArchive> assets) {
        super(Constants.ArchiveAssetsName, Map.copyOf(assets));
    }
}
