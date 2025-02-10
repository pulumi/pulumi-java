package com.pulumi.example.provider;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Set;

import com.pulumi.core.internal.Internal;
import com.pulumi.core.Output;
import com.pulumi.provider.internal.models.*;
import com.pulumi.provider.internal.properties.PropertyValue;
import com.pulumi.provider.internal.Provider;

public class ExampleProvider implements Provider {
    @Override
    public CompletableFuture<GetSchemaResponse> getSchema(GetSchemaRequest request) {
        var url = getClass().getResource("schema.json");
        if (url == null) {
            throw new RuntimeException("Could not find schema.json in package");
        }
        try {
            String schema = new String(Files.readAllBytes(Paths.get(url.toURI())));
            return CompletableFuture.completedFuture(new GetSchemaResponse(schema));
        } catch (Exception ex) {
            throw new RuntimeException("Could not read schema.json in package");
        }
    }

    @Override
    public CompletableFuture<ConstructResponse> construct(ConstructRequest request) {
        var length = request.getInputs().get("length").getNumberValue().intValue();
        var args = new HelloWorldArgs(Output.of(length));
        var comp = new HelloWorld(request.getName(), args, request.getOptions());
        var state = new HashMap<String, PropertyValue>();
        var value = Internal.of(comp.value).getValueNullable().join();
        if (value != null) {
            state.put("value", PropertyValue.of(value));
        }
        var urn = Internal.of(comp.urn()).getValueNullable().join();
        var response = new ConstructResponse(urn, state, new HashMap<String, Set<String>>());
        return CompletableFuture.completedFuture(response);
    }
}
