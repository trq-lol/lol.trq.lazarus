/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.nio.FloatBuffer;
import java.util.Locale;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

/**
 * Vertex memory for per-draw data, in one of three modes ({@code -Dlazarus.gl.stream=subdata|orphan|frames}):
 *
 * <ul>
 *   <li>{@code subdata}: one ring, appended with {@code glBufferSubData} and orphaned when it wraps. Fastest on
 *       drivers that rename storage on their own (NVIDIA, Mesa).
 *   <li>{@code orphan}: fresh storage from {@code glBufferData} for every draw, so a write never waits on the GPU.
 *   <li>{@code frames}: one buffer per frame in flight, fenced at each buffer swap, appended with
 *       {@code glBufferSubData}.
 * </ul>
 *
 * <p>Apple's OpenGL waits for the GPU on {@code glBufferSubData} into a buffer that is still in use, so Apple
 * drivers default to {@code orphan}; everything else defaults to {@code subdata}.
 */
final class StreamBuffer {
    private static final int FRAMES = 3;

    private enum Mode {
        SUBDATA,
        ORPHAN,
        FRAMES
    }

    private final Mode mode;
    private final int[] buffers;
    private final long[] fences = new long[FRAMES];
    private final long[] capacities;
    private int frame;
    private long head;
    private FloatBuffer staging = MemoryUtil.memAllocFloat(1 << 16);

    StreamBuffer() {
        mode = chooseMode();
        buffers = new int[mode == Mode.FRAMES ? FRAMES : 1];
        capacities = new long[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = GL15C.glGenBuffers();
            capacities[i] = 4L << 20;
            CoreGl.bindArrayBuffer(buffers[i]);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, capacities[i], GL15C.GL_STREAM_DRAW);
        }
    }

    private static Mode chooseMode() {
        String requested = System.getProperty("lazarus.gl.stream");
        if (requested != null) {
            return Mode.valueOf(requested.toUpperCase(Locale.ROOT));
        }
        String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
        return vendor != null && vendor.toLowerCase(Locale.ROOT).contains("apple") ? Mode.ORPHAN : Mode.SUBDATA;
    }

    /** {@return the buffer that the last {@link #reserve} bound} */
    int current() {
        return buffers[frame];
    }

    /**
     * Binds the stream buffer and reserves {@code bytes} in it for one draw.
     *
     * @return the buffer offset of the reservation
     */
    long reserve(long bytes) {
        bytes = (bytes + 63) & ~63L;
        CoreGl.bindArrayBuffer(buffers[frame]);
        if (mode == Mode.ORPHAN) {
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, bytes, GL15C.GL_STREAM_DRAW);
            return 0;
        }
        if (head + bytes > capacities[frame]) {
            while (capacities[frame] < bytes) {
                capacities[frame] *= 2;
            }
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, capacities[frame], GL15C.GL_STREAM_DRAW);
            head = 0;
        }
        long at = head;
        head += bytes;
        return at;
    }

    /** Called before each buffer swap: fences this frame's buffer and moves to the next free one. */
    void endFrame() {
        if (mode != Mode.FRAMES) {
            return;
        }
        fences[frame] = GL32C.glFenceSync(GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        frame = (frame + 1) % FRAMES;
        head = 0;
        if (fences[frame] != 0) {
            GL32C.glClientWaitSync(fences[frame], GL32C.GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000_000L);
            GL32C.glDeleteSync(fences[frame]);
            fences[frame] = 0;
        }
    }

    /** Copies {@code bytes} from native memory at {@code address} into the bound buffer at {@code offset}. */
    static void write(long offset, long address, long bytes) {
        GL15C.nglBufferSubData(GL15C.GL_ARRAY_BUFFER, offset, bytes, address);
    }

    /** Copies the first {@code count} floats of {@code data} into the bound buffer at {@code offset}. */
    void write(long offset, float[] data, int count) {
        if (staging.capacity() < count) {
            MemoryUtil.memFree(staging);
            staging = MemoryUtil.memAllocFloat(Math.max(count, staging.capacity() * 2));
        }
        staging.clear().put(data, 0, count);
        write(offset, MemoryUtil.memAddress0(staging), count * 4L);
    }
}
