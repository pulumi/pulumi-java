package com.pulumi.automation;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public class LocalWorkspaceOptions {

    private final Path workDir;

    public LocalWorkspaceOptions(Path workDir) {
        this.workDir = requireNonNull(workDir);
    }

    public Path workDir() {
        return workDir;
    }

    public static LocalWorkspaceOptions.Builder builder() {
        return new LocalWorkspaceOptions.Builder();
    }

    public static class Builder {

        private Path workDir;

        public Builder workDir(Path path) {
            this.workDir = path;
            return this;
        }

        public LocalWorkspaceOptions build() {
            return new LocalWorkspaceOptions(
                    this.workDir
            );
        }
    }
}
