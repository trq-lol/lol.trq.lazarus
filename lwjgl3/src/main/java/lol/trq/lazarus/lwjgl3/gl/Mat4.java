/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.util.Arrays;

/** A 4x4 float matrix in OpenGL's column-major order, with the fixed-function operations. Mutable, allocation-free. */
final class Mat4 {
    final float[] m = new float[16];
    private final float[] operand = new float[16];
    private final float[] result = new float[16];

    Mat4() {
        identity();
    }

    Mat4 identity() {
        Arrays.fill(m, 0f);
        m[0] = m[5] = m[10] = m[15] = 1f;
        return this;
    }

    Mat4 set(Mat4 o) {
        System.arraycopy(o.m, 0, m, 0, 16);
        return this;
    }

    /** {@code this = this * b}, as glMultMatrix does. */
    Mat4 mul(float[] b) {
        float[] a = m;
        float[] r = result;
        for (int c = 0; c < 4; c++) {
            float b0 = b[c * 4], b1 = b[c * 4 + 1], b2 = b[c * 4 + 2], b3 = b[c * 4 + 3];
            for (int row = 0; row < 4; row++) {
                r[c * 4 + row] = a[row] * b0 + a[4 + row] * b1 + a[8 + row] * b2 + a[12 + row] * b3;
            }
        }
        System.arraycopy(r, 0, m, 0, 16);
        return this;
    }

    Mat4 translate(float x, float y, float z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
        m[15] += m[3] * x + m[7] * y + m[11] * z;
        return this;
    }

    Mat4 scale(float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            m[i] *= x;
            m[4 + i] *= y;
            m[8 + i] *= z;
        }
        return this;
    }

    Mat4 rotate(float degrees, float x, float y, float z) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len == 0f) {
            return this;
        }
        x /= len;
        y /= len;
        z /= len;
        double rad = Math.toRadians(degrees);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        float t = 1f - c;
        float[] r = operand;
        r[0] = x * x * t + c;
        r[1] = y * x * t + z * s;
        r[2] = x * z * t - y * s;
        r[3] = 0;
        r[4] = x * y * t - z * s;
        r[5] = y * y * t + c;
        r[6] = y * z * t + x * s;
        r[7] = 0;
        r[8] = x * z * t + y * s;
        r[9] = y * z * t - x * s;
        r[10] = z * z * t + c;
        r[11] = 0;
        r[12] = 0;
        r[13] = 0;
        r[14] = 0;
        r[15] = 1;
        return mul(r);
    }

    Mat4 ortho(double l, double r, double b, double t, double n, double f) {
        float[] o = operand;
        Arrays.fill(o, 0f);
        o[0] = (float) (2 / (r - l));
        o[5] = (float) (2 / (t - b));
        o[10] = (float) (-2 / (f - n));
        o[12] = (float) (-(r + l) / (r - l));
        o[13] = (float) (-(t + b) / (t - b));
        o[14] = (float) (-(f + n) / (f - n));
        o[15] = 1;
        return mul(o);
    }

    /** Transforms a direction (w = 0), normalized, into {@code out}. */
    void transformDirNormalized(float x, float y, float z, float[] out) {
        float dx = m[0] * x + m[4] * y + m[8] * z;
        float dy = m[1] * x + m[5] * y + m[9] * z;
        float dz = m[2] * x + m[6] * y + m[10] * z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        out[0] = dx;
        out[1] = dy;
        out[2] = dz;
    }

    /** Writes the inverse transpose of the upper 3x3, column-major, into {@code out}: the normal matrix. */
    void normalMatrix(float[] out) {
        float a = m[0], b = m[4], c = m[8];
        float d = m[1], e = m[5], f = m[9];
        float g = m[2], h = m[6], i = m[10];
        float det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (Math.abs(det) < 1e-12f) {
            Arrays.fill(out, 0f);
            out[0] = out[4] = out[8] = 1f;
            return;
        }
        float inv = 1f / det;
        out[0] = (e * i - f * h) * inv;
        out[1] = -(b * i - c * h) * inv;
        out[2] = (b * f - c * e) * inv;
        out[3] = -(d * i - f * g) * inv;
        out[4] = (a * i - c * g) * inv;
        out[5] = -(a * f - c * d) * inv;
        out[6] = (d * h - e * g) * inv;
        out[7] = -(a * h - b * g) * inv;
        out[8] = (a * e - b * d) * inv;
    }
}
