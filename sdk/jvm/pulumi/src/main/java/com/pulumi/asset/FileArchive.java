package com.pulumi.asset;

import com.pulumi.core.internal.Constants;

/**
 * A FileArchive is a file-based archive, or a collection of file-based assets.  This can be a
 * raw directory or a single archive file in one of the supported formats(.tar, .tar.gz, or .zip).
 */
public final class FileArchive extends Archive {
    public FileArchive(String path) {
        super(Constants.AssetOrArchivePathName, path);
    }
}
