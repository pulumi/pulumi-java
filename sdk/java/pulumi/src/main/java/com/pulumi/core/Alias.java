package com.pulumi.core;

import com.pulumi.resources.Resource;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.pulumi.core.internal.Objects.requireFalseState;
import static com.pulumi.core.internal.Objects.requireNullState;
import static java.util.Objects.requireNonNull;

/**
 * Alias is a description of prior named used for a resource. It can be processed in the
 * context of a resource creation to determine what the full aliased URN would be.
 * <p>
 * The presence of a property indicates if its value should be used.
 * If absent (i.e. "null"), then the value is not used.
 * <p>
 * Note: because of the above, there needs to be special handling to indicate that the previous
 * "parent" of a @see {@link com.pulumi.resources.Resource} was "null".
 * Specifically, pass in: {@link Alias#noParent()}
 * @see <a href="https://www.pulumi.com/docs/intro/concepts/resources/options/aliases/">www.pulumi.com/docs/intro/concepts/resources/options/aliases/</a>
 */
public class Alias {

    @Nullable
    private final String urn;
    @Nullable
    private final Output<String> name;
    @Nullable
    private final Output<String> type;
    @Nullable
    private final Output<String> stack;
    @Nullable
    private final Output<String> project;
    @Nullable
    private final Resource parent;
    @Nullable
    private final Output<String> parentUrn;
    private final boolean noParent;

    private Alias(
            @Nullable String urn,
            @Nullable Output<String> name,
            @Nullable Output<String> type,
            @Nullable Output<String> stack,
            @Nullable Output<String> project,
            @Nullable Resource parent,
            @Nullable Output<String> parentUrn,
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

    /**
     * Create an {@link Alias} with no parent.
     * No parent can indicate a root resource.
     * @return an {@link Alias} instance with no parent
     * @see #builder()
     * @see #withUrn(String)
     */
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

    /**
     * Create an {@link Alias} for a given URN.
     *
     * @param urn the URN to use.
     * @return an {@link Alias} instance with the given {@code urn}
     * @see Alias#noParent()
     * @see <a href="https://www.pulumi.com/docs/intro/concepts/resources/names/#urns">www.pulumi.com/docs/intro/concepts/resources/names/#urns</a>
     */
    /* Internal documentation:
     * Use {@link com.pulumi.core.internal.Urn} in the case where a prior URN is known and can just be specified in
     * full. Otherwise, provide some subset of the other properties in this type to generate an
     * appropriate {@code urn} from the pre-existing values of the @see {@link com.pulumi.resources.Resource}
     * with certain parts overridden.
     */
    public static Alias withUrn(String urn) {
        return new Alias(
                requireNonNull(urn),
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    /**
     * @return an {@link Alias} {@link Builder}
     * @see #withUrn(String)
     * @see #noParent()
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The {@link Alias} {@link Builder}
     */
    public static final class Builder {
        @Nullable
        private Output<String> name;
        @Nullable
        private Output<String> type;
        @Nullable
        private Output<String> stack;
        @Nullable
        private Output<String> project;
        @Nullable
        private Resource parent;
        @Nullable
        private Output<String> parentUrn;

        public Builder name(@Nullable Output<String> name) {
            this.name = name;
            return this;
        }

        public Builder name(@Nullable String name) {
            this.name = Output.ofNullable(name);
            return this;
        }

        public Builder type(@Nullable Output<String> type) {
            this.type = type;
            return this;
        }

        public Builder type(@Nullable String type) {
            this.type = Output.ofNullable(type);
            return this;
        }

        public Builder stack(@Nullable Output<String> stack) {
            this.stack = stack;
            return this;
        }

        public Builder stack(@Nullable String stack) {
            this.stack = Output.ofNullable(stack);
            return this;
        }

        public Builder project(@Nullable Output<String> project) {
            this.project = project;
            return this;
        }

        public Builder project(@Nullable String project) {
            this.project = Output.ofNullable(project);
            return this;
        }

        public Builder parent(@Nullable Resource parent) {
            requireNullState(name, () -> "Alias should not specify Alias#parent when Alias#parentUrn is  already.");
            this.parent = parent;
            return this;
        }

        public Builder parentUrn(@Nullable Output<String> parentUrn) {
            requireNullState(name, () -> "Alias should not specify Alias#parentUrn when Alias#parent is  already.");
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
    public Optional<Output<String>> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * The previous type of the resource.
     * If empty, the current type of the resource is used.
     */
    public Optional<Output<String>> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * The previous stack of the resource.
     * If empty, defaults to the value of @see {@link com.pulumi.deployment.Deployment#getStackName()}
     */
    public Optional<Output<String>> getStack() {
        return Optional.ofNullable(stack);
    }

    /**
     * The previous project of the resource.
     * If empty, defaults to the value of @see {@link com.pulumi.deployment.Deployment#getProjectName()}
     */
    public Optional<Output<String>> getProject() {
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
    public Optional<Output<String>> getParentUrn() {
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