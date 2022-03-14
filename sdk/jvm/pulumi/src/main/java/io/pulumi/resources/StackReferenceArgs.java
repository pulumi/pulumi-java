package io.pulumi.resources;

import io.pulumi.core.Output;
import io.pulumi.core.annotations.InputImport;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * The set of arguments for constructing a StackReference resource.
 */
public class StackReferenceArgs extends ResourceArgs {

    /**
     * The name of the stack to reference.
     */
    @InputImport(name = "name", required = true)
    @Nullable
    public final Output<String> name;

    public StackReferenceArgs(@Nullable Output<String> name) {
        this.name = name;
    }

    public Optional<Output<String>> getName() {
        return Optional.ofNullable(name);
    }
}
