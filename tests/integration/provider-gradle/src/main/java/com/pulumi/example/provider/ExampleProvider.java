package com.pulumi.example.provider;

import com.pulumi.provider.internal.models.*;

import java.util.concurrent.CompletableFuture;
import com.pulumi.provider.internal.Provider;

public class ExampleProvider extends Provider {
    @Override
    public CompletableFuture<GetSchemaResponse> getSchema(GetSchemaRequest request) {
        String schema = "{\n" +
            "    \"name\": \"example\",\n" +
            "    \"version\": \"0.0.1\",\n" +
            "    \"resources\": {\n" +
            "        \"example:index:Resource\": {\n" +
            "            \"properties\": {\n" +
            "                \"value\": {\n" +
            "                    \"type\": \"string\"\n" +
            "                }\n" +
            "            }" +
            "        }\n" +
            "    }\n" +
            "}";
        return CompletableFuture.completedFuture(new GetSchemaResponse(schema));
    }
}
