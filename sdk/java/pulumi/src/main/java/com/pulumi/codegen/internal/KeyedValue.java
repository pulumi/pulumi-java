package com.pulumi.codegen.internal;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generic value T with a key of type String.
 * This class encapsulates values being iterated through a PCL range expression
 * which have key and value property.
 */
public class KeyedValue<T> {
    private final T value;
    private final String key;
    public KeyedValue(String key, T value) {
        this.value = value;
        this.key = key;
    }

    /**
     * Creates an instance of KeyedValue(U)
     * @param key the key of the value
     * @param input the value itself
     * @return an instance of KeyedValue(U)
     * @param <U> the type of the value
     */
    public static <U> KeyedValue<U> create(String key, U input) {
        return new KeyedValue<>(key, input);
    }

    /**
     * Returns the key of the pair
     */
    public String key() {
        return this.key;
    }


    /**
     * Returns the value of the pair
     */
    public T value() {
        return this.value;
    }

    /**
     * Takes an Iterator(T) and creates a list of KeyedValue(T) where each key
     * is the index of the value from the iterator.
     */
    public static <T> List<KeyedValue<T>> of(Iterable<T> items) {
        var results = new ArrayList<KeyedValue<T>>();
        int counter = 0;
        for(var item : items)
        {
            var key = String.valueOf(counter);
            results.add(KeyedValue.create(key, item));
        }
        return results;
    }
}

