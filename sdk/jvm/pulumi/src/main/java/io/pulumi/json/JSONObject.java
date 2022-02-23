package io.pulumi.json;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public interface JSONObject extends JSONElement {
    void object(String key, Consumer<JSONObject> value);

    void array(String key, Consumer<JSONArray> value);

    void string(String key, String value);

    void number(String key, int value);

    void number(String key, long value);

    void number(String key, double value);

    void boolean_(String key, boolean value);

    void true_(String key);

    void false_(String key);

    void null_(String key);
}