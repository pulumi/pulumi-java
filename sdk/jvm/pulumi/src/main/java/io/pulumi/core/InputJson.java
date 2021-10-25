package io.pulumi.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.pulumi.core.internal.InputOutputData;
import io.pulumi.core.internal.TypedInputOutput;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents an @see {@link Input} value that wraps a @see {@link JsonElement}
 */
public final class InputJson extends InputImpl<JsonElement, Input<JsonElement>> implements Input<JsonElement> {

    private InputJson(JsonElement json) {
        super(json, false);
    }

    private InputJson(CompletableFuture<JsonElement> future, boolean isSecret) {
        super(future, isSecret);
    }

    private InputJson(CompletableFuture<InputOutputData<JsonElement>> dataFuture) {
        super(dataFuture);
    }

    @Override
    protected Input<JsonElement> newInstance(CompletableFuture<InputOutputData<JsonElement>> dataFuture) {
        return new InputJson(dataFuture);
    }

    @Override
    public <U> Input<U> apply(Function<JsonElement, Input<U>> func) {
        return new InputDefault<>(InputOutputData.apply(dataFuture, func.andThen(
                o -> TypedInputOutput.cast(o).internalGetDataAsync())
        ));
    }

    // Static section -----

    public static InputJson of() {
        return new InputJson(JsonNull.INSTANCE);
    }

    public static InputJson of(JsonElement json) {
        return new InputJson(json);
    }

    public static InputJson of(Output<JsonElement> json) {
        return new InputJson(TypedInputOutput.cast(json).internalGetDataAsync());
    }

    public static InputJson parse(String json) {
        var gson = new Gson();
        return new InputJson(gson.fromJson(json, JsonElement.class));
    }

    public static InputJson parse(Output<String> json) {
        var gson = new Gson();
        return InputJson.of(json.applyValue((String j) -> gson.fromJson(j, JsonElement.class)));
    }
}