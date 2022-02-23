package io.pulumi.json;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
final class JSONObjectImpl extends GSONObject<JsonObject> implements JSONObject {

    public JSONObjectImpl(JsonObject node) {
        super(requireNonNull(node));
    }

    public JSONObjectImpl() {
        this(new JsonObject());
    }

    public void object(String key, Consumer<JSONObject> value) {
        var o = new JSONObjectImpl();
        value.accept(o);
        this.node.add(key, o.node);
    }

    public void array(String key, Consumer<JSONArray> value) {
        var a = new JSONArrayImpl();
        value.accept(a);
        this.node.add(key, a.node);
    }

    public void string(String key, String value) {
        this.node.addProperty(key, value);
    }

    public void number(String key, int value) {
        this.node.addProperty(key, value);
    }

    public void number(String key, long value) {
        this.node.addProperty(key, value);
    }

    public void number(String key, double value) {
        this.node.addProperty(key, value);
    }

    public void boolean_(String key, boolean value) {
        this.node.addProperty(key, value);
    }

    public void true_(String key) {
        this.node.addProperty(key, true);
    }

    public void false_(String key) {
        this.node.addProperty(key, false);
    }

    public void null_(String key) {
        this.node.add(key, JsonNull.INSTANCE);
    }
}