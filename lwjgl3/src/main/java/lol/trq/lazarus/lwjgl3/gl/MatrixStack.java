/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.util.Arrays;

/** A matrix stack that reuses its matrices, so push and pop never allocate after warm-up. */
final class MatrixStack {
    private Mat4[] stack = {new Mat4()};
    private int depth;

    Mat4 top() {
        return stack[depth];
    }

    void push() {
        if (depth + 1 == stack.length) {
            stack = Arrays.copyOf(stack, stack.length * 2);
        }
        Mat4 next = stack[depth + 1];
        if (next == null) {
            next = stack[depth + 1] = new Mat4();
        }
        next.set(stack[depth]);
        depth++;
    }

    void pop() {
        if (depth > 0) {
            depth--;
        }
    }
}
