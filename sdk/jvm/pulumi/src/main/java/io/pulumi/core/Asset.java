package io.pulumi.core;

import io.pulumi.core.internal.Constants;
import io.pulumi.core.internal.annotations.InternalUse;

/**
 * Asset represents a single blob of text or data that is managed as a first class entity.
 */
public abstract class Asset extends AssetOrArchive {
    protected Asset(String propName, Object value) {
        super(Constants.SpecialAssetSig, propName, value);
    }

    /**
     * FileAsset is a kind of asset produced from a given path to a file on the local filesystem.
     */
    public static final class FileAsset extends Asset {
        public FileAsset(String path) {
            super(Constants.AssetOrArchivePathName, path);
        }
    }

    /**
     * StringAsset is a kind of asset produced from an in-memory UTF8-encoded string.
     */
    public static final class StringAsset extends Asset {
        public StringAsset(String text) {
            super(Constants.AssetTextName, text);
        }
    }

    /**
     * RemoteAsset is a kind of asset produced from a given URI string.  The URI's scheme dictates
     * the protocol for fetching contents: <code>file://</code> specifies a local file, <code>http://</code>
     * and <code>https://</code> specify HTTP and HTTPS, respectively.  Note that specific providers may
     * recognize alternative schemes; this is merely the base-most set that all providers support.
     */
    public static final class RemoteAsset extends Asset {
        public RemoteAsset(String uri) {
            super(Constants.AssetOrArchiveUriName, uri);
        }
    }

    @InternalUse
    public static final class InvalidAsset extends Asset {
        public InvalidAsset() {
            super(Constants.AssetTextName, "");
        }
    }
}
