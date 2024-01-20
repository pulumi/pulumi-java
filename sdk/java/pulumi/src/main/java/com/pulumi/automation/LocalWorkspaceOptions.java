package com.pulumi.automation;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.pulumi.Context;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public class LocalWorkspaceOptions {

    private final Path workDir;
    @Nullable
    private final Consumer<Context> program;

    public LocalWorkspaceOptions(
            Path workDir,
            @Nullable Consumer<Context> program
    ) {

        this.workDir = requireNonNull(workDir);
        this.program = program;
    }

    public Path workDir() {
        return this.workDir;
    }

    @Nullable
    public Optional<Consumer<Context>> program() {
        return Optional.ofNullable(this.program);
    }

    public static LocalWorkspaceOptions.Builder builder() {
        return new LocalWorkspaceOptions.Builder();
    }

    @ParametersAreNonnullByDefault
    @CanIgnoreReturnValue
    public static class Builder {

        private Path workDir;
        private Consumer<Context> program;

        public Builder workDir(Path path) {
            this.workDir = path;
            return this;
        }

        /**
         * The inline Pulumi program to be used for Preview/Update operations if any.
         * @see Workspace#program()
         * @param program the inline Pulumi program to use
         * @return the {@link Builder} instance
         */
        public Builder program(@Nullable Consumer<Context> program) {
            this.program = program;
            return this;
        }

        public LocalWorkspaceOptions build() {
            return new LocalWorkspaceOptions(
                    this.workDir,
                    this.program
            );
        }
    }
}
