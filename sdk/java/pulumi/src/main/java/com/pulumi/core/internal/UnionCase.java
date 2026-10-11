package com.pulumi.core.internal;

import com.pulumi.core.internal.annotations.InternalUse;

/**
 * A case class of a {@link com.pulumi.core.annotations.UnionType} interface: a member of the
 * union that is not itself a class implementing the interface, held by value.
 * <p>
 * The Pulumi runtime serializes a case class as its {@link #value()}.
 */
@InternalUse
public interface UnionCase {
    /**
     * @return the wrapped member value
     */
    Object value();
}
