package io.pulumi.resources;

import io.pulumi.core.Input;
import io.pulumi.core.internal.annotations.InputImport;

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
    public final Input<String> name;

    public StackReferenceArgs(@Nullable Input<String> name) {
        this.name = name;
    }

    public Optional<Input<String>> getName() {
        return Optional.ofNullable(name);
    }
}
