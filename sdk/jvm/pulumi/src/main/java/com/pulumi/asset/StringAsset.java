package com.pulumi.asset;

import com.pulumi.core.internal.Constants;

/**
 * StringAsset is a kind of asset produced from an in-memory UTF8-encoded string.
 */
public final class StringAsset extends Asset {
    public StringAsset(String text) {
        super(Constants.AssetTextName, text);
    }
}
