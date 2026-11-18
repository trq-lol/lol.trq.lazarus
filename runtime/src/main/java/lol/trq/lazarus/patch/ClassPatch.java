/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Set;

/** A bytecode patch for one or more classes, applied as they load. */
public interface ClassPatch {
    /** {@return a short name for logs} */
    String name();

    /** {@return the internal names (slash-separated) of the classes this patch applies to} */
    Set<String> targets();

    /** {@return internal-name prefixes; the patch also applies to every class whose name starts with one} */
    default Set<String> prefixes() {
        return Set.of();
    }

    /**
     * {@return whether this patch applies to a class}
     *
     * @param className the internal name of the class
     */
    default boolean matches(String className) {
        if (targets().contains(className)) {
            return true;
        }
        for (String prefix : prefixes()) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Patches one class.
     *
     * @param className the internal name of the class
     * @param bytes the class file
     * @return the patched class file
     */
    byte[] apply(String className, byte[] bytes);
}
