/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

/** A 4x4 float matrix in OpenGL's column-major order, with the fixed-function operations. Mutable. */
final class Mat4 {
    final float[] m = new float[16];

    Mat4() {
        identity();
    }

    Mat4 identity() {
        java.util.Arrays.fill(m, 0f);
        m[0] = m[5] = m[10] = m[15] = 1f;
        return this;
    }

    Mat4 set(Mat4 o) {
        System.arraycopy(o.m, 0, m, 0, 16);
        return this;
    }

    Mat4 copy() {
        return new Mat4().set(this);
    }

    /** {@code this = this * b}, as glMultMatrix does. */
    Mat4 mul(float[] b) {
        float[] a = m;
        float[] r = new float[16];
        for (int c = 0; c < 4; c++) {
            for (int row = 0; row < 4; row++) {
                r[c * 4 + row] = a[row] * b[c * 4] + a[4 + row] * b[c * 4 + 1] + a[8 + row] * b[c * 4 + 2]
                        + a[12 + row] * b[c * 4 + 3];
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
        float rad = (float) Math.toRadians(degrees);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);
        float t = 1f - c;
        float[] r = {
            x * x * t + c, y * x * t + z * s, x * z * t - y * s, 0,
            x * y * t - z * s, y * y * t + c, y * z * t + x * s, 0,
            x * z * t + y * s, y * z * t - x * s, z * z * t + c, 0,
            0, 0, 0, 1
        };
        return mul(r);
    }

    Mat4 ortho(double l, double r, double b, double t, double n, double f) {
        float[] o = new float[16];
        o[0] = (float) (2 / (r - l));
        o[5] = (float) (2 / (t - b));
        o[10] = (float) (-2 / (f - n));
        o[12] = (float) (-(r + l) / (r - l));
        o[13] = (float) (-(t + b) / (t - b));
        o[14] = (float) (-(f + n) / (f - n));
        o[15] = 1;
        return mul(o);
    }

    Mat4 frustum(double l, double r, double b, double t, double n, double f) {
        float[] o = new float[16];
        o[0] = (float) (2 * n / (r - l));
        o[5] = (float) (2 * n / (t - b));
        o[8] = (float) ((r + l) / (r - l));
        o[9] = (float) ((t + b) / (t - b));
        o[10] = (float) (-(f + n) / (f - n));
        o[11] = -1;
        o[14] = (float) (-2 * f * n / (f - n));
        return mul(o);
    }

    /** Transforms a direction (w = 0). */
    float[] transformDir(float x, float y, float z) {
        return new float[] {
            m[0] * x + m[4] * y + m[8] * z, m[1] * x + m[5] * y + m[9] * z, m[2] * x + m[6] * y + m[10] * z
        };
    }

    /** The inverse transpose of the upper 3x3, column-major, for transforming normals. */
    float[] normalMatrix() {
        float a = m[0], b = m[4], c = m[8];
        float d = m[1], e = m[5], f = m[9];
        float g = m[2], h = m[6], i = m[10];
        float det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
        if (Math.abs(det) < 1e-12f) {
            return new float[] {1, 0, 0, 0, 1, 0, 0, 0, 1};
        }
        float inv = 1f / det;
        // inverse transpose, column-major
        return new float[] {
            (e * i - f * h) * inv, -(b * i - c * h) * inv, (b * f - c * e) * inv,
            -(d * i - f * g) * inv, (a * i - c * g) * inv, -(a * f - c * d) * inv,
            (d * h - e * g) * inv, -(a * h - b * g) * inv, (a * e - b * d) * inv
        };
    }
}
