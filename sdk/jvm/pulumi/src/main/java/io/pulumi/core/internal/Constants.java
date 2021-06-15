package io.pulumi.core.internal;

import io.grpc.Internal;

@Internal
public class Constants {

    private Constants() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * Unknown values are encoded as a distinguished string value.
     */
    public static final String UnknownValue = "04da6b54-80e4-46f7-96ec-b56ff0331ba9";

    /**
     * SpecialSigKey is sometimes used to encode type identity inside of a map. See sdk/go/common/resource/properties.go.
     */
    public static final String SpecialSigKey = "4dabf18193072939515e22adb298388d";

    /**
     * SpecialAssetSig is a randomly assigned hash used to identify assets in maps. See sdk/go/common/resource/asset.go.
     */
    public static final String SpecialAssetSig = "c44067f5952c0a294b673a41bacd8c17";

    /**
     * SpecialArchiveSig is a randomly assigned hash used to identify archives in maps. See sdk/go/common/resource/asset.go.
     */
    public static final String SpecialArchiveSig = "0def7320c3a5731c473e5ecbe6d01bc7";

    /**
     * SpecialSecretSig is a randomly assigned hash used to identify secrets in maps. See sdk/go/common/resource/properties.go.
     */
    public static final String SpecialSecretSig = "1b47061264138c4ac30d75fd1eb44270";

    /**
     * SpecialResourceSig is a randomly assigned hash used to identify resources in maps. See sdk/go/common/resource/properties.go.
     */
    public static final String SpecialResourceSig = "5cf8f73096256a8f31e491e813e4eb8e";

    public static final String SecretValueName = "value";

    public static final String AssetTextName = "text";
    public static final String ArchiveAssetsName = "assets";

    public static final String AssetOrArchivePathName = "path";
    public static final String AssetOrArchiveUriName = "uri";

    public static final String ResourceUrnName = "urn";
    public static final String ResourceIdName = "id";
    public static final String ResourceVersionName = "packageVersion";

    public static final String IdPropertyName = "id";
    public static final String UrnPropertyName = "urn";
    public static final String StatePropertyName = "state";
}