package io.pulumi.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
final class JSONArrayImpl extends GSONObject<JsonArray> implements JSONArray {

    public JSONArrayImpl(JsonArray node) {
        super(requireNonNull(node));
    }

    public JSONArrayImpl() {
        this(new JsonArray());
    }

    public void object(Consumer<JSONObject> value) {
        var o = new JSONObjectImpl();
        value.accept(o);
        this.node.add(o.node);
    }

    public void array(Consumer<JSONArray> value) {
        var a = new JSONArrayImpl();
        value.accept(a);
        this.node.addAll(a.node);
    }

    public void string(String value) {
        this.node.add(value);
    }

    public void number(int value) {
        this.node.add(value);
    }

    public void number(long value) {
        this.node.add(value);
    }

    public void number(double value) {
        this.node.add(value);
    }

    public void boolean_(boolean value) {
        this.node.add(value);
    }

    public void true_() {
        this.node.add(true);
    }

    public void false_() {
        this.node.add(false);
    }

    public void null_() {
        this.node.add(JsonNull.INSTANCE);
    }
}