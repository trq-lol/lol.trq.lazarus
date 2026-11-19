/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;

/** Vertex data captured into a display list, uploaded once to its own buffer. */
final class StaticMesh {
    /** A recorded draw of a mesh; display lists free the mesh when deleted. */
    record Draw(StaticMesh mesh, int mode) implements Runnable {
        @Override
        public void run() {
            mesh.drawNow(mode);
        }
    }

    private final float[] pending;
    private final int count;
    private int vbo;

    StaticMesh(float[] data, int count) {
        this.pending = data;
        this.count = count;
    }

    Runnable draw(int mode) {
        return new Draw(this, mode);
    }

    void drawNow(int mode) {
        if (vbo == 0) {
            vbo = GL15C.glGenBuffers();
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, pending, GL15C.GL_STATIC_DRAW);
        }
        CoreGl.setUniformsForList();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo);
        CoreGl.bindInterleaved();
        CoreGl.draw(mode, 0, count);
    }

    void free() {
        if (vbo != 0) {
            GL15C.glDeleteBuffers(vbo);
            vbo = 0;
        }
    }

    static void unbind() {
        GL30C.glBindVertexArray(0);
    }
}
