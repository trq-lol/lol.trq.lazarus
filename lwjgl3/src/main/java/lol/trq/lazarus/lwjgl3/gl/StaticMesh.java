/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;

/**
 * Vertex data captured into a display list. It is uploaded once, with its own vertex array object in the context
 * that first draws it, so replaying it is one bind and one draw call.
 */
final class StaticMesh {
    /** A recorded draw of a mesh; display lists free the mesh when deleted. */
    record Draw(StaticMesh mesh, int mode) implements Runnable {
        @Override
        public void run() {
            mesh.drawNow(mode);
        }
    }

    private float[] pending;
    private final int count;
    /** Bit per emulator attribute location that the list supplied per vertex; the rest come from current state. */
    private final int perVertex;

    private int vbo;
    private int vao;
    private long context;

    StaticMesh(float[] data, int count, int perVertex) {
        this.pending = data;
        this.count = count;
        this.perVertex = perVertex;
    }

    Runnable draw(int mode) {
        return new Draw(this, mode);
    }

    void drawNow(int mode) {
        CoreGl.prepareDraw();
        long current = GLFW.glfwGetCurrentContext();
        if (vbo == 0) {
            vbo = GL15C.glGenBuffers();
            CoreGl.bindArrayBuffer(vbo);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, pending, GL15C.GL_STATIC_DRAW);
            pending = null;
        }
        if (vao == 0) {
            vao = GL30C.glGenVertexArrays();
            context = current;
            CoreGl.bindVertexArray(vao);
            CoreGl.bindArrayBuffer(vbo);
            CoreGl.bindInterleaved(0, perVertex);
        } else if (context == current) {
            CoreGl.bindVertexArray(vao);
        } else {
            CoreGl.bindContextVertexArray();
            CoreGl.bindArrayBuffer(vbo);
            CoreGl.bindInterleaved(0, perVertex);
        }
        CoreGl.constants(~perVertex);
        CoreGl.draw(mode, 0, count);
        CoreGl.finishDraw();
    }

    void free() {
        if (vbo != 0) {
            GL15C.glDeleteBuffers(vbo);
            vbo = 0;
        }
        if (vao != 0 && GLFW.glfwGetCurrentContext() == context) {
            CoreGl.forgetVertexArray(vao);
            GL30C.glDeleteVertexArrays(vao);
            vao = 0;
        }
    }
}
