/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryUtil;

/**
 * The fixed-function OpenGL calls of Minecraft 1.8.9, Forge and OptiFine, emulated on a core profile context. The
 * agent redirects only these calls (identical names and descriptors) from game code to here; every other GL call
 * goes to the driver unchanged.
 *
 * <p>Emulated: immediate mode, client-side vertex arrays (from memory or a bound buffer), display lists, the
 * modelview/projection/texture matrix stacks, texture enables per unit, alpha test, fog, two directional lights with
 * colour material, {@code glGet} of matrices, and push/pop of the emulated state. Draws use one uber shader. Buffer
 * and program bindings are tracked here so draws never query the driver.
 */
public final class CoreGl {
    static final int GL_MODELVIEW = 0x1700, GL_PROJECTION = 0x1701, GL_TEXTURE = 0x1702;
    static final int GL_TEXTURE_2D = 0x0DE1, GL_ALPHA_TEST = 0x0BC0, GL_FOG = 0x0B60, GL_LIGHTING = 0x0B50;
    static final int GL_LIGHT0 = 0x4000, GL_LIGHT1 = 0x4001, GL_COLOR_MATERIAL = 0x0B57;
    static final int GL_NORMALIZE = 0x0BA1, GL_RESCALE_NORMAL = 0x803A;
    static final int GL_TEXTURE_GEN_S = 0x0C60, GL_TEXTURE_GEN_Q = 0x0C63;
    static final int GL_VERTEX_ARRAY = 0x8074, GL_NORMAL_ARRAY = 0x8075, GL_COLOR_ARRAY = 0x8076;
    static final int GL_TEXTURE_COORD_ARRAY = 0x8078;
    static final int GL_QUADS = 0x0007, GL_QUAD_STRIP = 0x0008, GL_POLYGON = 0x0009;
    static final int GL_FOG_MODE = 0x0B65, GL_FOG_DENSITY = 0x0B62, GL_FOG_START = 0x0B63, GL_FOG_END = 0x0B64;
    static final int GL_FOG_COLOR = 0x0B66;
    static final int GL_POSITION = 0x1203, GL_DIFFUSE = 0x1201;
    static final int GL_LIGHT_MODEL_AMBIENT = 0x0B53;
    static final int GL_MODELVIEW_MATRIX = 0x0BA6, GL_PROJECTION_MATRIX = 0x0BA7, GL_TEXTURE_MATRIX = 0x0BA8;
    static final int GL_MATRIX_MODE = 0x0BA0, GL_COMPILE_AND_EXECUTE = 0x1301;
    static final int GL_FLOAT = 0x1406, GL_UNSIGNED_BYTE = 0x1401, GL_BYTE = 0x1400, GL_SHORT = 0x1402;
    static final int GL_UNSIGNED_SHORT = 0x1403, GL_INT = 0x1404, GL_DOUBLE = 0x140A;
    private static final int GL_CLAMP = 0x2900, GL_CLAMP_TO_EDGE = 0x812F;
    private static final int GL_DEPTH_TEXTURE_MODE = 0x884B, GL_LUMINANCE = 0x1909, GL_INTENSITY = 0x8049;
    private static final int GL_ALPHA = 0x1906;
    private static final int GL33_TEXTURE_SWIZZLE_R = 0x8E42, GL33_TEXTURE_SWIZZLE_G = 0x8E43;
    private static final int GL33_TEXTURE_SWIZZLE_B = 0x8E44, GL33_TEXTURE_SWIZZLE_A = 0x8E45;
    private static final int GL_TEXTURE_ENV = 0x2300, GL_TEXTURE_ENV_MODE = 0x2200, GL_TEXTURE_ENV_COLOR = 0x2201;
    private static final int GL_COMBINE_RGB = 0x8571, GL_INTERPOLATE = 0x8575, GL_MODULATE = 0x2100;

    /** Interleaved layout of immediate mode and display lists: position 3, colour 4, uv0 2, uv1 2, normal 3. */
    private static final int FLOATS = 14;

    private static final int[] LOCATIONS = {Shader.POS, Shader.COLOR, Shader.UV0, Shader.UV1, Shader.NORMAL};

    /** One client-side array: its format and where it points. */
    private static final class Array {
        boolean enabled;
        int size = 4;
        int type = GL_FLOAT;
        int stride;
        ByteBuffer memory;
        long offset;
        int buffer;
        long streamedAt;
        boolean normalize;

        int stride() {
            return stride != 0 ? stride : size * typeSize(type);
        }

        long span(int first, int count) {
            return (long) (first + count - 1) * stride() + (long) size * typeSize(type);
        }

        /** Whether {@code count} vertices from {@code first} lie inside the client buffer, as LWJGL 2 checked. */
        boolean fits(int first, int count) {
            return memory == null || (offset >= 0 && stride >= 0 && offset + span(first, count) <= memory.capacity());
        }
    }

    private static Shader shader;
    private static int quadIbo;
    private static int quadIboQuads;

    private static final Map<Long, Integer> contextVaos = new ConcurrentHashMap<>();
    /** Whether a mod has bound a vertex array of its own; generic attribute calls then go straight to the driver. */
    private static final Map<Integer, Integer> shaderTypes = new ConcurrentHashMap<>();

    private static final Map<Integer, ProgramBuiltins> programBuiltins = new ConcurrentHashMap<>();
    private static final boolean DEBUG = Boolean.getBoolean("lazarus.gl.debug");

    /** Generic vertex attributes a shader mod sets up while the emulator's own vertex array is bound. */
    private static final int GENERIC_FIRST = LOCATIONS.length;

    private static final int BATCH_LIMIT = 65536;

    private static final Map<Integer, List<Runnable>> lists = new ConcurrentHashMap<>();
    private static int nextList = 1;

    private record Attribs(
            boolean[] textures,
            boolean alphaTest,
            int alphaFunc,
            float alphaRef,
            boolean fog,
            boolean lighting,
            boolean light0,
            boolean light1,
            float[] color,
            boolean blend,
            boolean depthTest,
            boolean cullFace) {}

    private CoreGl() {}

    /**
     * OpenGL state that belongs to one context, kept per thread: the cached bindings, the stream buffer and all
     * emulated fixed-function state. During loading Forge's splash thread draws on the window's context while the main
     * thread works on a shared one, so none of it can be shared between them.
     */
    private static final class Binding {
        final Thread owner = Thread.currentThread();
        StreamBuffer stream;
        int contextVao;
        boolean modVaoBound;
        int boundVao = -1;
        int boundArrayBuffer = -1;
        int gameArrayBuffer;
        int boundProgram = -1;
        int activeTexture;
        final Map<Integer, Boolean> driverCaps = new HashMap<>();
        /** 0 unknown, 1 off, 2 on, for the caps in {@link #capSlot}. */
        final byte[] capState = new byte[7];

        final int[] boundTextures = new int[32];
        final int[] viewport = new int[4];
        boolean viewportKnown;
        int genericApplied;

        // Emulated fixed-function state.
        float[] normalMatrix = new float[9];
        int matrixMode = GL_MODELVIEW;
        MatrixStack modelView = new MatrixStack();
        MatrixStack projection = new MatrixStack();
        MatrixStack[] textureMatrix = {new MatrixStack(), new MatrixStack()};
        int clientActiveTexture;
        boolean[] textureEnabled = new boolean[2];
        boolean alphaTest;
        int alphaFunc = 0x0207;
        float alphaRef;
        boolean fog;
        int fogMode = 0x0800;
        float fogStart, fogEnd = 1f, fogDensity = 1f;
        float[] fogColor = new float[4];
        boolean lighting, light0, light1;
        float[] lightDir0 = {0, 0, 1}, lightDir1 = {0, 0, 1};
        float[] lightDiffuse0 = {1, 1, 1}, lightDiffuse1 = {0, 0, 0};
        float[] lightAmbient = {0.2f, 0.2f, 0.2f};
        Mat4 modelViewProjection = new Mat4();
        boolean tintOn;
        float[] tint = new float[4];
        float[] scratch = new float[16];
        float[] color = {1, 1, 1, 1};
        float[] normal = {0, 0, 1};
        float[] uv0 = new float[2];
        float[] uv1 = new float[2];
        Array vertexArray = new Array();
        Array colorArray = new Array();
        Array normalArray = new Array();
        Array[] texArrays = {new Array(), new Array()};
        Array[] attribArrays = {vertexArray, colorArray, texArrays[0], texArrays[1], normalArray};
        Array[] generic = new Array[16];
        int genericEnabled;
        int beginMode = -1;
        float[] immediate = new float[FLOATS * 1024];
        int immediateCount;
        float[] batch = new float[FLOATS * 4096];
        int batchCount;
        List<Runnable> recording;
        boolean executeWhileRecording;
        ArrayList<Attribs> attribStack = new ArrayList<>();
        int beginAttributes;
        ByteBuffer raw = MemoryUtil.memAlloc(1 << 20);
        int rawCount;
        int rawMode;
        int rawStride;
        int[] rawLayout = new int[15];
        int[] layout = new int[15];
        float[] emitSource;
        int lastGameProgram;
        ProgramBuiltins lastBuiltins;
    }

    private static final ThreadLocal<Binding> BINDINGS = ThreadLocal.withInitial(Binding::new);
    private static Binding lastBinding;

    private static Binding b() {
        Binding last = lastBinding;
        if (last != null && last.owner == Thread.currentThread()) {
            return last;
        }
        Binding binding = BINDINGS.get();
        lastBinding = binding;
        return binding;
    }

    /** {@return this thread's stream buffer}, created on first use in the thread's context */
    private static StreamBuffer stream() {
        Binding binding = b();
        if (binding.stream == null) {
            binding.stream = new StreamBuffer();
        }
        return binding.stream;
    }

    /** Sets up the emulator on the current core context. Called once after the window's context exists. */
    public static void init() {
        bindContextVertexArray();
        if (Boolean.getBoolean("lazarus.gl.debug")) {
            Debug.install();
        }
        stream();
        restoreArrayBuffer();
        quadIbo = GL15C.glGenBuffers();
        shader = new Shader();
    }

    // ---- bindings

    /** Vertex arrays are not shared between contexts, so each context (the window, Forge's splash) gets its own. */
    static void bindContextVertexArray() {
        if (b().contextVao == 0) {
            b().contextVao = contextVaos.computeIfAbsent(GLFW.glfwGetCurrentContext(), k -> GL30C.glGenVertexArrays());
        }
        bindVertexArray(b().contextVao);
    }

    /**
     * Forgets the bindings cached for the previous context. Buffer, program, vertex array and texture bindings, enabled
     * capabilities and the viewport belong to each context, and Forge's splash screen renders on a second one.
     */
    public static void contextChanged() {
        b().contextVao = 0;
        b().modVaoBound = false;
        b().activeTexture = 0;
        b().boundVao = -1;
        b().boundArrayBuffer = -1;
        b().gameArrayBuffer = 0;
        b().boundProgram = -1;
        Arrays.fill(b().capState, (byte) 0);
        b().driverCaps.clear();
        Arrays.fill(b().boundTextures, 0);
        b().viewportKnown = false;
    }

    static void bindVertexArray(int vao) {
        if (vao != b().boundVao) {
            GL30C.glBindVertexArray(vao);
            b().boundVao = vao;
        }
    }

    static void forgetVertexArray(int vao) {
        if (b().boundVao == vao) {
            b().boundVao = -1;
        }
    }

    static void bindArrayBuffer(int buffer) {
        if (buffer != b().boundArrayBuffer) {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
            b().boundArrayBuffer = buffer;
        }
    }

    private static void restoreArrayBuffer() {
        bindArrayBuffer(b().gameArrayBuffer);
    }

    public static void glBindBuffer(int target, int buffer) {
        flush();
        if (target == GL15C.GL_ARRAY_BUFFER) {
            b().gameArrayBuffer = buffer;
            bindArrayBuffer(buffer);
        } else {
            GL15C.glBindBuffer(target, buffer);
        }
    }

    public static void glBindBufferARB(int target, int buffer) {
        glBindBuffer(target, buffer);
    }

    /** Mods that bind their own vertex arrays go through here, so the emulator's binding cache stays true. */
    public static void glBindVertexArray(int array) {
        flush();
        b().modVaoBound = array != 0;
        bindVertexArray(array);
    }

    public static void glUseProgram(int program) {
        flush();
        if (program != b().boundProgram) {
            GL20C.glUseProgram(program);
            b().boundProgram = program;
        }
    }

    public static void glUseProgramObjectARB(int program) {
        glUseProgram(program);
    }

    // ---- generic vertex attributes

    /** Whether generic attribute calls target the emulator's vertex array rather than one a mod bound itself. */
    private static boolean emulatesGeneric(int index) {
        return index >= GENERIC_FIRST && index < b().generic.length && !b().modVaoBound;
    }

    private static Array generic(int index) {
        Array a = b().generic[index];
        if (a == null) {
            a = b().generic[index] = new Array();
        }
        return a;
    }

    public static void glVertexAttribPointer(
            int index, int size, int type, boolean normalized, int stride, ByteBuffer pointer) {
        flush();
        if (!emulatesGeneric(index)) {
            GL20C.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
            return;
        }
        Array a = generic(index);
        pointer(a, size, type, stride, pointer, 0);
        a.normalize = normalized;
    }

    public static void glVertexAttribPointer(
            int index, int size, int type, boolean normalized, int stride, long offset) {
        flush();
        if (!emulatesGeneric(index)) {
            bindArrayBuffer(b().gameArrayBuffer);
            GL20C.glVertexAttribPointer(index, size, type, normalized, stride, offset);
            return;
        }
        Array a = generic(index);
        pointer(a, size, type, stride, null, offset);
        a.normalize = normalized;
    }

    public static void glEnableVertexAttribArray(int index) {
        flush();
        if (!emulatesGeneric(index)) {
            GL20C.glEnableVertexAttribArray(index);
            return;
        }
        generic(index).enabled = true;
        b().genericEnabled |= 1 << index;
    }

    public static void glDisableVertexAttribArray(int index) {
        flush();
        if (!emulatesGeneric(index)) {
            GL20C.glDisableVertexAttribArray(index);
            return;
        }
        generic(index).enabled = false;
        b().genericEnabled &= ~(1 << index);
    }

    /** Turns off generic attributes on the emulator's vertex array, for draws that do not use them. */
    private static void disableGenerics() {
        for (int bits = b().genericApplied; bits != 0; bits &= bits - 1) {
            GL20C.glDisableVertexAttribArray(Integer.numberOfTrailingZeros(bits));
        }
        b().genericApplied = 0;
    }

    // ---- the game's own shaders, translated from legacy GLSL

    /** Uniform locations of the fixed-function built-ins in one of the game's translated programs. */
    private record ProgramBuiltins(
            int modelView,
            int projection,
            int modelViewProjection,
            int texture0,
            int texture1,
            int normal,
            int fogColor,
            int fogDensity,
            int fogStart,
            int fogEnd,
            int fogScale,
            int alphaFunc,
            int alphaRef,
            Uniforms u) {
        static ProgramBuiltins of(int program) {
            int[] locations = {
                GL20C.glGetUniformLocation(program, "lz_ModelViewMatrix"),
                GL20C.glGetUniformLocation(program, "lz_ProjectionMatrix"),
                GL20C.glGetUniformLocation(program, "lz_ModelViewProjectionMatrix"),
                GL20C.glGetUniformLocation(program, "lz_TextureMatrix[0]"),
                GL20C.glGetUniformLocation(program, "lz_TextureMatrix[1]"),
                GL20C.glGetUniformLocation(program, "lz_NormalMatrix"),
                GL20C.glGetUniformLocation(program, "lz_Fog.color"),
                GL20C.glGetUniformLocation(program, "lz_Fog.density"),
                GL20C.glGetUniformLocation(program, "lz_Fog.start"),
                GL20C.glGetUniformLocation(program, "lz_Fog.end"),
                GL20C.glGetUniformLocation(program, "lz_Fog.scale"),
                GL20C.glGetUniformLocation(program, "lz_AlphaFunc"),
                GL20C.glGetUniformLocation(program, "lz_AlphaRef")
            };
            int max = 0;
            for (int l : locations) max = Math.max(max, l);
            return new ProgramBuiltins(
                    locations[0],
                    locations[1],
                    locations[2],
                    locations[3],
                    locations[4],
                    locations[5],
                    locations[6],
                    locations[7],
                    locations[8],
                    locations[9],
                    locations[10],
                    locations[11],
                    locations[12],
                    new Uniforms(max + 1));
        }
    }

    public static int glCreateShader(int type) {
        flush();
        int shader = GL20C.glCreateShader(type);
        shaderTypes.put(shader, type);
        return shader;
    }

    public static int glCreateShaderObjectARB(int type) {
        return glCreateShader(type);
    }

    public static void glShaderSource(int shader, ByteBuffer source) {
        flush();
        uploadSource(shader, MemoryUtil.memUTF8(source, source.remaining(), source.position()));
    }

    public static void glShaderSourceARB(int shader, ByteBuffer source) {
        glShaderSource(shader, source);
    }

    public static void glShaderSourceARB(int shader, CharSequence source) {
        flush();
        uploadSource(shader, source.toString());
    }

    private static void uploadSource(int shader, String source) {
        int end = source.indexOf('\0');
        String text = end >= 0 ? source.substring(0, end) : source;
        String translated = ShaderTranslator.translate(shaderTypes.getOrDefault(shader, 0), text);
        if (DEBUG) {
            ShaderDumps.remember(shader, text, translated);
        }
        GL20C.glShaderSource(shader, translated);
    }

    /** Finds uniforms the translator renamed, such as a sampler called {@code texture}, under their old names. */
    public static int glGetUniformLocationARB(int program, CharSequence name) {
        flush();
        String n = name.toString();
        return GL20C.glGetUniformLocation(program, n.equals("texture") ? ShaderTranslator.TEXTURE_RENAMED : n);
    }

    public static int glGetUniformLocation(int program, CharSequence name) {
        return glGetUniformLocationARB(program, name);
    }

    public static void glCompileShader(int shader) {
        flush();
        GL20C.glCompileShader(shader);
        if (DEBUG && GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) == GL11C.GL_FALSE) {
            ShaderDumps.compileFailed(shader, GL20C.glGetShaderInfoLog(shader));
        }
    }

    public static void glCompileShaderARB(int shader) {
        glCompileShader(shader);
    }

    /** Binds translated built-ins to the emulator's attribute locations and output 0, then links. */
    public static void glLinkProgram(int program) {
        flush();
        for (ShaderTranslator.Attribute attribute : ShaderTranslator.ATTRIBUTES) {
            GL20C.glBindAttribLocation(program, attribute.location(), attribute.name());
        }
        GL30C.glBindFragDataLocation(program, 0, "lz_FragColor");
        GL30C.glBindFragDataLocation(program, 0, "lz_FragData");
        GL20C.glLinkProgram(program);
        if (DEBUG && GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) == GL11C.GL_FALSE) {
            ShaderDumps.linkFailed(program, GL20C.glGetProgramInfoLog(program));
        }
        programBuiltins.remove(program);
        b().lastGameProgram = 0;
    }

    public static void glLinkProgramARB(int program) {
        glLinkProgram(program);
    }

    // ---- matrices

    private static MatrixStack current() {
        return switch (b().matrixMode) {
            case GL_PROJECTION -> b().projection;
            case GL_TEXTURE -> b().textureMatrix[Math.min(b().activeTexture, 1)];
            default -> b().modelView;
        };
    }

    private static Mat4 top() {
        return current().top();
    }

    private static boolean record(Runnable op) {
        if (b().recording == null) {
            return false;
        }
        b().recording.add(op);
        return !b().executeWhileRecording;
    }

    public static void glMatrixMode(int mode) {
        flush();
        if (b().recording != null && record(() -> glMatrixMode(mode))) return;
        b().matrixMode = mode;
    }

    public static void glLoadIdentity() {
        flush();
        if (b().recording != null && record(CoreGl::glLoadIdentity)) return;
        top().identity();
    }

    public static void glPushMatrix() {
        flush();
        if (b().recording != null && record(CoreGl::glPushMatrix)) return;
        current().push();
    }

    public static void glPopMatrix() {
        flush();
        if (b().recording != null && record(CoreGl::glPopMatrix)) return;
        current().pop();
    }

    public static void glTranslatef(float x, float y, float z) {
        flush();
        if (b().recording != null && record(() -> glTranslatef(x, y, z))) return;
        top().translate(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        glTranslatef((float) x, (float) y, (float) z);
    }

    public static void glScalef(float x, float y, float z) {
        flush();
        if (b().recording != null && record(() -> glScalef(x, y, z))) return;
        top().scale(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        glScalef((float) x, (float) y, (float) z);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        flush();
        if (b().recording != null && record(() -> glRotatef(angle, x, y, z))) return;
        top().rotate(angle, x, y, z);
    }

    public static void glOrtho(double l, double r, double b, double t, double n, double f) {
        flush();
        if (b().recording != null && record(() -> glOrtho(l, r, b, t, n, f))) return;
        top().ortho(l, r, b, t, n, f);
    }

    public static void glMultMatrix(FloatBuffer m) {
        flush();
        if (b().recording != null) {
            float[] a = new float[16];
            m.get(m.position(), a);
            if (record(() -> top().mul(a))) return;
        }
        m.get(m.position(), b().scratch, 0, 16);
        top().mul(b().scratch);
    }

    public static void glLoadMatrix(FloatBuffer m) {
        flush();
        if (b().recording != null) {
            float[] a = new float[16];
            m.get(m.position(), a);
            if (record(() -> System.arraycopy(a, 0, top().m, 0, 16))) return;
        }
        m.get(m.position(), top().m, 0, 16);
    }

    // ---- state

    public static void glEnable(int cap) {
        flush();
        if (b().recording != null && record(() -> glEnable(cap))) return;
        if (!setCap(cap, true)) toggle(cap, true);
    }

    public static void glDisable(int cap) {
        flush();
        if (b().recording != null && record(() -> glDisable(cap))) return;
        if (!setCap(cap, false)) toggle(cap, false);
    }

    public static void glViewport(int x, int y, int width, int height) {
        flush();
        b().viewport[0] = x;
        b().viewport[1] = y;
        b().viewport[2] = width;
        b().viewport[3] = height;
        b().viewportKnown = true;
        GL11C.glViewport(x, y, width, height);
    }

    /**
     * Always {@code GL_NO_ERROR}: Minecraft checks for errors several times a frame, and each check is a driver round
     * trip. With {@code lazarus.gl.debug} the real error flag is read, and the debug callback reports every error.
     */
    public static int glGetError() {
        flush();
        return DEBUG ? GL11C.glGetError() : GL11C.GL_NO_ERROR;
    }

    /** Answered from tracked state, so it never stalls the driver. */
    public static boolean glIsEnabled(int cap) {
        flush();
        switch (cap) {
            case GL_TEXTURE_2D:
                return b().activeTexture < 2 && b().textureEnabled[b().activeTexture];
            case GL_ALPHA_TEST:
                return b().alphaTest;
            case GL_FOG:
                return b().fog;
            case GL_LIGHTING:
                return b().lighting;
            case GL_LIGHT0:
                return b().light0;
            case GL_LIGHT1:
                return b().light1;
            default:
                return driverCap(cap);
        }
    }

    private static int capSlot(int cap) {
        return switch (cap) {
            case GL11C.GL_BLEND -> 0;
            case GL11C.GL_DEPTH_TEST -> 1;
            case GL11C.GL_CULL_FACE -> 2;
            case GL11C.GL_SCISSOR_TEST -> 3;
            case GL11C.GL_POLYGON_OFFSET_FILL -> 4;
            case GL11C.GL_STENCIL_TEST -> 5;
            case GL11C.GL_COLOR_LOGIC_OP -> 6;
            default -> -1;
        };
    }

    private static boolean driverCap(int cap) {
        int slot = capSlot(cap);
        if (slot >= 0) {
            if (b().capState[slot] == 0) b().capState[slot] = (byte) (GL11C.glIsEnabled(cap) ? 2 : 1);
            return b().capState[slot] == 2;
        }
        Boolean known = b().driverCaps.get(cap);
        if (known == null) {
            known = GL11C.glIsEnabled(cap);
            b().driverCaps.put(cap, known);
        }
        return known;
    }

    public static void glBindTexture(int target, int texture) {
        flush();
        if (b().recording != null && record(() -> glBindTexture(target, texture))) return;
        if (target == GL_TEXTURE_2D && b().activeTexture < b().boundTextures.length) {
            b().boundTextures[b().activeTexture] = texture;
        }
        GL11C.glBindTexture(target, texture);
    }

    private static boolean setCap(int cap, boolean on) {
        switch (cap) {
            case GL_TEXTURE_2D -> {
                if (b().activeTexture < 2) b().textureEnabled[b().activeTexture] = on;
            }
            case GL_ALPHA_TEST -> b().alphaTest = on;
            case GL_FOG -> b().fog = on;
            case GL_LIGHTING -> b().lighting = on;
            case GL_LIGHT0 -> b().light0 = on;
            case GL_LIGHT1 -> b().light1 = on;
            case GL_COLOR_MATERIAL, GL_NORMALIZE, GL_RESCALE_NORMAL -> {}
            default -> {
                return cap >= GL_TEXTURE_GEN_S && cap <= GL_TEXTURE_GEN_Q;
            }
        }
        return true;
    }

    public static void glAlphaFunc(int func, float ref) {
        flush();
        if (b().recording != null && record(() -> glAlphaFunc(func, ref))) return;
        b().alphaFunc = func;
        b().alphaRef = ref;
    }

    public static void glShadeModel(int mode) {}

    public static void glColorMaterial(int face, int mode) {}

    /**
     * Texture environments are emulated only as far as Minecraft uses them: the hurt and fire tint, which sets unit 1
     * to interpolate between the previous colour and a constant colour.
     */
    public static void glTexEnvi(int target, int pname, int param) {
        flush();
        if (b().recording != null && record(() -> glTexEnvi(target, pname, param))) return;
        if (target != GL_TEXTURE_ENV || b().activeTexture != 1) return;
        if (pname == GL_COMBINE_RGB) {
            b().tintOn = param == GL_INTERPOLATE;
        } else if (pname == GL_TEXTURE_ENV_MODE && param == GL_MODULATE) {
            b().tintOn = false;
        }
    }

    public static void glTexEnv(int target, int pname, FloatBuffer params) {
        flush();
        if (target != GL_TEXTURE_ENV || pname != GL_TEXTURE_ENV_COLOR) return;
        if (b().recording != null) {
            float[] c = new float[4];
            params.get(params.position(), c);
            if (record(() -> {
                if (b().activeTexture == 1) System.arraycopy(c, 0, b().tint, 0, 4);
            })) return;
        }
        if (b().activeTexture == 1) params.get(params.position(), b().tint, 0, 4);
    }

    public static void glTexGeni(int coord, int pname, int param) {}

    public static void glTexGen(int coord, int pname, FloatBuffer params) {}

    /** Wide lines are invalid in forward-compatible core contexts, which macOS requires. */
    public static void glLineWidth(float width) {
        flush();
        GL11C.glLineWidth(1f);
    }

    public static void glFogi(int pname, int param) {
        flush();
        if (b().recording != null && record(() -> glFogi(pname, param))) return;
        if (pname == GL_FOG_MODE) b().fogMode = param;
    }

    public static void glFogf(int pname, float param) {
        flush();
        if (b().recording != null && record(() -> glFogf(pname, param))) return;
        switch (pname) {
            case GL_FOG_START -> b().fogStart = param;
            case GL_FOG_END -> b().fogEnd = param;
            case GL_FOG_DENSITY -> b().fogDensity = param;
            case GL_FOG_MODE -> b().fogMode = (int) param;
            default -> {}
        }
    }

    public static void glFog(int pname, FloatBuffer params) {
        flush();
        if (pname != GL_FOG_COLOR) {
            glFogf(pname, params.get(params.position()));
            return;
        }
        if (b().recording != null) {
            float[] c = new float[4];
            params.get(params.position(), c);
            if (record(() -> System.arraycopy(c, 0, b().fogColor, 0, 4))) return;
        }
        params.get(params.position(), b().fogColor, 0, 4);
    }

    public static void glLight(int light, int pname, FloatBuffer params) {
        flush();
        int n = Math.min(4, params.remaining());
        if (b().recording != null) {
            float[] v = new float[4];
            params.get(params.position(), v, 0, n);
            if (record(() -> applyLight(light, pname, v))) return;
        }
        params.get(params.position(), b().scratch, 0, n);
        applyLight(light, pname, b().scratch);
    }

    private static void applyLight(int light, int pname, float[] v) {
        boolean first = light == GL_LIGHT0;
        if (pname == GL_POSITION) {
            b().modelView.top().transformDirNormalized(v[0], v[1], v[2], first ? b().lightDir0 : b().lightDir1);
        } else if (pname == GL_DIFFUSE) {
            System.arraycopy(v, 0, first ? b().lightDiffuse0 : b().lightDiffuse1, 0, 3);
        }
    }

    public static void glLightModel(int pname, FloatBuffer params) {
        flush();
        if (pname != GL_LIGHT_MODEL_AMBIENT) return;
        int n = Math.min(3, params.remaining());
        if (b().recording != null) {
            float[] v = new float[3];
            params.get(params.position(), v, 0, n);
            if (record(() -> System.arraycopy(v, 0, b().lightAmbient, 0, 3))) return;
        }
        params.get(params.position(), b().lightAmbient, 0, n);
    }

    public static void glActiveTexture(int texture) {
        flush();
        if (b().recording != null && record(() -> glActiveTexture(texture))) return;
        b().activeTexture = texture - GL13C.GL_TEXTURE0;
        GL13C.glActiveTexture(texture);
    }

    public static void glActiveTextureARB(int texture) {
        glActiveTexture(texture);
    }

    /** Client state: applies immediately, even while a display list is being compiled. */
    public static void glClientActiveTexture(int texture) {
        flush();
        b().clientActiveTexture = Math.min(texture - GL13C.GL_TEXTURE0, 1);
    }

    public static void glClientActiveTextureARB(int texture) {
        glClientActiveTexture(texture);
    }

    public static void glTexParameteri(int target, int pname, int param) {
        flush();
        if (pname == GL_DEPTH_TEXTURE_MODE) {
            // Core removed depth texture modes; a swizzle gives the compatibility result, depth in R, G and B.
            int rgb = param == GL_ALPHA ? GL11C.GL_ZERO : GL11C.GL_RED;
            int alpha = param == GL_LUMINANCE ? GL11C.GL_ONE : GL11C.GL_RED;
            GL11C.glTexParameteri(target, GL33_TEXTURE_SWIZZLE_R, rgb);
            GL11C.glTexParameteri(target, GL33_TEXTURE_SWIZZLE_G, rgb);
            GL11C.glTexParameteri(target, GL33_TEXTURE_SWIZZLE_B, rgb);
            GL11C.glTexParameteri(target, GL33_TEXTURE_SWIZZLE_A, alpha);
            return;
        }
        GL11C.glTexParameteri(target, pname, param == GL_CLAMP ? GL_CLAMP_TO_EDGE : param);
    }

    public static void glTexParameterf(int target, int pname, float param) {
        flush();
        GL11C.glTexParameterf(target, pname, param == GL_CLAMP ? GL_CLAMP_TO_EDGE : param);
    }

    /**
     * Frees a texture's storage but keeps its name. Minecraft deletes a texture and then binds the same name to
     * re-allocate it, which only compatibility profiles allow.
     */
    public static void glDeleteTextures(int texture) {
        flush();
        if (texture == 0) {
            return;
        }
        int previous = b().boundTextures[Math.min(b().activeTexture, b().boundTextures.length - 1)];
        GL11C.glBindTexture(GL_TEXTURE_2D, texture);
        GL11C.glTexImage2D(
                GL_TEXTURE_2D, 0, GL11C.GL_RGBA, 1, 1, 0, GL11C.GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11C.glBindTexture(GL_TEXTURE_2D, previous);
    }

    // ---- current attributes

    public static void glColor4f(float r, float g, float b, float a) {
        if (b().beginMode >= 0) b().beginAttributes |= 1 << 1;
        if (b().beginMode < 0 && b().rawCount > 0) flush();
        if (b().beginMode < 0 && b().recording != null && record(() -> glColor4f(r, g, b, a))) return;
        b().color[0] = r;
        b().color[1] = g;
        b().color[2] = b;
        b().color[3] = a;
    }

    public static void glColor3f(float r, float g, float b) {
        glColor4f(r, g, b, 1f);
    }

    public static void glColor3ub(byte r, byte g, byte b) {
        glColor4f((r & 0xFF) / 255f, (g & 0xFF) / 255f, (b & 0xFF) / 255f, 1f);
    }

    public static void glNormal3f(float x, float y, float z) {
        if (b().beginMode >= 0) b().beginAttributes |= 1 << 4;
        if (b().beginMode < 0 && b().rawCount > 0) flush();
        if (b().beginMode < 0 && b().recording != null && record(() -> glNormal3f(x, y, z))) return;
        b().normal[0] = x;
        b().normal[1] = y;
        b().normal[2] = z;
    }

    public static void glTexCoord2f(float s, float t) {
        if (b().beginMode >= 0) b().beginAttributes |= 1 << 2;
        if (b().beginMode < 0 && b().rawCount > 0) flush();
        if (b().beginMode < 0 && b().recording != null && record(() -> glTexCoord2f(s, t))) return;
        b().uv0[0] = s;
        b().uv0[1] = t;
    }

    public static void glMultiTexCoord2f(int target, float s, float t) {
        if (b().beginMode >= 0) b().beginAttributes |= target == GL13C.GL_TEXTURE1 ? 1 << 3 : 1 << 2;
        if (b().beginMode < 0 && b().rawCount > 0) flush();
        if (b().beginMode < 0 && b().recording != null && record(() -> glMultiTexCoord2f(target, s, t))) return;
        float[] uv = target == GL13C.GL_TEXTURE1 ? b().uv1 : b().uv0;
        uv[0] = s;
        uv[1] = t;
    }

    public static void glMultiTexCoord2fARB(int target, float s, float t) {
        glMultiTexCoord2f(target, s, t);
    }

    // ---- immediate mode

    private static final int ALL_ATTRIBUTES = (1 << 5) - 1;

    public static void glBegin(int mode) {
        b().beginMode = mode;
        b().immediateCount = 0;
        b().beginAttributes = 1;
    }

    public static void glVertex2f(float x, float y) {
        glVertex3f(x, y, 0f);
    }

    public static void glVertex3f(float x, float y, float z) {
        int o = b().immediateCount * FLOATS;
        if (b().immediate.length < o + FLOATS) {
            b().immediate = Arrays.copyOf(b().immediate, b().immediate.length * 2);
        }
        float[] v = b().immediate;
        v[o] = x;
        v[o + 1] = y;
        v[o + 2] = z;
        System.arraycopy(b().color, 0, v, o + 3, 4);
        v[o + 7] = b().uv0[0];
        v[o + 8] = b().uv0[1];
        v[o + 9] = b().uv1[0];
        v[o + 10] = b().uv1[1];
        System.arraycopy(b().normal, 0, v, o + 11, 3);
        b().immediateCount++;
    }

    public static void glEnd() {
        int mode = b().beginMode;
        b().beginMode = -1;
        int count = b().immediateCount;
        if (count == 0) return;
        if (b().recording != null) {
            b().recording
                    .add(new StaticMesh(
                                    Arrays.copyOf(b().immediate, count * FLOATS),
                                    count,
                                    b().beginAttributes,
                                    new float[0],
                                    new int[0],
                                    new int[0])
                            .draw(mode));
            if (!b().executeWhileRecording) return;
        }
        if (b().rawCount > 0) {
            flush();
        }
        if (!appendTriangles(b().immediate, mode, count)) {
            flush();
            drawImmediate(b().immediate, count, mode);
        }
    }

    // ---- raw batching of client-array draws

    private static final int RAW_LIMIT = 4 << 20;

    /**
     * Appends a client-array draw to the raw batch when every enabled array lives in one interleaved buffer, as
     * Minecraft's tessellator lays them out, and the draw has the same layout and primitive as the batch. Quads and
     * triangles only; the bytes are copied as they are.
     */
    private static boolean appendRaw(int mode, int first, int count) {
        if ((mode != GL_QUADS && mode != GL11C.GL_TRIANGLES)
                || !b().vertexArray.enabled
                || count > 65536
                || b().genericEnabled != 0) {
            return false;
        }
        ByteBuffer memory = b().vertexArray.memory;
        int stride = b().vertexArray.stride();
        if (memory == null) {
            return false;
        }
        long base = Long.MAX_VALUE;
        for (Array a : b().attribArrays) {
            if (!a.enabled) continue;
            if (a.memory != memory || a.stride() != stride) return false;
            base = Math.min(base, a.offset);
        }
        for (int i = 0; i < b().attribArrays.length; i++) {
            Array a = b().attribArrays[i];
            b().layout[i * 3] = a.enabled ? (int) (a.offset - base) : -1;
            b().layout[i * 3 + 1] = a.enabled ? a.size : 0;
            b().layout[i * 3 + 2] = a.enabled ? a.type : 0;
        }
        long bytes = (long) count * stride;
        long source = base + (long) first * stride;
        if (first < 0 || stride <= 0 || bytes > RAW_LIMIT || base < 0 || source + bytes > memory.capacity()) {
            return false;
        }
        if (b().rawCount > 0
                && (b().rawMode != mode
                        || b().rawStride != stride
                        || !Arrays.equals(b().rawLayout, b().layout)
                        || (long) b().rawCount * stride + bytes > RAW_LIMIT)) {
            flush();
        }
        if (b().batchCount > 0) {
            flush();
        }
        if (b().rawCount == 0) {
            b().rawMode = mode;
            b().rawStride = stride;
            System.arraycopy(b().layout, 0, b().rawLayout, 0, b().layout.length);
        }
        int at = Math.toIntExact((long) b().rawCount * stride);
        if (b().raw.capacity() < at + bytes) {
            b().raw = MemoryUtil.memRealloc(
                    b().raw, Math.toIntExact(Math.max(at + bytes, b().raw.capacity() * 2L)));
        }
        if (memory.isDirect()) {
            MemoryUtil.memCopy(MemoryUtil.memAddress0(memory) + source, MemoryUtil.memAddress0(b().raw) + at, bytes);
        } else {
            b().raw.put(at, memory, (int) source, (int) bytes);
        }
        b().rawCount += count;
        return true;
    }

    private static void flushRaw() {
        int count = b().rawCount;
        b().rawCount = 0;
        prepareDraw();
        bindContextVertexArray();
        disableGenerics();
        long bytes = (long) count * b().rawStride;
        long at = stream().reserve(bytes);
        stream().write(at, MemoryUtil.memAddress0(b().raw), bytes);
        for (int i = 0; i < LOCATIONS.length; i++) {
            int loc = LOCATIONS[i];
            int offset = b().rawLayout[i * 3];
            if (offset < 0) {
                GL20C.glDisableVertexAttribArray(loc);
                constant(loc);
                continue;
            }
            int type = b().rawLayout[i * 3 + 2];
            GL20C.glVertexAttribPointer(
                    loc, b().rawLayout[i * 3 + 1], type, normalized(loc, type), b().rawStride, at + offset);
            GL20C.glEnableVertexAttribArray(loc);
        }
        draw(b().rawMode, 0, count);
        finishDraw();
    }

    /** Appends a primitive to the batch as triangles; returns false for lines and points. */
    private static boolean appendTriangles(float[] source, int mode, int count) {
        int triangles =
                switch (mode) {
                    case GL11C.GL_TRIANGLES -> count / 3;
                    case GL_QUADS -> count / 4 * 2;
                    case GL11C.GL_TRIANGLE_STRIP, GL11C.GL_TRIANGLE_FAN, GL_QUAD_STRIP, GL_POLYGON -> count - 2;
                    default -> -1;
                };
        if (triangles < 0) {
            return false;
        }
        if (b().batchCount + triangles * 3 > BATCH_LIMIT) {
            flush();
        }
        b().emitSource = source;
        int needed = (b().batchCount + triangles * 3) * FLOATS;
        if (b().batch.length < needed) {
            b().batch = Arrays.copyOf(b().batch, Math.max(needed, b().batch.length * 2));
        }
        switch (mode) {
            case GL11C.GL_TRIANGLES -> {
                for (int v = 0; v < triangles * 3; v++) emit(v);
            }
            case GL_QUADS -> {
                for (int q = 0; q < count / 4; q++) {
                    int b = q * 4;
                    emit(b);
                    emit(b + 1);
                    emit(b + 2);
                    emit(b);
                    emit(b + 2);
                    emit(b + 3);
                }
            }
            case GL11C.GL_TRIANGLE_STRIP, GL_QUAD_STRIP -> {
                for (int v = 2; v < count; v++) {
                    boolean even = (v & 1) == 0;
                    emit(even ? v - 2 : v - 1);
                    emit(even ? v - 1 : v - 2);
                    emit(v);
                }
            }
            default -> {
                for (int v = 2; v < count; v++) {
                    emit(0);
                    emit(v - 1);
                    emit(v);
                }
            }
        }
        return true;
    }

    private static void emit(int vertex) {
        System.arraycopy(b().emitSource, vertex * FLOATS, b().batch, b().batchCount * FLOATS, FLOATS);
        b().batchCount++;
    }

    private static void drawImmediate(float[] data, int count, int mode) {
        prepareDraw();
        bindContextVertexArray();
        disableGenerics();
        int floats = count * FLOATS;
        long at = stream().reserve(floats * 4L);
        stream().write(at, data, floats);
        bindInterleaved(at);
        draw(mode, 0, count);
        finishDraw();
    }

    /**
     * Draws the pending batch. Every emulated call that changes state flushes first, and the agent inserts a flush
     * before every OpenGL call the emulator does not handle, so the batch always draws with the state it was built in.
     */
    public static void flush() {
        if (b().rawCount > 0) {
            flushRaw();
        }
        int count = b().batchCount;
        if (count == 0) {
            return;
        }
        b().batchCount = 0;
        drawImmediate(b().batch, count, GL11C.GL_TRIANGLES);
    }

    // ---- client arrays

    private static Array texArray() {
        return b().texArrays[b().clientActiveTexture];
    }

    private static void pointer(Array a, int size, int type, int stride, ByteBuffer memory, long offset) {
        a.size = size;
        a.type = type;
        a.stride = stride;
        a.memory = memory;
        // LWJGL 2 takes the buffer's address at its current position; Minecraft moves one shared buffer between calls.
        a.offset = memory != null ? memory.position() : offset;
        a.buffer = memory == null ? b().gameArrayBuffer : 0;
    }

    public static void glVertexPointer(int size, int type, int stride, ByteBuffer pointer) {
        flush();
        pointer(b().vertexArray, size, type, stride, pointer, 0);
    }

    public static void glVertexPointer(int size, int type, int stride, long offset) {
        flush();
        pointer(b().vertexArray, size, type, stride, null, offset);
    }

    public static void glColorPointer(int size, int type, int stride, ByteBuffer pointer) {
        flush();
        pointer(b().colorArray, size, type, stride, pointer, 0);
    }

    public static void glColorPointer(int size, int type, int stride, long offset) {
        flush();
        pointer(b().colorArray, size, type, stride, null, offset);
    }

    public static void glTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {
        flush();
        pointer(texArray(), size, type, stride, pointer, 0);
    }

    public static void glTexCoordPointer(int size, int type, int stride, long offset) {
        flush();
        pointer(texArray(), size, type, stride, null, offset);
    }

    public static void glNormalPointer(int type, int stride, ByteBuffer pointer) {
        flush();
        pointer(b().normalArray, 3, type, stride, pointer, 0);
    }

    public static void glNormalPointer(int type, int stride, long offset) {
        flush();
        pointer(b().normalArray, 3, type, stride, null, offset);
    }

    public static void glEnableClientState(int array) {
        flush();
        clientState(array, true);
    }

    public static void glDisableClientState(int array) {
        flush();
        clientState(array, false);
    }

    private static void clientState(int array, boolean on) {
        switch (array) {
            case GL_VERTEX_ARRAY -> b().vertexArray.enabled = on;
            case GL_COLOR_ARRAY -> b().colorArray.enabled = on;
            case GL_NORMAL_ARRAY -> b().normalArray.enabled = on;
            case GL_TEXTURE_COORD_ARRAY -> texArray().enabled = on;
            default -> {}
        }
    }

    public static void glDrawArrays(int mode, int first, int count) {
        if (count <= 0) return;
        if (first < 0) {
            throw new IllegalArgumentException("glDrawArrays first < 0: " + first);
        }
        for (Array a : b().attribArrays) {
            if (a.enabled && !a.fits(first, count)) {
                throw new IllegalArgumentException("glDrawArrays reads past the end of a client array");
            }
        }
        if (b().recording == null && appendRaw(mode, first, count)) {
            return;
        }
        flush();
        if (b().recording != null) {
            b().recording.add(capture(first, count).draw(mode));
            if (!b().executeWhileRecording) return;
        }
        if (!b().vertexArray.enabled) return;
        prepareDraw();
        bindContextVertexArray();
        long streamBytes = 0;
        for (Array a : b().attribArrays) {
            if (a.enabled && a.memory != null) {
                streamBytes += align(a.span(first, count));
            }
        }
        for (int bits = b().genericEnabled; bits != 0; bits &= bits - 1) {
            Array a = b().generic[Integer.numberOfTrailingZeros(bits)];
            if (!a.fits(first, count)) {
                throw new IllegalArgumentException("glDrawArrays reads past the end of a vertex attribute array");
            }
            if (a.memory != null) {
                streamBytes += align(a.span(first, count));
            }
        }
        long at = 0;
        if (streamBytes > 0) {
            at = stream().reserve(streamBytes);
        }
        for (int i = 0; i < b().attribArrays.length; i++) {
            Array a = b().attribArrays[i];
            int loc = LOCATIONS[i];
            if (!a.enabled) {
                GL20C.glDisableVertexAttribArray(loc);
                constant(loc);
                continue;
            }
            if (a.memory != null) {
                long bytes = a.span(first, count);
                if (a.memory.isDirect()) {
                    stream().write(at, MemoryUtil.memAddress0(a.memory) + a.offset, bytes);
                } else {
                    ByteBuffer copy = MemoryUtil.memAlloc((int) bytes);
                    copy.put(0, a.memory, (int) a.offset, (int) bytes);
                    stream().write(at, MemoryUtil.memAddress0(copy), bytes);
                    MemoryUtil.memFree(copy);
                }
                a.streamedAt = at;
                at += align(bytes);
            }
        }
        for (int bits = b().genericEnabled; bits != 0; bits &= bits - 1) {
            Array a = b().generic[Integer.numberOfTrailingZeros(bits)];
            if (a.memory != null) {
                long bytes = a.span(first, count);
                stream().write(at, MemoryUtil.memAddress0(a.memory) + a.offset, bytes);
                a.streamedAt = at;
                at += align(bytes);
            }
        }
        for (int i = 0; i < b().attribArrays.length; i++) {
            Array a = b().attribArrays[i];
            if (!a.enabled) continue;
            int loc = LOCATIONS[i];
            boolean norm = normalized(loc, a.type);
            if (a.memory != null) {
                bindArrayBuffer(stream().current());
                GL20C.glVertexAttribPointer(loc, a.size, a.type, norm, a.stride(), a.streamedAt);
            } else {
                bindArrayBuffer(a.buffer);
                GL20C.glVertexAttribPointer(loc, a.size, a.type, norm, a.stride(), a.offset);
            }
            GL20C.glEnableVertexAttribArray(loc);
        }
        for (int bits = b().genericApplied & ~b().genericEnabled; bits != 0; bits &= bits - 1) {
            GL20C.glDisableVertexAttribArray(Integer.numberOfTrailingZeros(bits));
        }
        for (int bits = b().genericEnabled; bits != 0; bits &= bits - 1) {
            int loc = Integer.numberOfTrailingZeros(bits);
            Array a = b().generic[loc];
            bindArrayBuffer(a.memory != null ? stream().current() : a.buffer);
            GL20C.glVertexAttribPointer(
                    loc, a.size, a.type, a.normalize, a.stride(), a.memory != null ? a.streamedAt : a.offset);
            GL20C.glEnableVertexAttribArray(loc);
        }
        b().genericApplied = b().genericEnabled;
        draw(mode, first, count);
        finishDraw();
    }

    private static int enabledArrays() {
        int mask = 0;
        for (int i = 0; i < b().attribArrays.length; i++) {
            if (b().attribArrays[i].enabled) mask |= 1 << i;
        }
        return mask | 1;
    }

    /** Sets the current value of every attribute in {@code mask} that is not read from an array. */
    static void constants(int mask) {
        for (int i = 0; i < LOCATIONS.length; i++) {
            if ((mask & (1 << i)) != 0) constant(LOCATIONS[i]);
        }
    }

    private static long align(long bytes) {
        return (bytes + 15) & ~15L;
    }

    private static boolean normalized(int location, int type) {
        return (location == Shader.COLOR || location == Shader.NORMAL) && type != GL_FLOAT;
    }

    private static void constant(int location) {
        switch (location) {
            case Shader.COLOR ->
                GL20C.glVertexAttrib4f(location, b().color[0], b().color[1], b().color[2], b().color[3]);
            case Shader.UV0 -> GL20C.glVertexAttrib4f(location, b().uv0[0], b().uv0[1], 0, 1);
            case Shader.UV1 -> GL20C.glVertexAttrib4f(location, b().uv1[0], b().uv1[1], 0, 1);
            case Shader.NORMAL -> GL20C.glVertexAttrib3f(location, b().normal[0], b().normal[1], b().normal[2]);
            default -> {}
        }
    }

    static int typeSize(int type) {
        return switch (type) {
            case GL_BYTE, GL_UNSIGNED_BYTE -> 1;
            case GL_SHORT, GL_UNSIGNED_SHORT -> 2;
            case GL_DOUBLE -> 8;
            default -> 4;
        };
    }

    /** Reads the enabled client arrays into the interleaved layout, for display list capture. */
    private static StaticMesh capture(int first, int count) {
        int generics = Integer.bitCount(b().genericEnabled);
        Array[] arrays = Arrays.copyOf(b().attribArrays, b().attribArrays.length + generics);
        int[] genericLocations = new int[generics];
        int[] genericSizes = new int[generics];
        int extraFloats = 0;
        for (int bits = b().genericEnabled, g = 0; bits != 0; bits &= bits - 1, g++) {
            int loc = Integer.numberOfTrailingZeros(bits);
            genericLocations[g] = loc;
            genericSizes[g] = b().generic[loc].size;
            extraFloats += b().generic[loc].size;
            arrays[b().attribArrays.length + g] = b().generic[loc];
        }
        ByteBuffer[] readBack = new ByteBuffer[arrays.length];
        ByteBuffer[] saved = new ByteBuffer[arrays.length];
        long[] savedOffsets = new long[arrays.length];
        for (int i = 0; i < arrays.length; i++) {
            Array a = arrays[i];
            if (a.enabled && a.memory == null && a.buffer != 0) {
                readBack[i] = MemoryUtil.memAlloc(Math.toIntExact(a.span(first, count)));
                bindArrayBuffer(a.buffer);
                GL15C.glGetBufferSubData(GL15C.GL_ARRAY_BUFFER, a.offset, readBack[i]);
                saved[i] = a.memory;
                savedOffsets[i] = a.offset;
                a.memory = readBack[i];
                a.offset = 0;
            }
        }
        try {
            float[] extra = new float[count * extraFloats];
            for (int v = 0, o = 0; v < count; v++) {
                for (int g = 0; g < generics; g++) {
                    Array a = arrays[b().attribArrays.length + g];
                    read(a, first + v, a.normalize, extra, o, a.size, null);
                    o += a.size;
                }
            }
            return new StaticMesh(
                    gatherClient(first, count), count, enabledArrays(), extra, genericLocations, genericSizes);
        } finally {
            for (int i = 0; i < arrays.length; i++) {
                if (readBack[i] != null) {
                    arrays[i].memory = saved[i];
                    arrays[i].offset = savedOffsets[i];
                    MemoryUtil.memFree(readBack[i]);
                }
            }
            restoreArrayBuffer();
        }
    }

    private static float[] gatherClient(int first, int count) {
        float[] out = new float[count * FLOATS];
        for (int v = 0; v < count; v++) {
            int o = v * FLOATS;
            int index = first + v;
            read(b().vertexArray, index, false, out, o, 3, null);
            read(b().colorArray, index, true, out, o + 3, 4, b().color);
            read(b().texArrays[0], index, false, out, o + 7, 2, b().uv0);
            read(b().texArrays[1], index, false, out, o + 9, 2, b().uv1);
            read(b().normalArray, index, true, out, o + 11, 3, b().normal);
        }
        return out;
    }

    /**
     * Writes {@code n} components of one vertex of an array into {@code out}; disabled or buffer-sourced arrays use
     * {@code fallback} (or zeros). Missing components default to 0, and alpha to 1.
     */
    private static void read(Array a, int index, boolean normalize, float[] out, int at, int n, float[] fallback) {
        if (!a.enabled || a.memory == null) {
            for (int i = 0; i < n; i++) {
                out[at + i] = fallback != null ? fallback[i] : 0f;
            }
            return;
        }
        ByteBuffer mem = a.memory;
        int size = typeSize(a.type);
        int base = (int) a.offset + index * a.stride();
        for (int i = 0; i < n; i++) {
            if (i >= a.size) {
                out[at + i] = i == 3 ? 1f : 0f;
                continue;
            }
            int p = base + i * size;
            out[at + i] = switch (a.type) {
                case GL_UNSIGNED_BYTE -> normalize ? (mem.get(p) & 0xFF) / 255f : mem.get(p) & 0xFF;
                case GL_BYTE -> normalize ? Math.max(mem.get(p) / 127f, -1f) : mem.get(p);
                case GL_SHORT -> normalize ? mem.getShort(p) / 32767f : mem.getShort(p);
                case GL_INT -> mem.getInt(p);
                case GL_DOUBLE -> (float) mem.getDouble(p);
                default -> mem.getFloat(p);
            };
        }
    }

    // ---- display lists

    public static int glGenLists(int range) {
        flush();
        int base = nextList;
        nextList += range;
        return base;
    }

    public static void glNewList(int list, int mode) {
        flush();
        freeList(list);
        b().recording = new ArrayList<>();
        b().executeWhileRecording = mode == GL_COMPILE_AND_EXECUTE;
        lists.put(list, b().recording);
    }

    public static void glEndList() {
        flush();
        if (b().recording instanceof ArrayList<Runnable> ops) {
            ops.trimToSize();
        }
        b().recording = null;
        b().executeWhileRecording = false;
    }

    public static void glCallList(int list) {
        flush();
        List<Runnable> ops = lists.get(list);
        if (ops == null) return;
        if (b().recording != null && record(() -> glCallList(list))) return;
        for (int i = 0, n = ops.size(); i < n; i++) {
            ops.get(i).run();
        }
    }

    /** Calls each list named in {@code lists}, from its position to its limit, as OptiFine's batched list calls do. */
    public static void glCallLists(IntBuffer lists) {
        flush();
        for (int i = lists.position(), end = lists.limit(); i < end; i++) {
            glCallList(lists.get(i));
        }
    }

    public static void glDeleteLists(int list, int range) {
        flush();
        for (int i = 0; i < range; i++) {
            freeList(list + i);
            lists.remove(list + i);
        }
    }

    private static void freeList(int list) {
        List<Runnable> ops = lists.get(list);
        if (ops != null) {
            for (Runnable op : ops) {
                if (op instanceof StaticMesh.Draw d) d.mesh().free();
            }
        }
    }

    // ---- queries and attribute stack

    public static void glGetFloat(int pname, FloatBuffer params) {
        flush();
        Mat4 m =
                switch (pname) {
                    case GL_MODELVIEW_MATRIX -> b().modelView.top();
                    case GL_PROJECTION_MATRIX -> b().projection.top();
                    case GL_TEXTURE_MATRIX ->
                        b().textureMatrix[Math.min(b().activeTexture, 1)].top();
                    default -> null;
                };
        if (m == null) {
            GL11C.glGetFloatv(pname, params);
        } else {
            params.put(params.position(), m.m);
        }
    }

    public static float glGetFloat(int pname) {
        flush();
        return GL11C.glGetFloat(pname);
    }

    public static void glGetInteger(int pname, IntBuffer params) {
        flush();
        if (pname == GL_MATRIX_MODE) {
            params.put(params.position(), b().matrixMode);
        } else if (pname == GL11C.GL_VIEWPORT && b().viewportKnown) {
            params.put(params.position(), b().viewport);
        } else {
            GL11C.glGetIntegerv(pname, params);
        }
    }

    public static int glGetInteger(int pname) {
        flush();
        return pname == GL_MATRIX_MODE ? b().matrixMode : GL11C.glGetInteger(pname);
    }

    public static void glPushAttrib(int mask) {
        flush();
        b().attribStack
                .add(new Attribs(
                        b().textureEnabled.clone(),
                        b().alphaTest,
                        b().alphaFunc,
                        b().alphaRef,
                        b().fog,
                        b().lighting,
                        b().light0,
                        b().light1,
                        b().color.clone(),
                        driverCap(GL11C.GL_BLEND),
                        driverCap(GL11C.GL_DEPTH_TEST),
                        driverCap(GL11C.GL_CULL_FACE)));
    }

    public static void glPopAttrib() {
        flush();
        if (b().attribStack.isEmpty()) return;
        Attribs s = b().attribStack.remove(b().attribStack.size() - 1);
        System.arraycopy(s.textures(), 0, b().textureEnabled, 0, 2);
        b().alphaTest = s.alphaTest();
        b().alphaFunc = s.alphaFunc();
        b().alphaRef = s.alphaRef();
        b().fog = s.fog();
        b().lighting = s.lighting();
        b().light0 = s.light0();
        b().light1 = s.light1();
        System.arraycopy(s.color(), 0, b().color, 0, 4);
        toggle(GL11C.GL_BLEND, s.blend());
        toggle(GL11C.GL_DEPTH_TEST, s.depthTest());
        toggle(GL11C.GL_CULL_FACE, s.cullFace());
    }

    private static void toggle(int cap, boolean on) {
        if (on) GL11C.glEnable(cap);
        else GL11C.glDisable(cap);
        int slot = capSlot(cap);
        if (slot >= 0) b().capState[slot] = (byte) (on ? 2 : 1);
        else b().driverCaps.put(cap, on);
    }

    // ---- drawing

    private static void bindInterleaved(long base) {
        bindInterleaved(base, ALL_ATTRIBUTES);
    }

    /** Points the attributes at the interleaved layout and enables those in {@code mask}; the rest stay constant. */
    static void bindInterleaved(long base, int mask) {
        int stride = FLOATS * 4;
        GL20C.glVertexAttribPointer(Shader.POS, 3, GL_FLOAT, false, stride, base);
        GL20C.glVertexAttribPointer(Shader.COLOR, 4, GL_FLOAT, false, stride, base + 12);
        GL20C.glVertexAttribPointer(Shader.UV0, 2, GL_FLOAT, false, stride, base + 28);
        GL20C.glVertexAttribPointer(Shader.UV1, 2, GL_FLOAT, false, stride, base + 36);
        GL20C.glVertexAttribPointer(Shader.NORMAL, 3, GL_FLOAT, false, stride, base + 44);
        for (int i = 0; i < LOCATIONS.length; i++) {
            if ((mask & (1 << i)) != 0) GL20C.glEnableVertexAttribArray(LOCATIONS[i]);
            else GL20C.glDisableVertexAttribArray(LOCATIONS[i]);
        }
    }

    static void draw(int mode, int first, int count) {
        switch (mode) {
            case GL_QUADS -> {
                int quads = count / 4;
                ensureQuadIndices(quads);
                GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, quadIbo);
                GL32C.glDrawElementsBaseVertex(GL11C.GL_TRIANGLES, quads * 6, GL11C.GL_UNSIGNED_INT, 0L, first);
            }
            case GL_QUAD_STRIP -> GL11C.glDrawArrays(GL11C.GL_TRIANGLE_STRIP, first, count);
            case GL_POLYGON -> GL11C.glDrawArrays(GL11C.GL_TRIANGLE_FAN, first, count);
            default -> GL11C.glDrawArrays(mode, first, count);
        }
    }

    private static void ensureQuadIndices(int quads) {
        if (quads <= quadIboQuads) return;
        int n = Math.max(quads, Math.max(16384, quadIboQuads * 2));
        IntBuffer idx = MemoryUtil.memAllocInt(n * 6);
        for (int q = 0; q < n; q++) {
            int b = q * 4;
            idx.put(b).put(b + 1).put(b + 2).put(b).put(b + 2).put(b + 3);
        }
        idx.flip();
        GL15C.glBindBuffer(GL15C.GL_ELEMENT_ARRAY_BUFFER, quadIbo);
        GL15C.glBufferData(GL15C.GL_ELEMENT_ARRAY_BUFFER, idx, GL15C.GL_STATIC_DRAW);
        MemoryUtil.memFree(idx);
        quadIboQuads = n;
    }

    /** Binds the fixed-function shader and uploads whatever state changed since the last draw. */
    static void prepareDraw() {
        if (b().boundProgram > 0 && b().boundProgram != shader.program) {
            prepareGameProgram(b().boundProgram);
            return;
        }
        Shader s = shader;
        glUseProgram(s.program);
        Uniforms u = s.uniforms;
        Mat4 mv = b().modelView.top();
        u.mat4(s.modelView, mv.m);
        u.mat4(s.projection, b().projection.top().m);
        u.i(s.texture0, b().textureEnabled[0] ? 1 : 0);
        u.i(s.texture1, b().textureEnabled[1] ? 1 : 0);
        if (b().textureEnabled[0]) u.mat4(s.tex0, b().textureMatrix[0].top().m);
        if (b().textureEnabled[1]) u.mat4(s.tex1, b().textureMatrix[1].top().m);
        u.i(s.alphaFunc, b().alphaTest ? b().alphaFunc : 0);
        if (b().alphaTest) u.f(s.alphaRef, b().alphaRef);
        u.i(s.fogMode, b().fog ? b().fogMode : 0);
        if (b().fog) {
            u.f(s.fogStart, b().fogStart);
            u.f(s.fogEnd, b().fogEnd);
            u.f(s.fogDensity, b().fogDensity);
            u.f4(s.fogColor, b().fogColor);
        }
        u.i(s.tintOn, b().tintOn ? 1 : 0);
        if (b().tintOn) u.f4(s.tint, b().tint);
        u.i(s.lighting, b().lighting ? 1 : 0);
        if (b().lighting) {
            mv.normalMatrix(b().normalMatrix);
            u.mat3(s.normalMat, b().normalMatrix);
            u.f3(s.lightDir0, b().lightDir0);
            u.f3(s.lightDir1, b().lightDir1);
            u.f3(s.lightDiffuse0, b().lightDiffuse0);
            u.f3(s.lightDiffuse1, b().lightDiffuse1);
            u.f3(s.lightAmbient, b().lightAmbient);
            u.i(s.light0, b().light0 ? 1 : 0);
            u.i(s.light1, b().light1 ? 1 : 0);
        }
    }

    // ---- state for GlState; names avoid the gl prefix so the agent never redirects them

    static float[] modelViewMatrix() {
        return b().modelView.top().m;
    }

    static float[] projectionMatrix() {
        return b().projection.top().m;
    }

    static int trackedProgram() {
        return b().boundProgram;
    }

    static int trackedVertexArray() {
        return b().boundVao;
    }

    static int trackedArrayBuffer() {
        return b().gameArrayBuffer;
    }

    static boolean trackedViewport(int[] out) {
        if (b().viewportKnown) System.arraycopy(b().viewport, 0, out, 0, 4);
        return b().viewportKnown;
    }

    /** Closes a frame; called before every buffer swap. */
    public static void endFrame() {
        flush();
        if (b().stream != null) {
            stream().endFrame();
        }
    }

    /** Feeds one of the game's translated programs the fixed-function built-ins it reads. */
    private static void prepareGameProgram(int program) {
        if (program != b().lastGameProgram) {
            b().lastBuiltins = programBuiltins.computeIfAbsent(program, ProgramBuiltins::of);
            b().lastGameProgram = program;
        }
        ProgramBuiltins b = b().lastBuiltins;
        Mat4 mv = b().modelView.top();
        Mat4 p = b().projection.top();
        b.u().mat4(b.modelView(), mv.m);
        b.u().mat4(b.projection(), p.m);
        if (b.modelViewProjection() >= 0) {
            b.u().mat4(b.modelViewProjection(), b().modelViewProjection.set(p).mul(mv.m).m);
        }
        b.u().mat4(b.texture0(), b().textureMatrix[0].top().m);
        b.u().mat4(b.texture1(), b().textureMatrix[1].top().m);
        if (b.normal() >= 0) {
            mv.normalMatrix(b().normalMatrix);
            b.u().mat3(b.normal(), b().normalMatrix);
        }
        b.u().f4(b.fogColor(), b().fogColor);
        b.u().f(b.fogDensity(), b().fogDensity);
        b.u().f(b.fogStart(), b().fogStart);
        b.u().f(b.fogEnd(), b().fogEnd);
        b.u().f(b.fogScale(), b().fogEnd != b().fogStart ? 1f / (b().fogEnd - b().fogStart) : 0f);
        b.u().i(b.alphaFunc(), b().alphaTest ? b().alphaFunc : 0);
        b.u().f(b.alphaRef(), b().alphaRef);
    }

    /** Returns the array buffer binding to what the game last bound, which it may rely on after a draw. */
    static void finishDraw() {
        restoreArrayBuffer();
    }
}
