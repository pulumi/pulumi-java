package com.pulumi.deployment;

import com.pulumi.resources.ProviderResource;
import com.pulumi.resources.Resource;
import javax.annotation.Nullable;

public final class InvokeOptionsBuilder {
    private @Nullable Resource parent;
    private @Nullable ProviderResource provider;
    private @Nullable String version;
    private @Nullable String pluginDownloadURL;

    /**
     * An optional parent resource to which this invoke belongs.
     */
    public InvokeOptionsBuilder parent(Resource parent) {
        this.parent = parent;
        return this;
    }

    /**
     * An optional provider to use for this invoke. If no provider is
     * supplied, the default provider for the invoke package will be used.
     */
    public InvokeOptionsBuilder provider(ProviderResource provider) {
        this.provider = provider;
        return this;
    }

    /**
     * An optional version, corresponding to the version of the provider plugin that should be
     * used when operating on this invoke. This version overrides the version information
     * inferred from the current package and should rarely be used.
     */
    public InvokeOptionsBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * An optional URL, corresponding to the url from which the provider plugin that should be
     * used when operating on this invoke is downloaded from. This URL overrides the download URL
     * inferred from the current package and should rarely be used.
     */
    public InvokeOptionsBuilder pluginDownloadURL(String pluginDownloadURL) {
        this.pluginDownloadURL = pluginDownloadURL;
        return this;
    }


    public InvokeOptions build() {
        return new InvokeOptions(parent, provider, version, pluginDownloadURL);
    }
}