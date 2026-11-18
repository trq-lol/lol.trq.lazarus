/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import lol.trq.lazarus.patch.ClassPatch;

/** Applies, in order, every patch that matches a loading class. */
final class PatchingTransformer implements ClassFileTransformer {
    private final List<ClassPatch> patches;

    PatchingTransformer(List<ClassPatch> patches) {
        this.patches = List.copyOf(patches);
    }

    @Override
    public byte[] transform(
            ClassLoader loader, String className, Class<?> redefined, ProtectionDomain domain, byte[] bytes) {
        if (className == null) {
            return null;
        }
        byte[] current = bytes;
        for (ClassPatch patch : patches) {
            if (!patch.matches(className)) {
                continue;
            }
            try {
                byte[] next = patch.apply(className, current);
                if (!Arrays.equals(next, current)) {
                    Log.info("patched " + className + " (" + patch.name() + ")");
                    current = next;
                }
            } catch (RuntimeException | LinkageError e) {
                Log.error("patch " + patch.name() + " failed on " + className, e);
            }
        }
        return current == bytes ? null : current;
    }
}
