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

        int stride() {
            return stride != 0 ? stride : size * typeSize(type);
        }

        long span(int first, int count) {
            return (long) (first + count - 1) * stride() + (long) size * typeSize(type);
        }
    }

    private static Shader shader;
    private static StreamBuffer stream;
    private static int quadIbo;
    private static int quadIboQuads;

    private static final Map<Long, Integer> contextVaos = new ConcurrentHashMap<>();
    private static int boundVao = -1;
    private static int boundArrayBuffer;
    private static int gameArrayBuffer;
    private static int boundProgram;
    private static final float[] normalMatrix = new float[9];

    private static int matrixMode = GL_MODELVIEW;
    private static final MatrixStack modelView = new MatrixStack();
    private static final MatrixStack projection = new MatrixStack();
    private static final MatrixStack[] textureMatrix = {new MatrixStack(), new MatrixStack()};

    private static int activeTexture;
    private static int clientActiveTexture;
    private static final boolean[] textureEnabled = new boolean[2];
    private static boolean alphaTest;
    private static int alphaFunc = 0x0207;
    private static float alphaRef;
    private static boolean fog;
    private static int fogMode = 0x0800;
    private static float fogStart, fogEnd = 1f, fogDensity = 1f;
    private static final float[] fogColor = new float[4];
    private static boolean lighting, light0, light1;
    private static final float[] lightDir0 = {0, 0, 1}, lightDir1 = {0, 0, 1};
    private static final float[] lightDiffuse0 = {1, 1, 1}, lightDiffuse1 = {0, 0, 0};
    private static final float[] lightAmbient = {0.2f, 0.2f, 0.2f};
    private static final Map<Integer, Boolean> driverCaps = new HashMap<>();
    private static final int[] boundTextures = new int[32];
    private static boolean tintOn;
    private static final float[] tint = new float[4];

    private static final float[] color = {1, 1, 1, 1};
    private static final float[] normal = {0, 0, 1};
    private static final float[] uv0 = new float[2];
    private static final float[] uv1 = new float[2];

    private static final Array vertexArray = new Array();
    private static final Array colorArray = new Array();
    private static final Array normalArray = new Array();
    private static final Array[] texArrays = {new Array(), new Array()};
    private static final Array[] attribArrays = {vertexArray, colorArray, texArrays[0], texArrays[1], normalArray};

    private static int beginMode = -1;
    private static float[] immediate = new float[FLOATS * 1024];
    private static int immediateCount;

    /** Triangles from consecutive immediate-mode draws with unchanged state, drawn as one call at the next flush. */
    private static float[] batch = new float[FLOATS * 4096];

    private static int batchCount;
    private static final int BATCH_LIMIT = 65536;

    private static final Map<Integer, List<Runnable>> lists = new HashMap<>();
    private static List<Runnable> recording;
    private static boolean executeWhileRecording;
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

    private static final ArrayList<Attribs> attribStack = new ArrayList<>();

    private CoreGl() {}

    /** Sets up the emulator on the current core context. Called once after the window's context exists. */
    public static void init() {
        bindContextVertexArray();
        if (Boolean.getBoolean("lazarus.gl.debug")) {
            Debug.install();
        }
        stream = new StreamBuffer();
        restoreArrayBuffer();
        quadIbo = GL15C.glGenBuffers();
        shader = new Shader();
    }

    // ---- bindings

    /** Vertex arrays are not shared between contexts, so each context (the window, Forge's splash) gets its own. */
    static void bindContextVertexArray() {
        long context = GLFW.glfwGetCurrentContext();
        bindVertexArray(contextVaos.computeIfAbsent(context, k -> GL30C.glGenVertexArrays()));
    }

    static void bindVertexArray(int vao) {
        if (vao != boundVao) {
            GL30C.glBindVertexArray(vao);
            boundVao = vao;
        }
    }

    static void forgetVertexArray(int vao) {
        if (boundVao == vao) {
            boundVao = -1;
        }
    }

    static void bindArrayBuffer(int buffer) {
        if (buffer != boundArrayBuffer) {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, buffer);
            boundArrayBuffer = buffer;
        }
    }

    private static void restoreArrayBuffer() {
        bindArrayBuffer(gameArrayBuffer);
    }

    public static void glBindBuffer(int target, int buffer) {
        flush();
        if (target == GL15C.GL_ARRAY_BUFFER) {
            gameArrayBuffer = buffer;
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
        bindVertexArray(array);
    }

    public static void glUseProgram(int program) {
        flush();
        if (program != boundProgram) {
            GL20C.glUseProgram(program);
            boundProgram = program;
        }
    }

    public static void glUseProgramObjectARB(int program) {
        glUseProgram(program);
    }

    // ---- matrices

    private static MatrixStack current() {
        return switch (matrixMode) {
            case GL_PROJECTION -> projection;
            case GL_TEXTURE -> textureMatrix[Math.min(activeTexture, 1)];
            default -> modelView;
        };
    }

    private static Mat4 top() {
        return current().top();
    }

    private static boolean record(Runnable op) {
        if (recording == null) {
            return false;
        }
        recording.add(op);
        return !executeWhileRecording;
    }

    public static void glMatrixMode(int mode) {
        flush();
        if (record(() -> glMatrixMode(mode))) return;
        matrixMode = mode;
    }

    public static void glLoadIdentity() {
        flush();
        if (record(CoreGl::glLoadIdentity)) return;
        top().identity();
    }

    public static void glPushMatrix() {
        flush();
        if (record(CoreGl::glPushMatrix)) return;
        current().push();
    }

    public static void glPopMatrix() {
        flush();
        if (record(CoreGl::glPopMatrix)) return;
        current().pop();
    }

    public static void glTranslatef(float x, float y, float z) {
        flush();
        if (record(() -> glTranslatef(x, y, z))) return;
        top().translate(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        glTranslatef((float) x, (float) y, (float) z);
    }

    public static void glScalef(float x, float y, float z) {
        flush();
        if (record(() -> glScalef(x, y, z))) return;
        top().scale(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        glScalef((float) x, (float) y, (float) z);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        flush();
        if (record(() -> glRotatef(angle, x, y, z))) return;
        top().rotate(angle, x, y, z);
    }

    public static void glOrtho(double l, double r, double b, double t, double n, double f) {
        flush();
        if (record(() -> glOrtho(l, r, b, t, n, f))) return;
        top().ortho(l, r, b, t, n, f);
    }

    public static void glMultMatrix(FloatBuffer m) {
        flush();
        float[] a = new float[16];
        m.get(m.position(), a);
        if (record(() -> top().mul(a))) return;
        top().mul(a);
    }

    // ---- state

    public static void glEnable(int cap) {
        flush();
        if (record(() -> glEnable(cap))) return;
        if (!setCap(cap, true)) toggle(cap, true);
    }

    public static void glDisable(int cap) {
        flush();
        if (record(() -> glDisable(cap))) return;
        if (!setCap(cap, false)) toggle(cap, false);
    }

    /** Answered from tracked state, so it never stalls the driver. */
    public static boolean glIsEnabled(int cap) {
        flush();
        switch (cap) {
            case GL_TEXTURE_2D:
                return activeTexture < 2 && textureEnabled[activeTexture];
            case GL_ALPHA_TEST:
                return alphaTest;
            case GL_FOG:
                return fog;
            case GL_LIGHTING:
                return lighting;
            case GL_LIGHT0:
                return light0;
            case GL_LIGHT1:
                return light1;
            default:
                return driverCap(cap);
        }
    }

    private static boolean driverCap(int cap) {
        Boolean known = driverCaps.get(cap);
        if (known == null) {
            known = GL11C.glIsEnabled(cap);
            driverCaps.put(cap, known);
        }
        return known;
    }

    public static void glBindTexture(int target, int texture) {
        flush();
        if (record(() -> glBindTexture(target, texture))) return;
        if (target == GL_TEXTURE_2D && activeTexture < boundTextures.length) {
            boundTextures[activeTexture] = texture;
        }
        GL11C.glBindTexture(target, texture);
    }

    private static boolean setCap(int cap, boolean on) {
        switch (cap) {
            case GL_TEXTURE_2D -> {
                if (activeTexture < 2) textureEnabled[activeTexture] = on;
            }
            case GL_ALPHA_TEST -> alphaTest = on;
            case GL_FOG -> fog = on;
            case GL_LIGHTING -> lighting = on;
            case GL_LIGHT0 -> light0 = on;
            case GL_LIGHT1 -> light1 = on;
            case GL_COLOR_MATERIAL, GL_NORMALIZE, GL_RESCALE_NORMAL -> {}
            default -> {
                return cap >= GL_TEXTURE_GEN_S && cap <= GL_TEXTURE_GEN_Q;
            }
        }
        return true;
    }

    public static void glAlphaFunc(int func, float ref) {
        flush();
        if (record(() -> glAlphaFunc(func, ref))) return;
        alphaFunc = func;
        alphaRef = ref;
    }

    public static void glShadeModel(int mode) {}

    public static void glColorMaterial(int face, int mode) {}

    /**
     * Texture environments are emulated only as far as Minecraft uses them: the hurt and fire tint, which sets unit 1
     * to interpolate between the previous colour and a constant colour.
     */
    public static void glTexEnvi(int target, int pname, int param) {
        flush();
        if (record(() -> glTexEnvi(target, pname, param))) return;
        if (target != GL_TEXTURE_ENV || activeTexture != 1) return;
        if (pname == GL_COMBINE_RGB) {
            tintOn = param == GL_INTERPOLATE;
        } else if (pname == GL_TEXTURE_ENV_MODE && param == GL_MODULATE) {
            tintOn = false;
        }
    }

    public static void glTexEnv(int target, int pname, FloatBuffer params) {
        flush();
        if (target != GL_TEXTURE_ENV || pname != GL_TEXTURE_ENV_COLOR) return;
        float[] c = new float[4];
        params.get(params.position(), c);
        Runnable apply = () -> {
            if (activeTexture == 1) System.arraycopy(c, 0, tint, 0, 4);
        };
        if (record(apply)) return;
        apply.run();
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
        if (record(() -> glFogi(pname, param))) return;
        if (pname == GL_FOG_MODE) fogMode = param;
    }

    public static void glFogf(int pname, float param) {
        flush();
        if (record(() -> glFogf(pname, param))) return;
        switch (pname) {
            case GL_FOG_START -> fogStart = param;
            case GL_FOG_END -> fogEnd = param;
            case GL_FOG_DENSITY -> fogDensity = param;
            case GL_FOG_MODE -> fogMode = (int) param;
            default -> {}
        }
    }

    public static void glFog(int pname, FloatBuffer params) {
        flush();
        if (pname != GL_FOG_COLOR) {
            glFogf(pname, params.get(params.position()));
            return;
        }
        float[] c = new float[4];
        params.get(params.position(), c);
        if (record(() -> System.arraycopy(c, 0, fogColor, 0, 4))) return;
        System.arraycopy(c, 0, fogColor, 0, 4);
    }

    public static void glLight(int light, int pname, FloatBuffer params) {
        flush();
        float[] v = new float[4];
        params.get(params.position(), v, 0, Math.min(4, params.remaining()));
        Runnable apply = () -> {
            boolean first = light == GL_LIGHT0;
            if (pname == GL_POSITION) {
                modelView.top().transformDirNormalized(v[0], v[1], v[2], first ? lightDir0 : lightDir1);
            } else if (pname == GL_DIFFUSE) {
                System.arraycopy(v, 0, first ? lightDiffuse0 : lightDiffuse1, 0, 3);
            }
        };
        if (record(apply)) return;
        apply.run();
    }

    public static void glLightModel(int pname, FloatBuffer params) {
        flush();
        if (pname != GL_LIGHT_MODEL_AMBIENT) return;
        float[] v = new float[4];
        params.get(params.position(), v, 0, Math.min(4, params.remaining()));
        if (record(() -> System.arraycopy(v, 0, lightAmbient, 0, 3))) return;
        System.arraycopy(v, 0, lightAmbient, 0, 3);
    }

    public static void glActiveTexture(int texture) {
        flush();
        if (record(() -> glActiveTexture(texture))) return;
        activeTexture = texture - GL13C.GL_TEXTURE0;
        GL13C.glActiveTexture(texture);
    }

    public static void glActiveTextureARB(int texture) {
        glActiveTexture(texture);
    }

    /** Client state: applies immediately, even while a display list is being compiled. */
    public static void glClientActiveTexture(int texture) {
        flush();
        clientActiveTexture = Math.min(texture - GL13C.GL_TEXTURE0, 1);
    }

    public static void glClientActiveTextureARB(int texture) {
        glClientActiveTexture(texture);
    }

    public static void glTexParameteri(int target, int pname, int param) {
        flush();
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
        int previous = boundTextures[Math.min(activeTexture, boundTextures.length - 1)];
        GL11C.glBindTexture(GL_TEXTURE_2D, texture);
        GL11C.glTexImage2D(
                GL_TEXTURE_2D, 0, GL11C.GL_RGBA, 1, 1, 0, GL11C.GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL11C.glBindTexture(GL_TEXTURE_2D, previous);
    }

    // ---- current attributes

    public static void glColor4f(float r, float g, float b, float a) {
        if (beginMode < 0 && record(() -> glColor4f(r, g, b, a))) return;
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
    }

    public static void glColor3f(float r, float g, float b) {
        glColor4f(r, g, b, 1f);
    }

    public static void glColor3ub(byte r, byte g, byte b) {
        glColor4f((r & 0xFF) / 255f, (g & 0xFF) / 255f, (b & 0xFF) / 255f, 1f);
    }

    public static void glNormal3f(float x, float y, float z) {
        if (beginMode < 0 && record(() -> glNormal3f(x, y, z))) return;
        normal[0] = x;
        normal[1] = y;
        normal[2] = z;
    }

    public static void glTexCoord2f(float s, float t) {
        uv0[0] = s;
        uv0[1] = t;
    }

    public static void glMultiTexCoord2f(int target, float s, float t) {
        if (beginMode < 0 && record(() -> glMultiTexCoord2f(target, s, t))) return;
        float[] uv = target == GL13C.GL_TEXTURE1 ? uv1 : uv0;
        uv[0] = s;
        uv[1] = t;
    }

    public static void glMultiTexCoord2fARB(int target, float s, float t) {
        glMultiTexCoord2f(target, s, t);
    }

    // ---- immediate mode

    public static void glBegin(int mode) {
        beginMode = mode;
        immediateCount = 0;
    }

    public static void glVertex2f(float x, float y) {
        glVertex3f(x, y, 0f);
    }

    public static void glVertex3f(float x, float y, float z) {
        int o = immediateCount * FLOATS;
        if (immediate.length < o + FLOATS) {
            immediate = Arrays.copyOf(immediate, immediate.length * 2);
        }
        float[] v = immediate;
        v[o] = x;
        v[o + 1] = y;
        v[o + 2] = z;
        System.arraycopy(color, 0, v, o + 3, 4);
        v[o + 7] = uv0[0];
        v[o + 8] = uv0[1];
        v[o + 9] = uv1[0];
        v[o + 10] = uv1[1];
        System.arraycopy(normal, 0, v, o + 11, 3);
        immediateCount++;
    }

    public static void glEnd() {
        int mode = beginMode;
        beginMode = -1;
        int count = immediateCount;
        if (count == 0) return;
        if (recording != null) {
            recording.add(new StaticMesh(Arrays.copyOf(immediate, count * FLOATS), count).draw(mode));
            if (!executeWhileRecording) return;
        }
        if (!appendTriangles(mode, count)) {
            flush();
            drawImmediate(immediate, count, mode);
        }
    }

    /** Appends an immediate-mode primitive to the batch as triangles; returns false for lines and points. */
    private static boolean appendTriangles(int mode, int count) {
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
        if (batchCount + triangles * 3 > BATCH_LIMIT) {
            flush();
        }
        int needed = (batchCount + triangles * 3) * FLOATS;
        if (batch.length < needed) {
            batch = Arrays.copyOf(batch, Math.max(needed, batch.length * 2));
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
        System.arraycopy(immediate, vertex * FLOATS, batch, batchCount * FLOATS, FLOATS);
        batchCount++;
    }

    private static void drawImmediate(float[] data, int count, int mode) {
        prepareDraw();
        bindContextVertexArray();
        int floats = count * FLOATS;
        long at = stream.reserve(floats * 4L);
        stream.write(at, data, floats);
        bindInterleaved(at);
        draw(mode, 0, count);
        finishDraw();
    }

    /**
     * Draws the pending batch. Every emulated call that changes state flushes first, and the agent inserts a flush
     * before every OpenGL call the emulator does not handle, so the batch always draws with the state it was built in.
     */
    public static void flush() {
        int count = batchCount;
        if (count == 0) {
            return;
        }
        batchCount = 0;
        drawImmediate(batch, count, GL11C.GL_TRIANGLES);
    }

    // ---- client arrays

    private static Array texArray() {
        return texArrays[clientActiveTexture];
    }

    private static void pointer(Array a, int size, int type, int stride, ByteBuffer memory, long offset) {
        a.size = size;
        a.type = type;
        a.stride = stride;
        a.memory = memory;
        // LWJGL 2 takes the buffer's address at its current position; Minecraft moves one shared buffer between calls.
        a.offset = memory != null ? memory.position() : offset;
        a.buffer = memory == null ? gameArrayBuffer : 0;
    }

    public static void glVertexPointer(int size, int type, int stride, ByteBuffer pointer) {
        flush();
        pointer(vertexArray, size, type, stride, pointer, 0);
    }

    public static void glVertexPointer(int size, int type, int stride, long offset) {
        flush();
        pointer(vertexArray, size, type, stride, null, offset);
    }

    public static void glColorPointer(int size, int type, int stride, ByteBuffer pointer) {
        flush();
        pointer(colorArray, size, type, stride, pointer, 0);
    }

    public static void glColorPointer(int size, int type, int stride, long offset) {
        flush();
        pointer(colorArray, size, type, stride, null, offset);
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
        pointer(normalArray, 3, type, stride, pointer, 0);
    }

    public static void glNormalPointer(int type, int stride, long offset) {
        flush();
        pointer(normalArray, 3, type, stride, null, offset);
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
            case GL_VERTEX_ARRAY -> vertexArray.enabled = on;
            case GL_COLOR_ARRAY -> colorArray.enabled = on;
            case GL_NORMAL_ARRAY -> normalArray.enabled = on;
            case GL_TEXTURE_COORD_ARRAY -> texArray().enabled = on;
            default -> {}
        }
    }

    public static void glDrawArrays(int mode, int first, int count) {
        flush();
        if (count <= 0) return;
        if (recording != null) {
            recording.add(new StaticMesh(gather(first, count), count).draw(mode));
            if (!executeWhileRecording) return;
        }
        if (!vertexArray.enabled) return;
        prepareDraw();
        bindContextVertexArray();
        long streamBytes = 0;
        for (Array a : attribArrays) {
            if (a.enabled && a.memory != null) {
                streamBytes += align(a.span(first, count));
            }
        }
        long at = 0;
        if (streamBytes > 0) {
            at = stream.reserve(streamBytes);
        }
        for (int i = 0; i < attribArrays.length; i++) {
            Array a = attribArrays[i];
            int loc = LOCATIONS[i];
            if (!a.enabled) {
                GL20C.glDisableVertexAttribArray(loc);
                constant(loc);
                continue;
            }
            if (a.memory != null) {
                long bytes = a.span(first, count);
                if (a.memory.isDirect()) {
                    StreamBuffer.write(at, MemoryUtil.memAddress0(a.memory) + a.offset, bytes);
                } else {
                    ByteBuffer copy = MemoryUtil.memAlloc((int) bytes);
                    copy.put(0, a.memory, (int) a.offset, (int) bytes);
                    StreamBuffer.write(at, MemoryUtil.memAddress0(copy), bytes);
                    MemoryUtil.memFree(copy);
                }
                a.streamedAt = at;
                at += align(bytes);
            }
        }
        for (int i = 0; i < attribArrays.length; i++) {
            Array a = attribArrays[i];
            if (!a.enabled) continue;
            int loc = LOCATIONS[i];
            boolean norm = normalized(loc, a.type);
            if (a.memory != null) {
                bindArrayBuffer(stream.current());
                GL20C.glVertexAttribPointer(loc, a.size, a.type, norm, a.stride(), a.streamedAt);
            } else {
                bindArrayBuffer(a.buffer);
                GL20C.glVertexAttribPointer(loc, a.size, a.type, norm, a.stride(), a.offset);
            }
            GL20C.glEnableVertexAttribArray(loc);
        }
        draw(mode, first, count);
        finishDraw();
    }

    private static long align(long bytes) {
        return (bytes + 15) & ~15L;
    }

    private static boolean normalized(int location, int type) {
        return (location == Shader.COLOR || location == Shader.NORMAL) && type != GL_FLOAT;
    }

    private static void constant(int location) {
        switch (location) {
            case Shader.COLOR -> GL20C.glVertexAttrib4f(location, color[0], color[1], color[2], color[3]);
            case Shader.UV0 -> GL20C.glVertexAttrib4f(location, uv0[0], uv0[1], 0, 1);
            case Shader.UV1 -> GL20C.glVertexAttrib4f(location, uv1[0], uv1[1], 0, 1);
            case Shader.NORMAL -> GL20C.glVertexAttrib3f(location, normal[0], normal[1], normal[2]);
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
    private static float[] gather(int first, int count) {
        float[] out = new float[count * FLOATS];
        for (int v = 0; v < count; v++) {
            int o = v * FLOATS;
            int index = first + v;
            read(vertexArray, index, false, out, o, 3, null);
            read(colorArray, index, true, out, o + 3, 4, color);
            read(texArrays[0], index, false, out, o + 7, 2, uv0);
            read(texArrays[1], index, false, out, o + 9, 2, uv1);
            read(normalArray, index, true, out, o + 11, 3, normal);
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
        recording = new ArrayList<>();
        executeWhileRecording = mode == GL_COMPILE_AND_EXECUTE;
        lists.put(list, recording);
    }

    public static void glEndList() {
        flush();
        if (recording instanceof ArrayList<Runnable> ops) {
            ops.trimToSize();
        }
        recording = null;
        executeWhileRecording = false;
    }

    public static void glCallList(int list) {
        flush();
        List<Runnable> ops = lists.get(list);
        if (ops == null) return;
        if (record(() -> glCallList(list))) return;
        for (int i = 0, n = ops.size(); i < n; i++) {
            ops.get(i).run();
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
                    case GL_MODELVIEW_MATRIX -> modelView.top();
                    case GL_PROJECTION_MATRIX -> projection.top();
                    case GL_TEXTURE_MATRIX -> textureMatrix[Math.min(activeTexture, 1)].top();
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
            params.put(params.position(), matrixMode);
        } else {
            GL11C.glGetIntegerv(pname, params);
        }
    }

    public static int glGetInteger(int pname) {
        flush();
        return pname == GL_MATRIX_MODE ? matrixMode : GL11C.glGetInteger(pname);
    }

    public static void glPushAttrib(int mask) {
        flush();
        attribStack.add(new Attribs(
                textureEnabled.clone(),
                alphaTest,
                alphaFunc,
                alphaRef,
                fog,
                lighting,
                light0,
                light1,
                color.clone(),
                driverCap(GL11C.GL_BLEND),
                driverCap(GL11C.GL_DEPTH_TEST),
                driverCap(GL11C.GL_CULL_FACE)));
    }

    public static void glPopAttrib() {
        flush();
        if (attribStack.isEmpty()) return;
        Attribs s = attribStack.remove(attribStack.size() - 1);
        System.arraycopy(s.textures(), 0, textureEnabled, 0, 2);
        alphaTest = s.alphaTest();
        alphaFunc = s.alphaFunc();
        alphaRef = s.alphaRef();
        fog = s.fog();
        lighting = s.lighting();
        light0 = s.light0();
        light1 = s.light1();
        System.arraycopy(s.color(), 0, color, 0, 4);
        toggle(GL11C.GL_BLEND, s.blend());
        toggle(GL11C.GL_DEPTH_TEST, s.depthTest());
        toggle(GL11C.GL_CULL_FACE, s.cullFace());
    }

    private static void toggle(int cap, boolean on) {
        if (on) GL11C.glEnable(cap);
        else GL11C.glDisable(cap);
        driverCaps.put(cap, on);
    }

    // ---- drawing

    static void bindInterleaved() {
        bindInterleaved(0);
    }

    private static void bindInterleaved(long base) {
        int stride = FLOATS * 4;
        GL20C.glVertexAttribPointer(Shader.POS, 3, GL_FLOAT, false, stride, base);
        GL20C.glVertexAttribPointer(Shader.COLOR, 4, GL_FLOAT, false, stride, base + 12);
        GL20C.glVertexAttribPointer(Shader.UV0, 2, GL_FLOAT, false, stride, base + 28);
        GL20C.glVertexAttribPointer(Shader.UV1, 2, GL_FLOAT, false, stride, base + 36);
        GL20C.glVertexAttribPointer(Shader.NORMAL, 3, GL_FLOAT, false, stride, base + 44);
        for (int loc : LOCATIONS) GL20C.glEnableVertexAttribArray(loc);
    }

    static void draw(int mode, int first, int count) {
        switch (mode) {
            case GL_QUADS -> {
                int quads = count / 4;
                ensureQuadIndices(first / 4 + quads);
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
        Shader s = shader;
        glUseProgram(s.program);
        Uniforms u = s.uniforms;
        Mat4 mv = modelView.top();
        u.mat4(s.modelView, mv.m);
        u.mat4(s.projection, projection.top().m);
        u.i(s.texture0, textureEnabled[0] ? 1 : 0);
        u.i(s.texture1, textureEnabled[1] ? 1 : 0);
        if (textureEnabled[0]) u.mat4(s.tex0, textureMatrix[0].top().m);
        if (textureEnabled[1]) u.mat4(s.tex1, textureMatrix[1].top().m);
        u.i(s.alphaFunc, alphaTest ? alphaFunc : 0);
        if (alphaTest) u.f(s.alphaRef, alphaRef);
        u.i(s.fogMode, fog ? fogMode : 0);
        if (fog) {
            u.f(s.fogStart, fogStart);
            u.f(s.fogEnd, fogEnd);
            u.f(s.fogDensity, fogDensity);
            u.f4(s.fogColor, fogColor);
        }
        u.i(s.tintOn, tintOn ? 1 : 0);
        if (tintOn) u.f4(s.tint, tint);
        u.i(s.lighting, lighting ? 1 : 0);
        if (lighting) {
            mv.normalMatrix(normalMatrix);
            u.mat3(s.normalMat, normalMatrix);
            u.f3(s.lightDir0, lightDir0);
            u.f3(s.lightDir1, lightDir1);
            u.f3(s.lightDiffuse0, lightDiffuse0);
            u.f3(s.lightDiffuse1, lightDiffuse1);
            u.f3(s.lightAmbient, lightAmbient);
            u.i(s.light0, light0 ? 1 : 0);
            u.i(s.light1, light1 ? 1 : 0);
        }
    }

    /** Closes a frame; called before every buffer swap. */
    public static void endFrame() {
        flush();
        if (stream != null) {
            stream.endFrame();
        }
    }

    /** Returns the array buffer binding to what the game last bound, which it may rely on after a draw. */
    static void finishDraw() {
        restoreArrayBuffer();
    }
}
