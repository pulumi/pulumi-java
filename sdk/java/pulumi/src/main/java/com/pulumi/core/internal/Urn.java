package com.pulumi.core.internal;

import com.pulumi.core.Output;
import com.pulumi.core.internal.annotations.InternalUse;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import static com.pulumi.core.internal.Objects.require;
import static com.pulumi.core.internal.Strings.isNonEmptyOrNull;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * An automatically generated logical URN, used to stably identify resources. These are created
 * automatically by Pulumi to identify resources. They cannot be manually constructed.
 * <p>
 * Read more in the documentation: <a href="https://www.pulumi.com/docs/intro/concepts/resources/names/#urns">www.pulumi.com/docs/intro/concepts/resources/names/#urns</a>
 * <p>
 * Each resource registered with the Pulumi engine is logically identified
 * by its uniform resource name (URN).
 * A resource's URN is derived from its type, parent type, and user-supplied name.
 * </p>
 * <br>
 * The structure of a URN is defined by the grammar below:
 * <pre>
 * urn = "urn:pulumi:" stack "::" project "::" qualified type name "::" name ;
 *
 * stack   = string ;
 * project = string ;
 * name    = string ;
 * string  = (* any sequence of unicode code points that does not contain "::" *) ;
 *
 * qualified type name = [ parent type "$" ] type ;
 * parent type         = type ;
 *
 * type       = package ":" [ module ":" ] type name ;
 * package    = identifier ;
 * module     = identifier ;
 * type name  = identifier ;
 * identifier = unicode letter { unicode letter | unicode digit | "_" } ;
 * </pre>
 * Source: <a href="https://pulumi-developer-docs.readthedocs.io/en/latest/providers/implementers-guide.html?highlight=URN#urns">https://pulumi-developer-docs.readthedocs.io/en/latest/providers/implementers-guide.html?highlight=URN#urns</a>
 * <p>Note that type identifiers without modules are sometimes emitted by codegen using this grammar instead, and should be recognizable by the SDK:</p>
 * <pre>
 * package "::" type name
 * </pre>
 */
@InternalUse
@ParametersAreNonnullByDefault
public final class Urn {

    private static final String Prefix = "urn:pulumi:";
    private static final String PartsSeparator = "::";
    private static final String PartsSeparatorRegex = "[:][:]";
    private static final String TypeSeparator = ":";
    private static final String TypeSeparatorRegex = "[:]";
    private static final String ParentSeparator = "$";
    private static final String ParentSeparatorRegex = "\\$";

    public final String stack;
    public final String project;
    public final QualifiedTypeName qualifiedType;
    public final String name;

    private Urn(String stack, String project, QualifiedTypeName qualifiedType, String name) {
        this.stack = requireNonNull(stack);
        this.project = requireNonNull(project);
        this.qualifiedType = requireNonNull(qualifiedType);
        this.name = requireNonNull(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Urn urn = (Urn) o;
        return stack.equals(urn.stack) && project.equals(urn.project) && qualifiedType.equals(urn.qualifiedType) && name.equals(urn.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, project, qualifiedType, name);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Urn.class.getSimpleName() + "[", "]")
                .add("stack='" + stack + "'")
                .add("project='" + project + "'")
                .add("qualifiedType=" + qualifiedType)
                .add("name='" + name + "'")
                .toString();
    }

    @InternalUse
    @SuppressWarnings("ConstantConditions") // IntelliJ can't understand the custom validators
    public static Urn parse(String urn) {
        require(u -> isNonEmptyOrNull(u), urn,
                () -> format("expected urn to be not empty and not null, got: '%s'", urn)
        );
        require(u -> u.startsWith(Prefix), urn,
                () -> format("expected urn to start with '%s', got: '%s'", Prefix, urn)
        );

        var urnParts = urn.split(PartsSeparatorRegex, -1); // -1 avoids dropping trailing empty strings
        require(up -> up.length == 4, urnParts,
                () -> format("expected urn to have 4 parts, separated by '%s', got '%s' in '%s'",
                        PartsSeparator, urnParts.length, urn
                )
        );
        var stack = urnParts[0].replaceFirst(Prefix, "");
        require(s -> isNonEmptyOrNull(s), stack,
                () -> format("expected urn stack part to be not empty, got: '%s'", stack)
        );
        require(s -> !s.contains(PartsSeparator), stack,
                () -> format("expected urn stack part to not contain '%s', got: '%s'", ParentSeparator, stack)
        );
        var project = urnParts[1];
        require(p -> isNonEmptyOrNull(p), project,
                () -> format("expected urn project part to be not empty, got: '%s'", project)
        );
        require(p -> !p.contains(PartsSeparator), project,
                () -> format("expected urn project part to not contain '%s', got: '%s'", ParentSeparator, project)
        );
        var qualifiedType = urnParts[2];
        require(q -> isNonEmptyOrNull(q), qualifiedType,
                () -> format("expected urn qualifiedType part to be not empty, got: '%s'", qualifiedType)
        );
        var name = urnParts[3];
        require(n -> isNonEmptyOrNull(n), name,
                () -> format("expected urn name part to be not empty, got: '%s'", name)
        );

        return new Urn(stack, project, QualifiedTypeName.parse(qualifiedType), name);
    }

    public String asString() {
        return Prefix + this.stack + PartsSeparator
                + this.project + PartsSeparator
                + this.qualifiedType.asString() + PartsSeparator
                + this.name;
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static final class QualifiedTypeName {
        // Contains the entire parent string, if exists. (i.e. parent1Type$parent2Type$...)
        public final Optional<String> parents;
        public final Type type;

        public QualifiedTypeName(Optional<String> parents, Type type) {
            this.parents = requireNonNull(parents);
            this.type = requireNonNull(type);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QualifiedTypeName that = (QualifiedTypeName) o;
            return parents.equals(that.parents) && type.equals(that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parents, type);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", QualifiedTypeName.class.getSimpleName() + "[", "]")
                    .add("parent=" + parents)
                    .add("type=" + type)
                    .toString();
        }

        @InternalUse
        @SuppressWarnings("ConstantConditions") // IntelliJ can't understand the custom validators
        public static QualifiedTypeName parse(String qualifiedType) {
            var qualifiedTypeParts = qualifiedType.split(ParentSeparatorRegex, -1); // -1 avoids dropping trailing empty strings
            require(qp -> qp.length > 0, qualifiedTypeParts,
                    () -> format("expected qualified type to not be empty, split by '%s', got '%s'",
                            ParentSeparatorRegex, String.join(", ", qualifiedTypeParts))
            );
            var type = qualifiedTypeParts[qualifiedTypeParts.length - 1];
            require(t -> isNonEmptyOrNull(t), type,
                    () -> format("expected qualified type, type part to be not empty, got: '%s'", type)
            );
            final Optional<String> parent;

            // Handle parents. URNs can have nested parents which should be joined with a "$".
            if (qualifiedTypeParts.length >= 2) {
                // Get only the parent parts of the URN type.
                List<String> parentList = Arrays.asList(qualifiedTypeParts).subList(0, qualifiedTypeParts.length - 1);

                var pt = String.join(ParentSeparator, parentList);
                require(p -> isNonEmptyOrNull(p), pt,
                        () -> format("expected qualified type, parent part to be not empty, got: '%s'", pt)
                );
                parent = Optional.of(pt);
            } else {
                parent = Optional.empty();
            }
            return new QualifiedTypeName(parent, Type.parse(type));
        }

        public String asString() {
            return this.parents.map(p -> p
                    + ParentSeparator).orElse("")
                    + this.type.asString();
        }
    }

    @InternalUse
    @ParametersAreNonnullByDefault
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static final class Type {
        public final String package_;
        public final Optional<String> module;
        public final String typeName;

        public Type(String package_, Optional<String> module, String typeName) {
            this.package_ = requireNonNull(package_);
            this.module = requireNonNull(module);
            this.typeName = requireNonNull(typeName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Type type = (Type) o;
            return package_.equals(type.package_) && module.equals(type.module) && typeName.equals(type.typeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(package_, module, typeName);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", Type.class.getSimpleName() + "[", "]")
                    .add("package_='" + package_ + "'")
                    .add("module=" + module)
                    .add("typeName='" + typeName + "'")
                    .toString();
        }

        @InternalUse
        @SuppressWarnings("ConstantConditions") // IntelliJ can't understand the custom validators
        public static Type parse(String type) {
            final var typeParts = type.split(TypeSeparatorRegex, -1); // -1 avoids dropping trailing empty strings
            require(tp -> tp.length == 2 || tp.length == 3, typeParts,
                    () -> format("type token '%s' does not match the expected format 'package%smodule?%stypename'",
                                 type, TypeSeparator, TypeSeparator)
            );
            final var package_ = typeParts[0];
            require(p -> isNonEmptyOrNull(p), package_,
                    () -> format("type token '%s' does not match the expected format 'package%smodule?%stypename' because the 'package' part is empty",
                                 type, TypeSeparator, TypeSeparator)
            );
            final Optional<String> module = typeParts.length == 3 && isNonEmptyOrNull(typeParts[1])
                ? Optional.of(typeParts[1])
                : Optional.empty();
            final var typeName = typeParts[typeParts.length - 1];
            require(p -> isNonEmptyOrNull(p), typeName,
                    () -> format("type token '%s' does not match the expected format 'package%smodule?%stypename' because the 'typename' part is empty",
                                 type, TypeSeparator, TypeSeparator)
            );
            return new Type(package_, module, typeName);
        }

        public String asString() {
            return this.package_
                    + TypeSeparator + this.module.map(m -> m + TypeSeparator).orElse("")
                    + this.typeName;
        }
    }

    /**
     * Computes a URN from the combination of a resource name, resource type, optional parent,
     * optional project and optional stack.
     */
    @InternalUse
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Output<String> create(
            Output<String> stack,
            Output<String> project,
            Optional<Output<String>> parentUrn,
            Output<String> type,
            Output<String> name
    ) {
        requireNonNull(name);
        requireNonNull(type);

        Output<Optional<String>> flippedParentUrn = parentUrn
                .map(o -> o.applyValue(Optional::of))
                .orElse(Output.of(Optional.empty()));
        //noinspection ConstantConditions
        return Output.tuple(stack, project, flippedParentUrn, type, name)
                .applyValue(t -> create(t.t1, t.t2, t.t3, t.t4, t.t5));
    }

    @InternalUse
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static String create(String stack, String project, Optional<String> parent, String type, String name) {
        requireNonNull(stack);
        requireNonNull(project);
        requireNonNull(type);
        requireNonNull(name);

        var parsedType = Type.parse(type);
        var parsedParent = parent
                .flatMap(p -> Optionals.ofBlank(p))
                .map(Urn::parse)
                .map(urn -> urn.qualifiedType.asString());
        var typeWithOptionalParent = new QualifiedTypeName(parsedParent, parsedType);
        return new Urn(stack, project, typeWithOptionalParent, name).asString();
    }
}