package io.pulumi.json;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@ParametersAreNonnullByDefault
public final class JSON {

    private JSON() {
        throw new UnsupportedOperationException("static class");
    }

    public static JSONObject object() {
        return new JSONObjectImpl();
    }

    public static JSONObject object(Consumer<JSONObject> init) {
        requireNonNull(init);
        var o = new JSONObjectImpl();
        init.accept(o);
        return o;
    }

    public static JSONArray array() {
        return new JSONArrayImpl();
    }

    public static JSONArray array(Consumer<JSONArray> init) {
        requireNonNull(init);
        var a = new JSONArrayImpl();
        init.accept(a);
        return a;
    }
}
