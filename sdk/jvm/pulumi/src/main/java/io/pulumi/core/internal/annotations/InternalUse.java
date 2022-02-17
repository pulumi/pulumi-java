package io.pulumi.core.internal.annotations;

import java.lang.annotation.*;

/**
 * Annotates a program element (class, method, package, etc) which is internal to Pulumi, not part of
 * the public API, and should not be used by users of Pulumi.
 *
 * <p>However, if you want to implement internal implementation you may use the internal parts.
 * Please consult the Pulumi team first, because internal APIs don't have the same API stability
 * guarantee as the public APIs do.
 *
 * <p>Note: This annotation is intended only for Pulumi library code. Users should not attach this
 * annotation to their own code.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PACKAGE,
        ElementType.TYPE})
@Documented
public @interface InternalUse {
    /* Empty */
}
