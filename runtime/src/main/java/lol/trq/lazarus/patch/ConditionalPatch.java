/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Set;
import java.util.function.BooleanSupplier;

/** Applies another patch only while a condition holds, checked when a matching class loads. */
public final class ConditionalPatch implements ClassPatch {
    private final ClassPatch delegate;
    private final BooleanSupplier condition;

    /**
     * Creates the patch.
     *
     * @param delegate the patch
     * @param condition when it applies
     */
    public ConditionalPatch(ClassPatch delegate, BooleanSupplier condition) {
        this.delegate = delegate;
        this.condition = condition;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Set<String> targets() {
        return delegate.targets();
    }

    @Override
    public Set<String> prefixes() {
        return delegate.prefixes();
    }

    @Override
    public boolean matches(String className) {
        return delegate.matches(className) && condition.getAsBoolean();
    }

    @Override
    public byte[] apply(String className, byte[] bytes) {
        return delegate.apply(className, bytes);
    }
}
