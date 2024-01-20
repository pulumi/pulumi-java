package com.pulumi.automation;

/**
 * Common options controlling the behavior of update actions taken
 * against an instance of {@link WorkspaceStack}.
 */
public final class UpOptions extends GlobalOptions {

    private UpOptions() { /* empty */ }

    public static UpOptions.Builder builder() {
        return new UpOptions.Builder(new UpOptions());
    }

    /**
     * The {@link GlobalOptions} builder.
     */
    public static final class Builder extends GlobalOptions.Builder<UpOptions, UpOptions.Builder> {

        private Builder(UpOptions options) {
            super(options);
        }

        public UpOptions build() {
            return this.options;
        }
    }
}
