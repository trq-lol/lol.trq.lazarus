/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.nio.FloatBuffer;
import java.util.Locale;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

/**
 * Vertex memory for per-draw data, in one of two modes ({@code -Dlazarus.gl.stream=subdata|map}):
 *
 * <ul>
 *   <li>{@code subdata}: one ring, appended with {@code glBufferSubData} and orphaned when it wraps. Fastest on
 *       drivers that rename storage on their own (NVIDIA, Mesa).
 *   <li>{@code map}: one buffer per frame in flight, fenced at each buffer swap, written through an unsynchronized
 *       {@code glMapBufferRange}, so no upload call can make the driver wait.
 * </ul>
 *
 * <p>Apple's OpenGL waits for the GPU on {@code glBufferSubData} into a buffer that is still in use, so Apple
 * drivers default to {@code map}, which measured fastest there; everything else defaults to {@code subdata}.
 */
final class StreamBuffer {
    private static final int FRAMES = 3;
    private static final long INITIAL_CAPACITY = 4L << 20;
    private static final int MAP_ACCESS =
            GL30C.GL_MAP_WRITE_BIT | GL30C.GL_MAP_UNSYNCHRONIZED_BIT | GL30C.GL_MAP_INVALIDATE_RANGE_BIT;

    private final boolean map;
    private final int[] buffers;
    private final long[] fences = new long[FRAMES];
    private final long[] capacities;
    private int frame;
    private long head;
    private FloatBuffer staging = MemoryUtil.memAllocFloat(1 << 16);

    StreamBuffer() {
        map = useMap();
        buffers = new int[map ? FRAMES : 1];
        capacities = new long[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = GL15C.glGenBuffers();
            capacities[i] = INITIAL_CAPACITY;
            CoreGl.bindArrayBuffer(buffers[i]);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, capacities[i], GL15C.GL_STREAM_DRAW);
        }
    }

    private static boolean useMap() {
        String requested = System.getProperty("lazarus.gl.stream");
        if (requested != null) {
            return requested.equalsIgnoreCase("map");
        }
        String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
        return vendor != null && vendor.toLowerCase(Locale.ROOT).contains("apple");
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
        if (head + bytes > capacities[frame]) {
            while (capacities[frame] < bytes) {
                capacities[frame] = Math.max(INITIAL_CAPACITY, capacities[frame] * 2);
            }
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, capacities[frame], GL15C.GL_STREAM_DRAW);
            head = 0;
        }
        long at = head;
        head += bytes;
        return at;
    }

    /** Called before each buffer swap: in map mode, fences this frame's buffer and moves to the next free one. */
    void endFrame() {
        if (!map) {
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
    void write(long offset, long address, long bytes) {
        if (map) {
            long target = GL30C.nglMapBufferRange(GL15C.GL_ARRAY_BUFFER, offset, bytes, MAP_ACCESS);
            if (target != 0) {
                MemoryUtil.memCopy(address, target, bytes);
                GL15C.glUnmapBuffer(GL15C.GL_ARRAY_BUFFER);
                return;
            }
        }
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
