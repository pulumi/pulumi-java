package io.pulumi.core;

import io.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.pulumi.core.internal.Objects.requireFalseState;
import static io.pulumi.core.internal.Objects.requireNullState;

/**
 * Alias is a description of prior named used for a resource. It can be processed in the
 * context of a resource creation to determine what the full aliased URN would be.
 * <p>
 * Use @see {@link Urn} in the case where a prior URN is known and can just be specified in
 * full. Otherwise, provide some subset of the other properties in this type to generate an
 * appropriate {@code urn} from the pre-existing values of the @see {@link io.pulumi.resources.Resource}
 * with certain parts overridden.
 * <p>
 * The presence of a property indicates if its value should be used. If absent (i.e. "null"), then the value is not used.
 * <p>
 * Note: because of the above, there needs to be special handling to indicate that the previous
 * "parent" of a @see {@link io.pulumi.resources.Resource} was "null".
 * Specifically, pass in: Alias.noParent()
 */
public class Alias {

    @Nullable
    private final String urn;
    @Nullable
    private final io.pulumi.core.Input<String> name;
    @Nullable
    private final io.pulumi.core.Input<String> type;
    @Nullable
    private final io.pulumi.core.Input<String> stack;
    @Nullable
    private final io.pulumi.core.Input<String> project;
    @Nullable
    private final Resource parent;
    @Nullable
    private final io.pulumi.core.Input<String> parentUrn;
    private final boolean noParent;

    @SuppressWarnings("unused")
    private Alias() {
        throw new UnsupportedOperationException("static class");
    }

    private Alias(
            @Nullable String urn,
            @Nullable Input<String> name,
            @Nullable Input<String> type,
            @Nullable Input<String> stack,
            @Nullable Input<String> project,
            @Nullable Resource parent,
            @Nullable Input<String> parentUrn,
            boolean noParent
    ) {
        this.urn = urn;
        this.name = name;
        this.type = type;
        this.stack = stack;
        this.project = project;
        this.parent = parent;
        this.parentUrn = parentUrn;
        this.noParent = noParent;
    }

    public static Alias noParent() {
        return new Alias(
                null, // TODO what about all those values???
                null,
                null,
                null,
                null,
                null,
                null,
                true
        );
    }

    public static Alias withUrn(String urn) {
        return new Alias(
                Objects.requireNonNull(urn),
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        @Nullable
        private io.pulumi.core.Input<String> name;
        @Nullable
        private io.pulumi.core.Input<String> type;
        @Nullable
        private io.pulumi.core.Input<String> stack;
        @Nullable
        private io.pulumi.core.Input<String> project;
        @Nullable
        private Resource parent;
        @Nullable
        private io.pulumi.core.Input<String> parentUrn;

        public Builder setName(@Nullable Input<String> name) {
            this.name = name;
            return this;
        }

        public Builder setName(@Nullable String name) {
            this.name = Input.ofNullable(name);
            return this;
        }

        public Builder setType(@Nullable Input<String> type) {
            this.type = type;
            return this;
        }

        public Builder setType(@Nullable String type) {
            this.type = Input.ofNullable(type);
            return this;
        }

        public Builder setStack(@Nullable Input<String> stack) {
            this.stack = stack;
            return this;
        }

        public Builder setStack(@Nullable String stack) {
            this.stack = Input.ofNullable(stack);
            return this;
        }

        public Builder setProject(@Nullable Input<String> project) {
            this.project = project;
            return this;
        }

        public Builder setProject(@Nullable String project) {
            this.project = Input.ofNullable(project);
            return this;
        }

        public Builder setParent(@Nullable Resource parent) {
            requireNullState(name, () -> "Alias should not specify Alias#parent when Alias#parentUrn is set already.");
            this.parent = parent;
            return this;
        }

        public Builder setParentUrn(@Nullable Input<String> parentUrn) {
            requireNullState(name, () -> "Alias should not specify Alias#parentUrn when Alias#parent is set already.");
            this.parentUrn = parentUrn;
            return this;
        }

        public Alias build() {
            return new Alias(null, name, type, stack, project, parent, parentUrn, false);
        }
    }

    /**
     * The previous urn to alias to.
     * If this is provided, no other properties in this type should be provided.
     */
    public Optional<String> getUrn() {
        // TODO: we can probably move this check to regression tests with proper constructor/builder
        if (this.urn != null) {
            Function<String, Supplier<String>> conflict = (String field) ->
                    () -> String.format("Alias should not specify both Alias#urn and Alias#%s", field);
            requireNullState(name, conflict.apply("name"));
            requireNullState(type, conflict.apply("type"));
            requireNullState(project, conflict.apply("project"));
            requireNullState(stack, conflict.apply("stack"));
            requireNullState(parent, conflict.apply("parent"));
            requireNullState(parentUrn, conflict.apply("parentUrn"));
            requireFalseState(noParent, conflict.apply("noParent"));
        }
        return Optional.ofNullable(this.urn);
    }

    /**
     * The previous name of the resource.
     * If empty, the current name of the resource is used.
     */
    public Optional<Input<String>> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * The previous type of the resource.
     * If empty, the current type of the resource is used.
     */
    public Optional<Input<String>> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * The previous stack of the resource.
     * If empty, defaults to the value of @see {@link io.pulumi.deployment.Deployment#getStackName()}
     */
    public Optional<Input<String>> getStack() {
        return Optional.ofNullable(stack);
    }

    /**
     * The previous project of the resource.
     * If empty, defaults to the value of @see {@link io.pulumi.deployment.Deployment#getProjectName()}
     */
    public Optional<Input<String>> getProject() {
        return Optional.ofNullable(project);
    }

    /**
     * The previous parent of the resource. If empty, the current parent of the resource is used.
     * <p>
     * To specify no original parent, use "noParent".
     * <p>
     * Only specify one of "parent" or "parentUrn" or "noParent".
     */
    public Optional<Resource> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * The previous parent of the resource. if empty, the current parent of
     * the resource is used.
     * <p>
     * To specify no original parent, use "noParent".
     * <p>
     * Only specify one of "parent" or "parentUrn" or "noParent".
     */
    public Optional<Input<String>> getParentUrn() {
        return Optional.ofNullable(parentUrn);
    }

    /**
     * Used to indicate the resource previously had no parent.
     * If "false" this property is ignored.
     * <p>
     * Only specify one of "parent" or "parentUrn" or "noParent".
     */
    public boolean hasNoParent() {
        return noParent;
    }
}