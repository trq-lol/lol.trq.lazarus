/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Set;

/**
 * Applies another patch to every class whose internal name ends with a suffix, so a library is patched even when a
 * mod relocates it under its own package.
 */
public final class SuffixPatch implements ClassPatch {
    private final ClassPatch delegate;
    private final String suffix;

    /**
     * Creates the patch.
     *
     * @param delegate the patch to apply
     * @param suffix the internal-name suffix, such as {@code "org/spongepowered/asm/mixin/Foo"}
     */
    public SuffixPatch(ClassPatch delegate, String suffix) {
        this.delegate = delegate;
        this.suffix = suffix;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public Set<String> targets() {
        return Set.of();
    }

    @Override
    public boolean matches(String className) {
        return className.endsWith(suffix);
    }

    @Override
    public byte[] apply(String className, byte[] bytes) {
        return delegate.apply(className, bytes);
    }
}
