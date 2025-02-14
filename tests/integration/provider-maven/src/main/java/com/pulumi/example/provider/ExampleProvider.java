package com.pulumi.example.provider;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.pulumi.core.internal.Internal;
import com.pulumi.core.Output;
import com.pulumi.provider.internal.infer.ComponentAnalyzer;
import com.pulumi.provider.internal.infer.Metadata;
import com.pulumi.provider.internal.models.*;
import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.provider.internal.properties.PropertyValueSerializer;
import com.pulumi.provider.internal.Provider;

public class ExampleProvider implements Provider {
    @Override
    public CompletableFuture<GetSchemaResponse> getSchema(GetSchemaRequest request) {
        var metadata = new Metadata("javap", "0.1.0", "Sample Provider for testing");
        var schema = ComponentAnalyzer.generateSchema(metadata, HelloWorld.class);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return CompletableFuture.completedFuture(new GetSchemaResponse(gson.toJson(schema)));
    }

    @Override
    public CompletableFuture<ConstructResponse> construct(ConstructRequest request) {
        var args = PropertyValueSerializer.deserialize(PropertyValue.of(request.getInputs()), HelloWorldArgs.class);
        var comp = new HelloWorld(request.getName(), args, request.getOptions());
        var state = PropertyValueSerializer.stateFromComponentResource(comp);
        var urn = Internal.of(comp.urn()).getValueNullable().join();
        var response = new ConstructResponse(urn, state, new HashMap<String, Set<String>>());
        return CompletableFuture.completedFuture(response);
    }
}
