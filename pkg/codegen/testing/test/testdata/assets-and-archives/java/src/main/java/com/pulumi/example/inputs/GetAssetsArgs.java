// *** WARNING: this file was generated by test. ***
// *** Do not edit by hand unless you're certain you know what you are doing! ***

package com.pulumi.example.inputs;

import com.pulumi.asset.Archive;
import com.pulumi.asset.AssetOrArchive;
import com.pulumi.core.Output;
import com.pulumi.core.annotations.Import;
import com.pulumi.exceptions.MissingRequiredPropertyException;
import java.util.Objects;


public final class GetAssetsArgs extends com.pulumi.resources.InvokeArgs {

    public static final GetAssetsArgs Empty = new GetAssetsArgs();

    @Import(name="archive", required=true)
    private Output<Archive> archive;

    public Output<Archive> archive() {
        return this.archive;
    }

    @Import(name="source", required=true)
    private Output<AssetOrArchive> source;

    public Output<AssetOrArchive> source() {
        return this.source;
    }

    private GetAssetsArgs() {}

    private GetAssetsArgs(GetAssetsArgs $) {
        this.archive = $.archive;
        this.source = $.source;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static Builder builder(GetAssetsArgs defaults) {
        return new Builder(defaults);
    }

    public static final class Builder {
        private GetAssetsArgs $;

        public Builder() {
            $ = new GetAssetsArgs();
        }

        public Builder(GetAssetsArgs defaults) {
            $ = new GetAssetsArgs(Objects.requireNonNull(defaults));
        }

        public Builder archive(Output<Archive> archive) {
            $.archive = archive;
            return this;
        }

        public Builder archive(Archive archive) {
            return archive(Output.of(archive));
        }

        public Builder source(Output<AssetOrArchive> source) {
            $.source = source;
            return this;
        }

        public Builder source(AssetOrArchive source) {
            return source(Output.of(source));
        }

        public GetAssetsArgs build() {
            if ($.archive == null) {
                throw new MissingRequiredPropertyException("GetAssetsArgs", "archive");
            }
            if ($.source == null) {
                throw new MissingRequiredPropertyException("GetAssetsArgs", "source");
            }
            return $;
        }
    }

}
