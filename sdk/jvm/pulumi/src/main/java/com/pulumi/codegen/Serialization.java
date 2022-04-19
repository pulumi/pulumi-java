package com.pulumi.codegen;

import com.google.gson.Gson;

import java.util.Map;

/**
 * Contains utility functions for JSON serialization used by generated Pulumi programs.
 */
public class Serialization {
    /**
     * Converts the input Map(String, Object) into a JSON string.
     * This function maps to the toJSON({...}) function from PCL.
     */
    public static String SerializeJson(Map<String, Object> object) {
        var gson = new Gson();
        return gson.toJson(object);
    }

    /**
     * Accepts an array of Entry(String, Object) values to build up a Map(String, Object).
     * The entries are usually created using the JsonProperty(String key, Object value) function.
     */
    @SafeVarargs
    public static Map<String, Object> JsonObject(Map.Entry<String, Object> ...entries) {
        return Map.ofEntries(entries);
    }

    /**
     * Creates an Entry(String, Object) used to build up a Map(String, Object) using JsonObject(entries) function.
     */
    public static Map.Entry<String, Object> JsonProperty(String name, Object value) {
        return Map.entry(name, value);
    }

    /**
     * Accepts an array of objects to be serialized as a JSON array.
     */
    public static Object[] JsonArray(Object ...values) {
        return values;
    }
}
