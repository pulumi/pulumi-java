package io.pulumi.asset;

import io.pulumi.core.internal.Constants;

/**
 * RemoteAsset is a kind of asset produced from a given URI string.  The URI's scheme dictates
 * the protocol for fetching contents: <code>file://</code> specifies a local file, <code>http://</code>
 * and <code>https://</code> specify HTTP and HTTPS, respectively.  Note that specific providers may
 * recognize alternative schemes; this is merely the base-most set that all providers support.
 */
public final class RemoteAsset extends Asset {
    public RemoteAsset(String uri) {
        super(Constants.AssetOrArchiveUriName, uri);
    }
}
