/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

/**
 * LWJGL 2's OpenAL lifecycle and renamed AL10 methods. In LWJGL 2, {@code AL.create()} opened the default device and
 * made a context current; LWJGL 3 has no such method (it loads the library through {@code ALC.create()}), so callers of the LWJGL 2 form are redirected here.
 */
public final class Al2 {
    private static long device = NULL;
    private static long context = NULL;

    private Al2() {}

    /** Opens the default device, creates and makes current a context, and loads the AL capabilities. */
    public static synchronized void create() throws LWJGLException {
        if (context != NULL) {
            throw new IllegalStateException("OpenAL is already created");
        }
        try {
            ALC.create();
        } catch (IllegalStateException alreadyLoaded) {
            // The library was loaded earlier; that is fine.
        }
        device = alcOpenDevice((java.nio.ByteBuffer) null);
        if (device == NULL) {
            throw new LWJGLException("could not open the default OpenAL device");
        }
        ALCCapabilities alc = ALC.createCapabilities(device);
        context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL || !alcMakeContextCurrent(context)) {
            alcCloseDevice(device);
            device = NULL;
            throw new LWJGLException("could not create an OpenAL context");
        }
        AL.createCapabilities(alc);
    }

    /** {@return whether OpenAL is created} */
    public static synchronized boolean isCreated() {
        return context != NULL;
    }

    /** Destroys the context, closes the device and unloads the library. */
    public static synchronized void destroy() {
        if (context != NULL) {
            alcMakeContextCurrent(NULL);
            alcDestroyContext(context);
            context = NULL;
        }
        if (device != NULL) {
            alcCloseDevice(device);
            device = NULL;
        }
        ALC.destroy();
    }

    public static boolean AL_isCreated() {
        return isCreated();
    }

    public static void AL10_alListener(int pname, FloatBuffer value) {
        AL10.alListenerfv(pname, value);
    }

    public static void AL10_alSource(int source, int pname, FloatBuffer value) {
        AL10.alSourcefv(source, pname, value);
    }

    public static void AL10_alSourceStop(IntBuffer sources) {
        AL10.alSourceStopv(sources);
    }
}
