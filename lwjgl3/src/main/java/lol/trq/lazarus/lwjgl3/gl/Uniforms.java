/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.util.Arrays;
import org.lwjgl.opengl.GL20C;

/** Uploads a uniform only when its value differs from the last upload to the same location of one program. */
final class Uniforms {
    private final float[][] floats;
    private final int[] ints;
    private final boolean[] set;

    Uniforms(int locations) {
        floats = new float[locations][];
        ints = new int[locations];
        set = new boolean[locations];
    }

    void i(int location, int value) {
        if (location < 0 || (set[location] && ints[location] == value)) {
            return;
        }
        set[location] = true;
        ints[location] = value;
        GL20C.glUniform1i(location, value);
    }

    void f(int location, float x) {
        if (changed(location, x, 0, 0, 0, 1)) {
            GL20C.glUniform1f(location, x);
        }
    }

    void f3(int location, float[] v) {
        if (changed(location, v[0], v[1], v[2], 0, 3)) {
            GL20C.glUniform3f(location, v[0], v[1], v[2]);
        }
    }

    void f4(int location, float[] v) {
        if (changed(location, v[0], v[1], v[2], v[3], 4)) {
            GL20C.glUniform4f(location, v[0], v[1], v[2], v[3]);
        }
    }

    void mat3(int location, float[] m) {
        if (changedArray(location, m)) {
            GL20C.glUniformMatrix3fv(location, false, m);
        }
    }

    void mat4(int location, float[] m) {
        if (changedArray(location, m)) {
            GL20C.glUniformMatrix4fv(location, false, m);
        }
    }

    private boolean changed(int location, float a, float b, float c, float d, int n) {
        if (location < 0) {
            return false;
        }
        float[] last = floats[location];
        if (last == null) {
            last = floats[location] = new float[4];
        } else if (set[location] && last[0] == a && last[1] == b && last[2] == c && last[3] == d) {
            return false;
        }
        set[location] = true;
        last[0] = a;
        last[1] = b;
        last[2] = c;
        last[3] = d;
        return true;
    }

    private boolean changedArray(int location, float[] value) {
        if (location < 0) {
            return false;
        }
        float[] last = floats[location];
        if (last != null && set[location] && Arrays.equals(last, value)) {
            return false;
        }
        if (last == null || last.length != value.length) {
            last = floats[location] = new float[value.length];
        }
        System.arraycopy(value, 0, last, 0, value.length);
        set[location] = true;
        return true;
    }
}
