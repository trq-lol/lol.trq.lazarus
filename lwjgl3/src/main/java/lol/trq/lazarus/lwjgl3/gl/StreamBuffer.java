/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;

/**
 * A ring of vertex memory for per-draw data. Draws append with {@code glBufferSubData}, and the buffer is orphaned
 * when the ring wraps, so the driver never waits on data the GPU may still be reading. Mapping per draw measured
 * several times slower on desktop drivers.
 */
final class StreamBuffer {
    final int buffer;
    private long capacity = 4L << 20;
    private long head;
    private FloatBuffer staging = MemoryUtil.memAllocFloat(1 << 16);

    /** Creates the buffer; it is left bound to {@code GL_ARRAY_BUFFER}. */
    StreamBuffer() {
        buffer = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, capacity, GL15C.GL_STREAM_DRAW);
    }

    /**
     * Reserves {@code bytes} for one draw; the buffer must be bound to {@code GL_ARRAY_BUFFER}.
     *
     * @return the buffer offset of the reservation
     */
    long reserve(long bytes) {
        bytes = (bytes + 63) & ~63L;
        if (head + bytes > capacity) {
            while (capacity < bytes) {
                capacity *= 2;
            }
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, capacity, GL15C.GL_STREAM_DRAW);
            head = 0;
        }
        long at = head;
        head += bytes;
        return at;
    }

    /** Copies {@code bytes} from native memory at {@code address} into the buffer at {@code offset}. */
    static void write(long offset, long address, long bytes) {
        GL15C.nglBufferSubData(GL15C.GL_ARRAY_BUFFER, offset, bytes, address);
    }

    /** Copies the first {@code count} floats of {@code data} into the buffer at {@code offset}. */
    void write(long offset, float[] data, int count) {
        if (staging.capacity() < count) {
            MemoryUtil.memFree(staging);
            staging = MemoryUtil.memAllocFloat(Math.max(count, staging.capacity() * 2));
        }
        staging.clear().put(data, 0, count);
        write(offset, MemoryUtil.memAddress0(staging), count * 4L);
    }
}
