package io.pulumi.core;

import io.pulumi.core.internal.Constants;
import io.pulumi.core.internal.annotations.InternalUse;

import java.util.Map;

/**
 * An Archive represents a collection of named assets.
 */
public abstract class Archive extends AssetOrArchive {
    protected Archive(String propName, Object value) {
        super(Constants.SpecialArchiveSig, propName, value);
    }


    /**
     * An AssetArchive is an archive created from an in-memory collection of named assets or other
     * archives.
     */
    public static final class AssetArchive extends Archive {
        public AssetArchive(Map<String, AssetOrArchive> assets) {
            super(Constants.ArchiveAssetsName, Map.copyOf(assets));
        }
    }

    /**
     * A FileArchive is a file-based archive, or a collection of file-based assets.  This can be a
     * raw directory or a single archive file in one of the supported formats(.tar, .tar.gz, or .zip).
     */
    public static final class FileArchive extends Archive {
        public FileArchive(String path) {
            super(Constants.AssetOrArchivePathName, path);
        }
    }

    /**
     * A RemoteArchive is a file-based archive fetched from a remote location.  The URI's scheme
     * dictates the protocol for fetching the archive's contents: <code>file://</code> is a local file
     * (just like a FileArchive), <code>http://</code> and <code>https://</code> specify HTTP and HTTPS,
     * respectively, and specific providers may recognize custom schemes.
     */
    public static final class RemoteArchive extends Archive {
        public RemoteArchive(String uri) {
            super(Constants.AssetOrArchiveUriName, uri);
        }
    }

    @InternalUse
    public static final class InvalidArchive extends Archive {
        public InvalidArchive() {
            super(Constants.ArchiveAssetsName, Map.<String, AssetOrArchive>of());
        }
    }
}
