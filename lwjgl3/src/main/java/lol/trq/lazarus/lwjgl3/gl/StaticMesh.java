/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
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
    /** Generic attributes captured with the list (shader mod data), stored after the interleaved vertices. */
    private float[] extra;

    private final int[] genericLocations;
    private final int[] genericSizes;

    private int vbo;
    private long extraBase;
    private int vao;
    private long context;

    StaticMesh(float[] data, int count, int perVertex, float[] extra, int[] genericLocations, int[] genericSizes) {
        this.pending = data;
        this.count = count;
        this.perVertex = perVertex;
        this.extra = extra;
        this.genericLocations = genericLocations;
        this.genericSizes = genericSizes;
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
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, (pending.length + extra.length) * 4L, GL15C.GL_STATIC_DRAW);
            GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, pending);
            if (extra.length > 0) {
                GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, pending.length * 4L, extra);
            }
            extraBase = pending.length * 4L;
            pending = null;
            extra = null;
        }
        if (vao == 0) {
            vao = GL30C.glGenVertexArrays();
            context = current;
            CoreGl.bindVertexArray(vao);
            CoreGl.bindArrayBuffer(vbo);
            CoreGl.bindInterleaved(0, perVertex);
            bindGenerics();
        } else if (context == current) {
            CoreGl.bindVertexArray(vao);
        } else {
            CoreGl.bindContextVertexArray();
            CoreGl.bindArrayBuffer(vbo);
            CoreGl.bindInterleaved(0, perVertex);
            bindGenerics();
        }
        CoreGl.constants(~perVertex);
        CoreGl.draw(mode, 0, count);
        CoreGl.finishDraw();
    }

    private void bindGenerics() {
        int stride = 0;
        for (int size : genericSizes) stride += size;
        long offset = extraBase;
        for (int g = 0; g < genericLocations.length; g++) {
            GL20C.glVertexAttribPointer(
                    genericLocations[g], genericSizes[g], GL11C.GL_FLOAT, false, stride * 4, offset);
            GL20C.glEnableVertexAttribArray(genericLocations[g]);
            offset += genericSizes[g] * 4L;
        }
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
