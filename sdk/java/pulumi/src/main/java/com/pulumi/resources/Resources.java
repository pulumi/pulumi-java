package com.pulumi.resources;

import com.pulumi.core.internal.Copyable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class providing helper methods for working with resource collections.
 *
 * @see Resource
 */
public class Resources {

    /**
     * Private constructor to prevent instantiation of this utility class.
     *
     * @throws UnsupportedOperationException always thrown to prevent instantiation
     */
    private Resources() {
        throw new UnsupportedOperationException("static class");
    }

    /**
     * Merges two nullable lists into a single list.
     *
     * @param original   the original list, may be null
     * @param additional the additional list to merge, may be null
     * @param <T>        the type of elements in the lists
     * @return a merged list containing all elements from both lists, or null if both are null
     */
    @Nullable
    protected static <T> List<T> mergeNullableList(@Nullable List<T> original, @Nullable List<T> additional) {
        if (original == null && additional == null) {
            return null;
        }
        if (original != null && additional == null) {
            return original;
        }
        //noinspection ConstantConditions
        if (original == null && additional != null) {
            return additional;
        }
        var list = new ArrayList<>(original);
        list.addAll(additional);
        return list;
    }

    /**
     * Returns a shallow copy of the given list, or null if the original is null.
     *
     * @param original the list to copy, may be null
     * @param <T>      the type of elements in the list
     * @return a shallow copy of the {@code original} list, or null if the original is null
     */
    protected static <T> List<T> copyNullableList(@Nullable List<T> original) {
        if (original == null) {
            return null;
        }
        return List.copyOf(original);
    }

    /**
     * Creates a copy of the given object that implements the {@link Copyable} interface.
     * If the original object is null, returns null.
     *
     * @param original the object to copy, may be null
     * @param <T>      the type of the object, must implement {@link Copyable}
     * @return a copy of the original object, or null if the original is null
     */
    protected static <T extends Copyable<T>> T copyNullable(@Nullable T original) {
        if (original == null) {
            return null;
        }
        return original.copy();
    }
}
