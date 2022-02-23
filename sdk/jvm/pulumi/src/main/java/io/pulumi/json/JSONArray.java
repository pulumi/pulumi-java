package io.pulumi.json;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public interface JSONArray extends JSONElement {
    void object(Consumer<JSONObject> value);

    void array(Consumer<JSONArray> value);

    void string(String value);

    void number(int value);

    void number(long value);

    void number(double value);

    void boolean_(boolean value);

    void true_();

    void false_();

    void null_();
}