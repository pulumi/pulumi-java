package com.pulumi.provider.internal;

import java.lang.Package;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Set;

import org.reflections.Reflections;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.pulumi.core.internal.Internal;
import com.pulumi.core.Output;
import com.pulumi.provider.internal.infer.ComponentAnalyzer;
import com.pulumi.provider.internal.models.*;
import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.provider.internal.properties.PropertyValueSerializer;
import com.pulumi.provider.internal.Provider;
import com.pulumi.resources.ComponentResource;
import com.pulumi.resources.ComponentResourceOptions;

public class ComponentProvider implements Provider {
    private final Metadata metadata;
    private final String basePackageName;

    public ComponentProvider(Metadata metadata, Package pkg) {
        this.metadata = metadata;
        this.basePackageName = pkg.getName();
    }
    
    @Override
    public CompletableFuture<GetSchemaResponse> getSchema(GetSchemaRequest request) {
        // Find all component classes in the package
        Reflections reflections = new Reflections(this.basePackageName);
        Set<Class<? extends ComponentResource>> componentClasses = reflections.getSubTypesOf(ComponentResource.class);
        
        // Generate schema for all component classes
        var schema = ComponentAnalyzer.generateSchema(this.metadata, componentClasses.toArray(new Class<?>[0]));

        // Serialize the schema to JSON
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return CompletableFuture.completedFuture(new GetSchemaResponse(gson.toJson(schema)));
    }

    @Override
    public CompletableFuture<ConstructResponse> construct(ConstructRequest request) {
        String type = request.getType();
        String[] parts = type.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                String.format("Component type must be in format 'package:index:Name', got: %s", type));
        }
        if (!parts[0].equals(this.metadata.getName())) {
            throw new IllegalArgumentException(
                String.format("Component type must start with '%s', got: %s", this.metadata.getName(), parts[0]));
        }
        if (!parts[1].equals("index")) {
            throw new IllegalArgumentException(
                String.format("Component type must have 'index' as second part, got: %s", parts[1]));
        }
        String className = parts[2];

        // Get the component class using the provided base package name
        Class<?> componentClass = null;                
        try {
            componentClass = Class.forName(basePackageName + "." + className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to construct component: " + request.getType(), e);
        }

        // Validate the class extends ComponentResource
        if (!ComponentResource.class.isAssignableFrom(componentClass)) {
            throw new IllegalArgumentException(
                String.format("Class %s must extend ComponentResource", className));
        }

        // Get all constructors and validate we have exactly one
        var constructors = componentClass.getDeclaredConstructors();
        if (constructors.length != 1) {
            throw new IllegalArgumentException(
                String.format("Component %s must have exactly one constructor, found %d", className, constructors.length));
        }
                       
        // Validate constructor has at least 2 parameters
        var constructor = constructors[0];
        constructor.setAccessible(true);
        var paramTypes = constructor.getParameterTypes();
        if (paramTypes.length != 3) {
            throw new IllegalArgumentException(
                String.format("Component %s constructor must have exactly 3 parameters, found %d", className, paramTypes.length));
        }
        
        // Get and validate the args class
        Class<?> argsClass = paramTypes[1];
        if (!com.pulumi.resources.ResourceArgs.class.isAssignableFrom(argsClass)) {
            throw new IllegalArgumentException(
                String.format("Component %s args parameter must extend ResourceArgs, found %s", className, argsClass.getName()));
        }
        
        // Deserialize the inputs to the Args type
        Object args = PropertyValueSerializer.deserialize(PropertyValue.of(request.getInputs()), argsClass);
        
        // Create component instance using reflection
        ComponentResource comp = null;
        try {
            comp = (ComponentResource) constructor.newInstance(request.getName(), args, request.getOptions());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to construct component: " + request.getType(), e);
        }
        
        // Create the response
        var state = PropertyValueSerializer.stateFromComponentResource(comp);
        return Internal.of(comp.urn()).getValueNullable()
                .thenApply(urn -> new ConstructResponse(urn, state, new HashMap<String, Set<String>>()));
    }
}
