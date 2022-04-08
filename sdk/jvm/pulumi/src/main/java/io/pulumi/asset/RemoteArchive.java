package io.pulumi.asset;

import io.pulumi.core.internal.Constants;

/**
 * A RemoteArchive is a file-based archive fetched from a remote location.  The URI's scheme
 * dictates the protocol for fetching the archive's contents: <code>file://</code> is a local file
 * (just like a FileArchive), <code>http://</code> and <code>https://</code> specify HTTP and HTTPS,
 * respectively, and specific providers may recognize custom schemes.
 */
public final class RemoteArchive extends Archive {
    public RemoteArchive(String uri) {
        super(Constants.AssetOrArchiveUriName, uri);
    }
}
