package com.pulumi.asset;

import com.pulumi.core.internal.Constants;

/**
 * FileAsset is a kind of asset produced from a given path to a file on the local filesystem.
 */
public final class FileAsset extends Asset {
    public FileAsset(String path) {
        super(Constants.AssetOrArchivePathName, path);
    }
}
