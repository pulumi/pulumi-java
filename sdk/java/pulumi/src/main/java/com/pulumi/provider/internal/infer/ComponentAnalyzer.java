package com.pulumi.provider.internal.infer;

import com.pulumi.core.Output;
import com.pulumi.core.annotations.Export;
import com.pulumi.core.annotations.Import;
import com.pulumi.provider.internal.Metadata;
import com.pulumi.provider.internal.schema.BuiltinTypeSpec;
import com.pulumi.provider.internal.schema.ResourceSpec;
import com.pulumi.provider.internal.schema.PropertySpec;
import com.pulumi.provider.internal.schema.ComplexTypeSpec;
import com.pulumi.provider.internal.schema.TypeSpec;
import com.pulumi.resources.ComponentResource;
import com.pulumi.provider.internal.schema.PackageSpec;
import com.pulumi.resources.ResourceArgs;
import com.pulumi.asset.Archive;
import com.pulumi.asset.Asset;
import com.pulumi.resources.CustomResource;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.TreeSet;
import java.util.Set;

public final class ComponentAnalyzer {
    private final Metadata metadata;
    private final Map<String, ComplexTypeSpec> typeDefinitions = new HashMap<>();

    private ComponentAnalyzer(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Analyzes the given classes as Pulumi components and generates a package schema.
     * Package name is derived from the first component class's package.
     * @param classes The component resource classes to analyze
     * @return A PackageSpec containing the complete schema for all components and their types
     */
    public static PackageSpec generateSchema(Class<?>... classes) {
        if (classes.length == 0) {
            throw new IllegalArgumentException("At least one component class must be provided");
        }

        // Get package name from first component class
        String name = classes[0].getPackage().getName();
        // Use last segment of package name if it contains dots
        name = name.substring(name.lastIndexOf('.') + 1);

        return generateSchema(new Metadata(name, null, null), classes);
    }

    private static String getNamespace(Class<?> clazz) {
        String[] split = classes[0].getPackage().getName().split("\\.");
        if (split.length == 0) {
            return "";
        }
        return split[1];
    }


    /**
     * Analyzes the given classes as Pulumi components and generates a package schema.
     * @param metadata The package metadata including name (required), version and display name (optional)
     * @param classes The component resource classes to analyze
     * @return A PackageSpec containing the complete schema for all components and their types
     * @throws IllegalArgumentException if metadata name is null or empty
     */
    public static PackageSpec generateSchema(Metadata metadata, Class<?>... classes) {
        if (metadata == null || metadata.getName() == null || metadata.getName().isEmpty()) {
            throw new IllegalArgumentException("Metadata with non-empty name is required");
        }

        ComponentAnalyzer analyzer = new ComponentAnalyzer(metadata);
        Map<String, ResourceSpec> components = new HashMap<>();

        if (classes.length == 0) {
            throw new IllegalArgumentException("At least one component class must be provided");
        }

        String namespace = getNamespace(classes[0]);

        for (Class<?> clazz : classes) {
            if (getNamespace(clazz) != namespace) {
                throw new IllegalArgumentException("All classes must be in the same top level package");
            }
            if (ComponentResource.class.isAssignableFrom(clazz) && !clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                components.put(clazz.getSimpleName(), analyzer.analyzeComponent(clazz));
            }
        }

        return analyzer.generateSchema(metadata, namespace, components, analyzer.typeDefinitions);
    }

    private PackageSpec generateSchema(
            Metadata metadata,
            String namespace,
            Map<String, ResourceSpec> components,
            Map<String, ComplexTypeSpec> typeDefinitions) {

        PackageSpec pkg = new PackageSpec();
        pkg.setName(metadata.getName());
        pkg.setVersion(metadata.getVersion());
        pkg.setDisplayName(metadata.getDisplayName() != null ? metadata.getDisplayName() : metadata.getName());
        pkg.setNamespace(namespace);

        // Set up language settings
        Map<String, Object> languageSettings = new HashMap<>();
        languageSettings.put("respectSchemaVersion", true);
        pkg.getLanguage().put("nodejs", languageSettings);
        pkg.getLanguage().put("python", languageSettings);
        pkg.getLanguage().put("csharp", languageSettings);
        pkg.getLanguage().put("java", languageSettings);
        pkg.getLanguage().put("go", languageSettings);

        // Process components
        components.forEach((componentName, component) -> {
            String name = String.format("%s:index:%s", metadata.getName(), componentName);
            pkg.getResources().put(name, component);
        });

        // Process type definitions
        typeDefinitions.forEach((typeName, type) -> {
            pkg.getTypes().put(
                String.format("%s:index:%s", metadata.getName(), typeName),
                type
            );
        });

        return pkg;
    }

    private ResourceSpec analyzeComponent(Class<?> componentType) {
        Class<?> argsType = getArgsType(componentType);
        var analysis = analyzeType(argsType);
        var outputAnalysis = analyzeOutputsWithRequired(componentType);

        return new ResourceSpec(
                analysis.properties(),
                analysis.required(),
                outputAnalysis.properties(),
                outputAnalysis.required()
        );
    }

    private Class<?> getArgsType(Class<?> componentType) {
        return Arrays.stream(componentType.getConstructors())
                .findFirst()
                .map(constructor -> Arrays.stream(constructor.getParameters())
                        .filter(param -> ResourceArgs.class.isAssignableFrom(param.getType()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException(
                            String.format("Component %s must have exactly one constructor parameter that extends ResourceArgs",
                                componentType.getName()))))
                .map(Parameter::getType)
                .orElseThrow();
    }

    private TypeAnalysis analyzeType(Class<?> type) {
        Map<String, PropertySpec> properties = new HashMap<>();
        Set<String> required = new TreeSet<>();

        Arrays.stream(type.getDeclaredFields())
                .forEach(field -> {
                    String schemaName = getSchemaPropertyName(field);
                    analyzeProperty(field).ifPresent(propertyDef -> {
                        properties.put(schemaName, propertyDef);
                        if (!isOptionalProperty(field)) {
                            required.add(schemaName);
                        }
                    });
                });

        return new TypeAnalysis(properties, required);
    }

    private TypeAnalysis analyzeOutputsWithRequired(Class<?> type) {
        Map<String, PropertySpec> properties = new HashMap<>();
        Set<String> required = new TreeSet<>();

        Arrays.stream(type.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Export.class))
                .forEach(field -> {
                    Export exportAnnotation = field.getAnnotation(Export.class);
                    String schemaName = exportAnnotation.name().isEmpty() ?
                            getSchemaPropertyName(field) : exportAnnotation.name();

                    analyzeProperty(field).ifPresent(propertyDef -> {
                        properties.put(schemaName, propertyDef);
                        if (!isOptionalProperty(field)) {
                            required.add(schemaName);
                        }
                    });
                });

        return new TypeAnalysis(properties, required);
    }

    private Optional<PropertySpec> analyzeProperty(java.lang.reflect.Field field) {
        Type fieldType = field.getGenericType();
        try {
            boolean isOutput = fieldType instanceof ParameterizedType &&
                ((ParameterizedType) fieldType).getRawType().equals(Output.class);

            TypeSpec typeSpec = analyzeTypeParameter(fieldType, field.getDeclaringClass().getSimpleName() + "." + field.getName(), isOutput);

            return Optional.of(new PropertySpec(
                typeSpec.getType(),
                typeSpec.getRef(),
                typeSpec.getPlain(),
                typeSpec.getItems(),
                typeSpec.getAdditionalProperties()
            ));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage().replace(fieldType.getTypeName(),
                field.getDeclaringClass().getSimpleName() + "." + field.getName()));
        }
    }

    private boolean isOptionalProperty(java.lang.reflect.Field field) {
        if (field.getType().equals(Optional.class)) {
            return true;
        }

        Import importAnnotation = field.getAnnotation(Import.class);
        if (importAnnotation != null) {
            return !importAnnotation.required();
        }

        return true;
    }

    private String getSchemaPropertyName(java.lang.reflect.Field field) {
        Import importAnnotation = field.getAnnotation(Import.class);
        if (importAnnotation != null && !importAnnotation.name().isEmpty()) {
            return importAnnotation.name();
        }

        return field.getName();
    }

    private boolean isBuiltinType(Class<?> type) {
        return type.equals(String.class) ||
               type.equals(Integer.class) || type.equals(int.class) ||
               type.equals(Long.class) || type.equals(long.class) ||
               type.equals(Double.class) || type.equals(double.class) ||
               type.equals(Float.class) || type.equals(float.class) ||
               type.equals(Boolean.class) || type.equals(boolean.class);
    }

    private Optional<String> getSpecialTypeRef(Class<?> type) {
        if (type.equals(Archive.class)) {
            return Optional.of("pulumi.json#/Archive");
        } else if (type.equals(Asset.class)) {
            return Optional.of("pulumi.json#/Asset");
        }
        return Optional.empty();
    }

    private String getBuiltinTypeName(Class<?> type) {
        if (type.equals(String.class)) {
            return BuiltinTypeSpec.STRING;
        }
        if (type.equals(Integer.class) || type.equals(int.class) ||
            type.equals(Long.class) || type.equals(long.class)) {
            return BuiltinTypeSpec.INTEGER;
        }
        if (type.equals(Double.class) || type.equals(double.class) ||
            type.equals(Float.class) || type.equals(float.class)) {
            return BuiltinTypeSpec.NUMBER;
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return BuiltinTypeSpec.BOOLEAN;
        }
        return BuiltinTypeSpec.OBJECT;
    }

    private String getTypeName(Class<?> type) {
        String name = type.getSimpleName();
        return name.endsWith("Args") ? name.substring(0, name.length() - 4) : name;
    }

    private boolean isResourceType(Class<?> type) {
        return CustomResource.class.isAssignableFrom(type);
    }

    private static final class TypeAnalysis {
        private final Map<String, PropertySpec> properties;
        private final Set<String> required;

        private TypeAnalysis(Map<String, PropertySpec> properties, Set<String> required) {
            this.properties = properties;
            this.required = required;
        }

        public Map<String, PropertySpec> properties() {
            return properties;
        }

        public Set<String> required() {
            return required;
        }
    }

    private TypeSpec analyzeTypeParameter(Type type, String context, boolean isOutput) {
        if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            if (paramType.getRawType().equals(Output.class)) {
                // Analyze inner type, marking it as being in an Output
                return analyzeTypeParameter(paramType.getActualTypeArguments()[0], context, true);
            }
            if (paramType.getRawType().equals(List.class)) {
                // Propagate isOutput to list items
                TypeSpec itemSpec = analyzeTypeParameter(paramType.getActualTypeArguments()[0], context, isOutput);
                return TypeSpec.ofArray(itemSpec);
            }
            if (paramType.getRawType().equals(Map.class)) {
                Type keyType = paramType.getActualTypeArguments()[0];
                if (!keyType.equals(String.class)) {
                    throw new IllegalArgumentException(
                        String.format("map keys must be strings, got '%s' for '%s'",
                            ((Class<?>)keyType).getSimpleName(),
                            context)
                    );
                }
                // Propagate isOutput to map values
                TypeSpec valueSpec = analyzeTypeParameter(paramType.getActualTypeArguments()[1], context, isOutput);
                return TypeSpec.ofDict(valueSpec);
            }
        }

        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (isBuiltinType(clazz)) {
                return TypeSpec.ofBuiltin(getBuiltinTypeName(clazz), !isOutput);
            }

            var specialTypeRef = getSpecialTypeRef(clazz);
            if (specialTypeRef.isPresent()) {
                return TypeSpec.ofRef(specialTypeRef.get(), !isOutput);
            }

            if (isResourceType(clazz)) {
                throw new IllegalArgumentException(
                    String.format("Resource references are not supported yet: found type '%s' for '%s'",
                        clazz.getSimpleName(), context)
                );
            }

            if (!clazz.isInterface() && !clazz.isPrimitive() && clazz != String.class) {
                String typeName = getTypeName(clazz);
                String typeRef = String.format("#/types/%s:index:%s", metadata.getName(), typeName);

                // Only analyze if we haven't already done so
                if (!typeDefinitions.containsKey(typeName)) {
                    // Add empty definition to prevent infinite recursion
                    typeDefinitions.put(typeName, ComplexTypeSpec.ofObject(Map.of(), Set.of()));

                    // Then analyze and update with actual properties
                    var analysis = analyzeType(clazz);
                    typeDefinitions.put(typeName, ComplexTypeSpec.ofObject(
                        analysis.properties(),
                        analysis.required()));
                }

                return TypeSpec.ofRef(typeRef, !isOutput);
            }
        }

        throw new IllegalArgumentException("Unsupported type parameter: " + type);
    }
}
