// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Configuration for the project's Pulumi state storage backend.
 */
public class ProjectBackend {
    @Nullable
    private final String url;

    private ProjectBackend(Builder builder) {
        url = builder.url;
    }

    /**
     * Returns a new builder for {@link ProjectBackend}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The backend URL. Use the same URL format that is passed to "pulumi login",
     * see https://www.pulumi.com/docs/cli/commands/pulumi_login/
     * <p>
     * To explicitly use the Pulumi Cloud backend, use URL "https://api.pulumi.com"
     *
     * @return the backend URL
     */
    @Nullable
    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ProjectBackend) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    /**
     * Creates a new {@link Builder} initialized with the values from this instance.
     *
     * @return a new {@link Builder} with values copied from this instance
     */
    public Builder toBuilder() {
        return new Builder().url(url);
    }

    /**
     * Builder for {@link ProjectBackend}.
     */
    public static class Builder {
        @Nullable
        private String url;

        private Builder() {
        }

        /**
         * The backend URL. Use the same URL format that is passed to "pulumi login",
         * see https://www.pulumi.com/docs/cli/commands/pulumi_login/
         * <p>
         * To explicitly use the Pulumi Cloud backend, use URL "https://api.pulumi.com"
         *
         * @param url the backend URL
         * @return the builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Builds the {@link ProjectBackend}.
         *
         * @return the project backend
         */
        public ProjectBackend build() {
            return new ProjectBackend(this);
        }
    }
}
