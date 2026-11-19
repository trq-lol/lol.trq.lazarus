/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * colour material, {@code glGet} of matrices, and push/pop of the emulated state. Draws use one uber shader.
 */
public final class CoreGl {
    // GL enums used here.
    static final int GL_MODELVIEW = 0x1700, GL_PROJECTION = 0x1701, GL_TEXTURE = 0x1702;
    static final int GL_TEXTURE_2D = 0x0DE1, GL_ALPHA_TEST = 0x0BC0, GL_FOG = 0x0B60, GL_LIGHTING = 0x0B50;
    static final int GL_LIGHT0 = 0x4000, GL_LIGHT1 = 0x4001, GL_COLOR_MATERIAL = 0x0B57;
    static final int GL_NORMALIZE = 0x0BA1, GL_RESCALE_NORMAL = 0x803A;
    static final int GL_TEXTURE_GEN_S = 0x0C60, GL_TEXTURE_GEN_Q = 0x0C63;
    static final int GL_VERTEX_ARRAY = 0x8074, GL_NORMAL_ARRAY = 0x8075, GL_COLOR_ARRAY = 0x8076;
    static final int GL_TEXTURE_COORD_ARRAY = 0x8078;
    static final int GL_QUADS = 0x0007, GL_QUAD_STRIP = 0x0008, GL_POLYGON = 0x0009;
    static final int GL_FOG_MODE = 0x0B65, GL_FOG_DENSITY = 0x0B62, GL_FOG_START = 0x0B63, GL_FOG_END = 0x0B64;
    static final int GL_FOG_COLOR = 0x0B66, GL_LINEAR = 0x2601, GL_EXP = 0x0800;
    static final int GL_POSITION = 0x1203, GL_DIFFUSE = 0x1201, GL_AMBIENT = 0x1200;
    static final int GL_LIGHT_MODEL_AMBIENT = 0x0B53;
    static final int GL_MODELVIEW_MATRIX = 0x0BA6, GL_PROJECTION_MATRIX = 0x0BA7, GL_TEXTURE_MATRIX = 0x0BA8;
    static final int GL_MATRIX_MODE = 0x0BA0, GL_COMPILE_AND_EXECUTE = 0x1301;
    static final int GL_FLOAT = 0x1406, GL_UNSIGNED_BYTE = 0x1401, GL_BYTE = 0x1400, GL_SHORT = 0x1402;
    static final int GL_UNSIGNED_SHORT = 0x1403, GL_INT = 0x1404, GL_DOUBLE = 0x140A;

    private static final int FLOATS = 14; // pos3 color4 uv0 2 uv1 2 normal3

    /** One client-side array: its format and where it points. */
    private static final class Array {
        boolean enabled;
        int size = 4;
        int type = GL_FLOAT;
        int stride;
        ByteBuffer memory;
        long offset;
        int buffer;
    }

    private static Shader shader;
    private static final java.util.concurrent.ConcurrentHashMap<Long, Integer> vaos =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static int streamVbo;
    private static int quadIbo;
    private static int quadIboQuads;

    private static int matrixMode = GL_MODELVIEW;
    private static final ArrayList<Mat4> modelView = stack();
    private static final ArrayList<Mat4> projection = stack();
    private static final ArrayList<Mat4> texture0 = stack();
    private static final ArrayList<Mat4> texture1 = stack();

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
    private static boolean lighting, light0, light1, colorMaterial;
    private static final float[] lightDir0 = {0, 0, 1}, lightDir1 = {0, 0, 1};
    private static final float[] lightDiffuse0 = {1, 1, 1}, lightDiffuse1 = {0, 0, 0};
    private static final float[] lightAmbient = {0.2f, 0.2f, 0.2f};

    private static final float[] color = {1, 1, 1, 1};
    private static final float[] normal = {0, 0, 1};
    private static final float[] uv0 = new float[2];
    private static final float[] uv1 = new float[2];

    private static final Array vertexArray = new Array();
    private static final Array colorArray = new Array();
    private static final Array normalArray = new Array();
    private static final Array[] texArrays = {new Array(), new Array()};

    private static int beginMode = -1;
    private static float[] immediate = new float[FLOATS * 1024];
    private static int immediateCount;

    private static final Map<Integer, List<Runnable>> lists = new HashMap<>();
    private static List<Runnable> recording;
    private static boolean executeWhileRecording;
    private static int nextList = 1;

    private static final ArrayList<Object[]> attribStack = new ArrayList<>();

    private CoreGl() {}

    private static ArrayList<Mat4> stack() {
        ArrayList<Mat4> s = new ArrayList<>();
        s.add(new Mat4());
        return s;
    }

    /** Sets up the emulator on the current core context. Called once after the window's context exists. */
    public static void init() {
        bindVao();
        if (Boolean.getBoolean("lazarus.gl.debug")) {
            Debug.install();
        }
        streamVbo = GL15C.glGenBuffers();
        quadIbo = GL15C.glGenBuffers();
        shader = new Shader();
    }

    /** Vertex arrays are not shared between contexts, so each context (the window, Forge's splash) gets its own. */
    private static void bindVao() {
        long context = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        GL30C.glBindVertexArray(vaos.computeIfAbsent(context, k -> GL30C.glGenVertexArrays()));
    }

    private static ArrayList<Mat4> current() {
        return switch (matrixMode) {
            case GL_PROJECTION -> projection;
            case GL_TEXTURE -> activeTexture == 1 ? texture1 : texture0;
            default -> modelView;
        };
    }

    private static Mat4 top() {
        ArrayList<Mat4> s = current();
        return s.get(s.size() - 1);
    }

    private static boolean record(Runnable op) {
        if (recording == null) {
            return false;
        }
        recording.add(op);
        return !executeWhileRecording;
    }

    // ---- matrices

    public static void glMatrixMode(int mode) {
        if (record(() -> glMatrixMode(mode))) return;
        matrixMode = mode;
    }

    public static void glLoadIdentity() {
        if (record(CoreGl::glLoadIdentity)) return;
        top().identity();
    }

    public static void glPushMatrix() {
        if (record(CoreGl::glPushMatrix)) return;
        ArrayList<Mat4> s = current();
        s.add(s.get(s.size() - 1).copy());
    }

    public static void glPopMatrix() {
        if (record(CoreGl::glPopMatrix)) return;
        ArrayList<Mat4> s = current();
        if (s.size() > 1) s.remove(s.size() - 1);
    }

    public static void glTranslatef(float x, float y, float z) {
        if (record(() -> glTranslatef(x, y, z))) return;
        top().translate(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        glTranslatef((float) x, (float) y, (float) z);
    }

    public static void glScalef(float x, float y, float z) {
        if (record(() -> glScalef(x, y, z))) return;
        top().scale(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        glScalef((float) x, (float) y, (float) z);
    }

    public static void glRotatef(float angle, float x, float y, float z) {
        if (record(() -> glRotatef(angle, x, y, z))) return;
        top().rotate(angle, x, y, z);
    }

    public static void glOrtho(double l, double r, double b, double t, double n, double f) {
        if (record(() -> glOrtho(l, r, b, t, n, f))) return;
        top().ortho(l, r, b, t, n, f);
    }

    public static void glMultMatrix(FloatBuffer m) {
        float[] a = new float[16];
        m.duplicate().get(a);
        if (record(() -> top().mul(a))) return;
        top().mul(a);
    }

    // ---- state

    public static void glEnable(int cap) {
        if (record(() -> glEnable(cap))) return;
        if (!setCap(cap, true)) GL11C.glEnable(cap);
    }

    public static void glDisable(int cap) {
        if (record(() -> glDisable(cap))) return;
        if (!setCap(cap, false)) GL11C.glDisable(cap);
    }

    private static boolean setCap(int cap, boolean on) {
        switch (cap) {
            case GL_TEXTURE_2D -> textureEnabled[Math.min(activeTexture, 1)] = on;
            case GL_ALPHA_TEST -> alphaTest = on;
            case GL_FOG -> fog = on;
            case GL_LIGHTING -> lighting = on;
            case GL_LIGHT0 -> light0 = on;
            case GL_LIGHT1 -> light1 = on;
            case GL_COLOR_MATERIAL -> colorMaterial = on;
            case GL_NORMALIZE, GL_RESCALE_NORMAL -> {}
            default -> {
                return cap >= GL_TEXTURE_GEN_S && cap <= GL_TEXTURE_GEN_Q;
            }
        }
        return true;
    }

    public static void glAlphaFunc(int func, float ref) {
        if (record(() -> glAlphaFunc(func, ref))) return;
        alphaFunc = func;
        alphaRef = ref;
    }

    public static void glShadeModel(int mode) {}

    public static void glColorMaterial(int face, int mode) {}

    public static void glTexEnvi(int target, int pname, int param) {}

    public static void glTexEnv(int target, int pname, FloatBuffer params) {}

    public static void glTexGeni(int coord, int pname, int param) {}

    public static void glTexGen(int coord, int pname, FloatBuffer params) {}

    public static void glLineWidth(float width) {
        // Wide lines are invalid in forward-compatible core contexts (macOS).
        GL11C.glLineWidth(1f);
    }

    public static void glFogi(int pname, int param) {
        if (record(() -> glFogi(pname, param))) return;
        if (pname == GL_FOG_MODE) fogMode = param;
    }

    public static void glFogf(int pname, float param) {
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
        if (pname == GL_FOG_COLOR) {
            float[] c = new float[4];
            params.duplicate().get(c);
            if (record(() -> System.arraycopy(c, 0, fogColor, 0, 4))) return;
            System.arraycopy(c, 0, fogColor, 0, 4);
        } else {
            glFogf(pname, params.get(params.position()));
        }
    }

    public static void glLight(int light, int pname, FloatBuffer params) {
        float[] v = new float[4];
        params.duplicate().get(v, 0, Math.min(4, params.remaining()));
        Runnable apply = () -> {
            boolean first = light == GL_LIGHT0;
            if (pname == GL_POSITION) {
                float[] d = modelView.get(modelView.size() - 1).transformDir(v[0], v[1], v[2]);
                float len = (float) Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                if (len > 0) for (int i = 0; i < 3; i++) d[i] /= len;
                System.arraycopy(d, 0, first ? lightDir0 : lightDir1, 0, 3);
            } else if (pname == GL_DIFFUSE) {
                System.arraycopy(v, 0, first ? lightDiffuse0 : lightDiffuse1, 0, 3);
            }
        };
        if (record(apply)) return;
        apply.run();
    }

    public static void glLightModel(int pname, FloatBuffer params) {
        if (pname != GL_LIGHT_MODEL_AMBIENT) return;
        float[] v = new float[4];
        params.duplicate().get(v, 0, Math.min(4, params.remaining()));
        if (record(() -> System.arraycopy(v, 0, lightAmbient, 0, 3))) return;
        System.arraycopy(v, 0, lightAmbient, 0, 3);
    }

    public static void glActiveTexture(int texture) {
        if (record(() -> glActiveTexture(texture))) return;
        activeTexture = texture - GL13C.GL_TEXTURE0;
        GL13C.glActiveTexture(texture);
    }

    public static void glActiveTextureARB(int texture) {
        glActiveTexture(texture);
    }

    public static void glClientActiveTexture(int texture) {
        clientActiveTexture = Math.min(texture - GL13C.GL_TEXTURE0, 1);
    }

    public static void glClientActiveTextureARB(int texture) {
        glClientActiveTexture(texture);
    }

    private static final int GL_CLAMP = 0x2900;

    public static void glTexParameteri(int target, int pname, int param) {
        GL11C.glTexParameteri(target, pname, param == GL_CLAMP ? 0x812F : param);
    }

    public static void glTexParameterf(int target, int pname, float param) {
        GL11C.glTexParameterf(target, pname, param == GL_CLAMP ? 0x812F : param);
    }

    /**
     * Frees a texture's storage but keeps its name. Minecraft deletes a texture and then binds the same name to
     * re-allocate it, which only compatibility profiles allow.
     */
    public static void glDeleteTextures(int texture) {
        if (texture == 0 || !GL11C.glIsTexture(texture)) {
            return;
        }
        int previous = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GL11C.glBindTexture(GL_TEXTURE_2D, texture);
        GL11C.glTexImage2D(
                GL_TEXTURE_2D, 0, GL11C.GL_RGBA, 1, 1, 0, GL11C.GL_RGBA, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
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
        if (immediate.length < (immediateCount + 1) * FLOATS) {
            immediate = java.util.Arrays.copyOf(immediate, immediate.length * 2);
        }
        int o = immediateCount * FLOATS;
        float[] v = immediate;
        v[o] = x;
        v[o + 1] = y;
        v[o + 2] = z;
        v[o + 3] = color[0];
        v[o + 4] = color[1];
        v[o + 5] = color[2];
        v[o + 6] = color[3];
        v[o + 7] = uv0[0];
        v[o + 8] = uv0[1];
        v[o + 9] = uv1[0];
        v[o + 10] = uv1[1];
        v[o + 11] = normal[0];
        v[o + 12] = normal[1];
        v[o + 13] = normal[2];
        immediateCount++;
    }

    public static void glEnd() {
        int mode = beginMode;
        beginMode = -1;
        if (immediateCount == 0) return;
        float[] data = java.util.Arrays.copyOf(immediate, immediateCount * FLOATS);
        int count = immediateCount;
        if (recording != null) {
            StaticMesh mesh = new StaticMesh(data, count);
            recording.add(mesh.draw(mode));
            if (!executeWhileRecording) return;
        }
        drawInterleaved(data, count, mode);
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
        a.buffer = memory == null ? GL11C.glGetInteger(GL15C.GL_ARRAY_BUFFER_BINDING) : 0;
    }

    public static void glVertexPointer(int size, int type, int stride, ByteBuffer pointer) {
        pointer(vertexArray, size, type, stride, pointer, 0);
    }

    public static void glVertexPointer(int size, int type, int stride, long offset) {
        pointer(vertexArray, size, type, stride, null, offset);
    }

    public static void glColorPointer(int size, int type, int stride, ByteBuffer pointer) {
        pointer(colorArray, size, type, stride, pointer, 0);
    }

    public static void glColorPointer(int size, int type, int stride, long offset) {
        pointer(colorArray, size, type, stride, null, offset);
    }

    public static void glTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {
        pointer(texArray(), size, type, stride, pointer, 0);
    }

    public static void glTexCoordPointer(int size, int type, int stride, long offset) {
        pointer(texArray(), size, type, stride, null, offset);
    }

    public static void glNormalPointer(int type, int stride, ByteBuffer pointer) {
        pointer(normalArray, 3, type, stride, pointer, 0);
    }

    public static void glNormalPointer(int type, int stride, long offset) {
        pointer(normalArray, 3, type, stride, null, offset);
    }

    public static void glEnableClientState(int array) {
        clientState(array, true);
    }

    public static void glDisableClientState(int array) {
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
        if (count <= 0) return;
        if (recording != null) {
            // Display lists capture the vertex data at compile time.
            float[] data = gather(first, count);
            StaticMesh mesh = new StaticMesh(data, count);
            recording.add(mesh.draw(mode));
            if (!executeWhileRecording) return;
        }
        if (!vertexArray.enabled) return;
        setUniforms();
        bindVao();
        int uploaded = 0;
        Array[] arrays = {vertexArray, colorArray, texArrays[0], texArrays[1], normalArray};
        int[] locations = {Shader.POS, Shader.COLOR, Shader.UV0, Shader.UV1, Shader.NORMAL};
        long streamBytes = 0;
        for (Array a : arrays) {
            if (a.enabled && a.memory != null) {
                streamBytes += align(span(a, first, count));
            }
        }
        if (streamBytes > 0) {
            GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, streamVbo);
            GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, streamBytes, GL15C.GL_STREAM_DRAW);
        }
        for (int i = 0; i < arrays.length; i++) {
            Array a = arrays[i];
            int loc = locations[i];
            if (!a.enabled) {
                GL20C.glDisableVertexAttribArray(loc);
                constant(loc);
                continue;
            }
            int stride = a.stride != 0 ? a.stride : a.size * typeSize(a.type);
            if (a.memory != null) {
                long bytes = span(a, first, count);
                ByteBuffer src = a.memory.duplicate();
                src.limit((int) Math.min(src.capacity(), a.offset + bytes));
                src.position((int) a.offset);
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, streamVbo);
                GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, uploaded, src);
                GL20C.glVertexAttribPointer(loc, a.size, a.type, normalized(loc, a.type), stride, uploaded);
                uploaded += (int) align(bytes);
            } else {
                GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, a.buffer);
                GL20C.glVertexAttribPointer(loc, a.size, a.type, normalized(loc, a.type), stride, a.offset);
            }
            GL20C.glEnableVertexAttribArray(loc);
        }
        draw(mode, first, count);
    }

    private static long span(Array a, int first, int count) {
        int stride = a.stride != 0 ? a.stride : a.size * typeSize(a.type);
        return (long) (first + count - 1) * stride + (long) a.size * typeSize(a.type);
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

    /** Reads enabled client arrays into the interleaved immediate-mode layout, for display list capture. */
    private static float[] gather(int first, int count) {
        float[] out = new float[count * FLOATS];
        for (int v = 0; v < count; v++) {
            int o = v * FLOATS;
            float[] c = colorArray.enabled ? read(colorArray, first + v, true) : color;
            float[] t0 = texArrays[0].enabled ? read(texArrays[0], first + v, false) : uv0;
            float[] t1 = texArrays[1].enabled ? read(texArrays[1], first + v, false) : uv1;
            float[] n = normalArray.enabled ? read(normalArray, first + v, true) : normal;
            float[] p = vertexArray.enabled ? read(vertexArray, first + v, false) : new float[3];
            out[o] = p[0];
            out[o + 1] = p.length > 1 ? p[1] : 0;
            out[o + 2] = vertexArray.size > 2 ? p[2] : 0;
            for (int i = 0; i < 4; i++) out[o + 3 + i] = i < c.length ? c[i] : 1f;
            out[o + 7] = t0[0];
            out[o + 8] = t0[1];
            out[o + 9] = t1[0];
            out[o + 10] = t1[1];
            out[o + 11] = n[0];
            out[o + 12] = n[1];
            out[o + 13] = n[2];
        }
        return out;
    }

    private static float[] read(Array a, int index, boolean normalize) {
        float[] r = new float[Math.max(a.size, 4)];
        if (a.size < 4) r[3] = 1f;
        if (a.memory == null) {
            return r; // buffer-sourced arrays in display lists are not captured
        }
        int stride = a.stride != 0 ? a.stride : a.size * typeSize(a.type);
        int base = (int) a.offset + index * stride;
        for (int i = 0; i < a.size; i++) {
            int at = base + i * typeSize(a.type);
            r[i] = switch (a.type) {
                case GL_UNSIGNED_BYTE -> normalize ? (a.memory.get(at) & 0xFF) / 255f : a.memory.get(at) & 0xFF;
                case GL_BYTE -> normalize ? Math.max(a.memory.get(at) / 127f, -1f) : a.memory.get(at);
                case GL_SHORT -> normalize ? a.memory.getShort(at) / 32767f : a.memory.getShort(at);
                case GL_INT -> a.memory.getInt(at);
                case GL_DOUBLE -> (float) a.memory.getDouble(at);
                default -> a.memory.getFloat(at);
            };
        }
        return r;
    }

    // ---- display lists

    public static int glGenLists(int range) {
        int base = nextList;
        nextList += range;
        return base;
    }

    public static void glNewList(int list, int mode) {
        recording = new ArrayList<>();
        executeWhileRecording = mode == GL_COMPILE_AND_EXECUTE;
        lists.put(list, recording);
    }

    public static void glEndList() {
        recording = null;
        executeWhileRecording = false;
    }

    public static void glCallList(int list) {
        List<Runnable> ops = lists.get(list);
        if (ops == null) return;
        if (record(() -> glCallList(list))) return;
        for (Runnable op : ops) op.run();
    }

    public static void glDeleteLists(int list, int range) {
        for (int i = 0; i < range; i++) {
            List<Runnable> ops = lists.remove(list + i);
            if (ops != null) {
                for (Runnable op : ops) {
                    if (op instanceof StaticMesh.Draw d) d.mesh().free();
                }
            }
        }
    }

    // ---- queries and attribute stack

    public static void glGetFloat(int pname, FloatBuffer params) {
        Mat4 m =
                switch (pname) {
                    case GL_MODELVIEW_MATRIX -> modelView.get(modelView.size() - 1);
                    case GL_PROJECTION_MATRIX -> projection.get(projection.size() - 1);
                    case GL_TEXTURE_MATRIX -> (activeTexture == 1 ? texture1 : texture0).get(0);
                    default -> null;
                };
        if (m == null) {
            GL11C.glGetFloatv(pname, params);
        } else {
            params.duplicate().put(m.m);
        }
    }

    public static float glGetFloat(int pname) {
        return GL11C.glGetFloat(pname);
    }

    public static void glGetInteger(int pname, IntBuffer params) {
        if (pname == GL_MATRIX_MODE) {
            params.put(params.position(), matrixMode);
        } else {
            GL11C.glGetIntegerv(pname, params);
        }
    }

    public static int glGetInteger(int pname) {
        return pname == GL_MATRIX_MODE ? matrixMode : GL11C.glGetInteger(pname);
    }

    public static void glPushAttrib(int mask) {
        attribStack.add(new Object[] {
            textureEnabled.clone(),
            alphaTest,
            alphaFunc,
            alphaRef,
            fog,
            lighting,
            light0,
            light1,
            colorMaterial,
            color.clone(),
            GL11C.glIsEnabled(GL11C.GL_BLEND),
            GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST),
            GL11C.glIsEnabled(GL11C.GL_CULL_FACE)
        });
    }

    public static void glPopAttrib() {
        if (attribStack.isEmpty()) return;
        Object[] s = attribStack.remove(attribStack.size() - 1);
        System.arraycopy((boolean[]) s[0], 0, textureEnabled, 0, 2);
        alphaTest = (boolean) s[1];
        alphaFunc = (int) s[2];
        alphaRef = (float) s[3];
        fog = (boolean) s[4];
        lighting = (boolean) s[5];
        light0 = (boolean) s[6];
        light1 = (boolean) s[7];
        colorMaterial = (boolean) s[8];
        System.arraycopy((float[]) s[9], 0, color, 0, 4);
        toggle(GL11C.GL_BLEND, (boolean) s[10]);
        toggle(GL11C.GL_DEPTH_TEST, (boolean) s[11]);
        toggle(GL11C.GL_CULL_FACE, (boolean) s[12]);
    }

    private static void toggle(int cap, boolean on) {
        if (on) GL11C.glEnable(cap);
        else GL11C.glDisable(cap);
    }

    // ---- drawing

    static void drawInterleaved(float[] data, int count, int mode) {
        setUniforms();
        bindVao();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, streamVbo);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, data, GL15C.GL_STREAM_DRAW);
        bindInterleaved();
        draw(mode, 0, count);
    }

    static void bindInterleaved() {
        int stride = FLOATS * 4;
        GL20C.glVertexAttribPointer(Shader.POS, 3, GL_FLOAT, false, stride, 0);
        GL20C.glVertexAttribPointer(Shader.COLOR, 4, GL_FLOAT, false, stride, 12);
        GL20C.glVertexAttribPointer(Shader.UV0, 2, GL_FLOAT, false, stride, 28);
        GL20C.glVertexAttribPointer(Shader.UV1, 2, GL_FLOAT, false, stride, 36);
        GL20C.glVertexAttribPointer(Shader.NORMAL, 3, GL_FLOAT, false, stride, 44);
        for (int loc = 0; loc <= 4; loc++) GL20C.glEnableVertexAttribArray(loc);
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

    static void setUniformsForList() {
        setUniforms();
        bindVao();
    }

    private static void setUniforms() {
        Shader s = shader;
        GL20C.glUseProgram(s.program);
        Mat4 mv = modelView.get(modelView.size() - 1);
        GL20C.glUniformMatrix4fv(s.modelView, false, mv.m);
        GL20C.glUniformMatrix4fv(s.projection, false, projection.get(projection.size() - 1).m);
        GL20C.glUniformMatrix4fv(s.tex0, false, texture0.get(texture0.size() - 1).m);
        GL20C.glUniformMatrix4fv(s.tex1, false, texture1.get(texture1.size() - 1).m);
        GL20C.glUniform1i(s.texture0, textureEnabled[0] ? 1 : 0);
        GL20C.glUniform1i(s.texture1, textureEnabled[1] ? 1 : 0);
        GL20C.glUniform1i(s.alphaFunc, alphaTest ? alphaFunc : 0);
        GL20C.glUniform1f(s.alphaRef, alphaRef);
        GL20C.glUniform1i(s.fogMode, fog ? fogMode : 0);
        GL20C.glUniform1f(s.fogStart, fogStart);
        GL20C.glUniform1f(s.fogEnd, fogEnd);
        GL20C.glUniform1f(s.fogDensity, fogDensity);
        GL20C.glUniform4f(s.fogColor, fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
        GL20C.glUniform1i(s.lighting, lighting ? 1 : 0);
        if (lighting) {
            GL20C.glUniformMatrix3fv(s.normalMat, false, mv.normalMatrix());
            GL20C.glUniform3f(s.lightDir0, lightDir0[0], lightDir0[1], lightDir0[2]);
            GL20C.glUniform3f(s.lightDir1, lightDir1[0], lightDir1[1], lightDir1[2]);
            GL20C.glUniform3f(s.lightDiffuse0, lightDiffuse0[0], lightDiffuse0[1], lightDiffuse0[2]);
            GL20C.glUniform3f(s.lightDiffuse1, lightDiffuse1[0], lightDiffuse1[1], lightDiffuse1[2]);
            GL20C.glUniform3f(s.lightAmbient, lightAmbient[0], lightAmbient[1], lightAmbient[2]);
            GL20C.glUniform1i(s.light0, light0 ? 1 : 0);
            GL20C.glUniform1i(s.light1, light1 ? 1 : 0);
        }
    }
}
