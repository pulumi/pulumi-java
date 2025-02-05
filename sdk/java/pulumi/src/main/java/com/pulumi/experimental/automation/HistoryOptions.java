// Copyright 2025, Pulumi Corporation

package com.pulumi.experimental.automation;

import javax.annotation.Nullable;

/**
 * Options controlling the behavior of a {@link WorkspaceStack#getHistory()}
 * operation.
 */
public final class HistoryOptions {
    private final Integer page;
    private final Integer pageSize;
    private final boolean showSecrets;

    private HistoryOptions(Builder builder) {
        this.page = builder.page;
        this.pageSize = builder.pageSize;
        this.showSecrets = builder.showSecrets;
    }

    /**
     * Returns a new builder for {@link HistoryOptions}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the page.
     *
     * @return the page
     */
    @Nullable
    public Integer getPage() {
        return page;
    }

    /**
     * Returns the page size.
     *
     * @return the page size
     */
    @Nullable
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Show config secrets when they appear.
     *
     * @return whether to show secrets
     */
    public boolean getShowSecrets() {
        return showSecrets;
    }

    /**
     * Builder for {@link HistoryOptions}.
     */
    public static class Builder {
        private Integer page;
        private Integer pageSize;
        private boolean showSecrets;

        private Builder() {
        }

        /**
         * The page number to return.
         *
         * @param page the page number
         * @return the builder
         */
        public Builder page(Integer page) {
            this.page = page;
            return this;
        }

        /**
         * The number of entries per page.
         *
         * @param pageSize the page size
         * @return the builder
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Show config secrets when they appear.
         *
         * @param showSecrets whether to show secrets
         * @return the builder
         */
        public Builder showSecrets(boolean showSecrets) {
            this.showSecrets = showSecrets;
            return this;
        }

        /**
         * Builds the {@link HistoryOptions}.
         *
         * @return the history options
         */
        public HistoryOptions build() {
            return new HistoryOptions(this);
        }
    }
}
