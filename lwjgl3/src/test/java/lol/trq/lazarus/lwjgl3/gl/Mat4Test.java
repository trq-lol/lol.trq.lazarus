/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class Mat4Test {
    private static final float EPS = 1e-5f;

    private static float[] apply(Mat4 m, float x, float y, float z) {
        float[] a = m.m;
        return new float[] {
            a[0] * x + a[4] * y + a[8] * z + a[12],
            a[1] * x + a[5] * y + a[9] * z + a[13],
            a[2] * x + a[6] * y + a[10] * z + a[14],
            a[3] * x + a[7] * y + a[11] * z + a[15]
        };
    }

    @Test
    void translateThenScaleAppliesScaleFirst() {
        Mat4 m = new Mat4().translate(1, 2, 3).scale(2, 2, 2);
        assertArrayEquals(new float[] {3, 4, 5, 1}, apply(m, 1, 1, 1), EPS);
    }

    @Test
    void rotateIsCounterClockwiseAboutTheAxis() {
        Mat4 m = new Mat4().rotate(90, 0, 0, 1);
        assertArrayEquals(new float[] {0, 1, 0, 1}, apply(m, 1, 0, 0), EPS);
    }

    @Test
    void rotateNormalizesTheAxis() {
        Mat4 a = new Mat4().rotate(30, 0, 2, 0);
        Mat4 b = new Mat4().rotate(30, 0, 1, 0);
        assertArrayEquals(b.m, a.m, EPS);
    }

    @Test
    void orthoMapsTheBoxToClipSpace() {
        Mat4 m = new Mat4().ortho(0, 320, 240, 0, 1000, 3000);
        assertArrayEquals(new float[] {-1, 1, -1, 1}, apply(m, 0, 0, -1000), EPS);
        assertArrayEquals(new float[] {1, -1, 1, 1}, apply(m, 320, 240, -3000), EPS);
    }

    @Test
    void mulMatchesSequentialOperations() {
        Mat4 expected = new Mat4().translate(5, 0, 0).rotate(45, 1, 0, 0);
        Mat4 rotation = new Mat4().rotate(45, 1, 0, 0);
        Mat4 actual = new Mat4().translate(5, 0, 0).mul(rotation.m.clone());
        assertArrayEquals(expected.m, actual.m, EPS);
    }

    @Test
    void normalMatrixUndoesNonUniformScale() {
        float[] n = new float[9];
        new Mat4().scale(2, 1, 1).normalMatrix(n);
        assertArrayEquals(new float[] {0.5f, 0, 0, 0, 1, 0, 0, 0, 1}, n, EPS);
    }

    @Test
    void normalMatrixOfSingularMatrixIsIdentity() {
        float[] n = new float[9];
        new Mat4().scale(0, 1, 1).normalMatrix(n);
        assertArrayEquals(new float[] {1, 0, 0, 0, 1, 0, 0, 0, 1}, n, EPS);
    }

    @Test
    void transformDirIgnoresTranslationAndNormalizes() {
        float[] out = new float[3];
        new Mat4().translate(9, 9, 9).scale(3, 3, 3).transformDirNormalized(0, 0, 1, out);
        assertArrayEquals(new float[] {0, 0, 1}, out, EPS);
    }

    @Test
    void stackPushCopiesAndPopRestores() {
        MatrixStack s = new MatrixStack();
        s.top().translate(1, 0, 0);
        s.push();
        s.top().translate(1, 0, 0);
        assertEquals(2f, s.top().m[12], EPS);
        s.pop();
        assertEquals(1f, s.top().m[12], EPS);
    }

    @Test
    void stackReusesMatricesAndNeverPopsTheLast() {
        MatrixStack s = new MatrixStack();
        s.push();
        Mat4 pushed = s.top();
        s.pop();
        s.pop();
        s.push();
        assertSame(pushed, s.top());
    }
}
