package com.pulumi.deployment;

import com.pulumi.resources.ProviderResource;
import com.pulumi.resources.Resource;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class InvokeOutputOptionsBuilder {
    private @Nullable Resource parent;
    private @Nullable ProviderResource provider;
    private @Nullable String version;
    private @Nullable String pluginDownloadURL;
    private @Nullable List<Resource> dependsOn;

    /**
     * An optional parent resource to which this invoke belongs.
     */
    public InvokeOutputOptionsBuilder parent(Resource parent) {
        this.parent = parent;
        return this;
    }

    /**
     * An optional provider to use for this invoke. If no provider is
     * supplied, the default provider for the invoke package will be used.
     */
    public InvokeOutputOptionsBuilder provider(ProviderResource provider) {
        this.provider = provider;
        return this;
    }

    /**
     * An optional version, corresponding to the version of the provider plugin that should be
     * used when operating on this invoke. This version overrides the version information
     * inferred from the current package and should rarely be used.
     */
    public InvokeOutputOptionsBuilder version(String version) {
        this.version = version;
        return this;
    }

    /**
     * An optional URL, corresponding to the url from which the provider plugin that should be
     * used when operating on this invoke is downloaded from. This URL overrides the download URL
     * inferred from the current package and should rarely be used.
     */
    public InvokeOutputOptionsBuilder pluginDownloadURL(String pluginDownloadURL) {
        this.pluginDownloadURL = pluginDownloadURL;
        return this;
    }

    /**
     *  Additional explicit dependencies on other resources.
     */
    public InvokeOutputOptionsBuilder dependsOn(Resource... dependsOn) {
        if (this.dependsOn == null) {
            this.dependsOn = new ArrayList<Resource>();
        }
        for (var r : dependsOn) {
            this.dependsOn.add(r);
        }
        return this;
    }

    public InvokeOutputOptions build() {
        return new InvokeOutputOptions(parent, provider, version, pluginDownloadURL, dependsOn);
    }
}