/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites legacy GLSL (1.10 to 1.50 compatibility, as written for Minecraft 1.8.9, Forge mods and OptiFine) into
 * GLSL 3.30 core. Fixed-function built-ins become {@code lz_*} attributes, varyings and uniforms that the emulator
 * binds and uploads; {@code attribute}/{@code varying} become {@code in}/{@code out}; the old texture functions
 * become {@code texture}. Shaders that already target GLSL 3.30, or 1.50 core, pass through unchanged.
 */
final class ShaderTranslator {
    static final int VERTEX = 0x8B31;
    static final int FRAGMENT = 0x8B30;

    /**
     * Legacy shaders, OptiFine packs among them, often name a sampler {@code texture}, which GLSL 1.30 made a built-in
     * function; such identifiers are renamed to this.
     */
    static final String TEXTURE_RENAMED = "lz_texture";

    /** A vertex input of translated shaders and the emulator's attribute location for it. */
    record Attribute(String name, int location) {}

    static final Attribute[] ATTRIBUTES = {
        new Attribute("lz_Vertex", Shader.POS),
        new Attribute("Position", Shader.POS),
        new Attribute("lz_Color", Shader.COLOR),
        new Attribute("lz_MultiTexCoord0", Shader.UV0),
        new Attribute("lz_MultiTexCoord1", Shader.UV1),
        new Attribute("lz_Normal", Shader.NORMAL)
    };

    private static final Pattern VERSION =
            Pattern.compile("^[ \\t]*#version[ \\t]+(\\d+)([ \\t]+\\w+)?[^\\n]*$", Pattern.MULTILINE);
    private static final Pattern EXTENSION = Pattern.compile("^[ \\t]*#extension[^\\n]*$", Pattern.MULTILINE);

    private ShaderTranslator() {}

    /**
     * Translates one shader stage.
     *
     * @param type {@link #VERTEX} or {@link #FRAGMENT}; any other stage is returned unchanged
     * @param source the original source
     * @return GLSL 3.30 core source
     */
    static String translate(int type, String source) {
        source = zeroUndefinedMacros(source.replace("\r\n", "\n").replace('\r', '\n'));
        int target = 330;
        Matcher version = VERSION.matcher(source);
        if (version.find()) {
            int number = Integer.parseInt(version.group(1));
            String profile = version.group(2) == null ? "" : version.group(2).trim();
            if (!profile.equals("compatibility") && (number >= 330 || (number >= 150 && profile.equals("core")))) {
                return source;
            }
            target = Math.max(330, number);
            source = version.replaceFirst("");
        }
        if (type != VERTEX && type != FRAGMENT) {
            return "#version " + target + " core\n" + source;
        }
        boolean vertex = type == VERTEX;
        String body = EXTENSION.matcher(source).replaceAll("");
        Set<String> decls = new LinkedHashSet<>();

        body = body.replaceAll("\\btexture\\b(?!\\s*\\()", TEXTURE_RENAMED);
        body = word(body, "attribute", "in");
        body = word(body, "varying", vertex ? "out" : "in");

        if (body.contains("ftransform()")) {
            body = body.replace("ftransform()", "(lz_ModelViewProjectionMatrix * lz_Vertex)");
        }
        if (vertex) {
            body = builtin(body, "gl_Vertex", "lz_Vertex", "in vec4 lz_Vertex;", decls);
            body = builtin(body, "gl_Color", "lz_Color", "in vec4 lz_Color;", decls);
            body = builtin(body, "gl_MultiTexCoord0", "lz_MultiTexCoord0", "in vec4 lz_MultiTexCoord0;", decls);
            body = builtin(body, "gl_MultiTexCoord1", "lz_MultiTexCoord1", "in vec4 lz_MultiTexCoord1;", decls);
            body = builtin(body, "gl_Normal", "lz_Normal", "in vec3 lz_Normal;", decls);
            body = builtin(body, "gl_FrontColor", "lz_FrontColor", "out vec4 lz_FrontColor;", decls);
            body = builtin(body, "gl_TexCoord", "lz_TexCoord", "out vec4 lz_TexCoord[4];", decls);
        } else {
            body = builtin(body, "gl_Color", "lz_FrontColor", "in vec4 lz_FrontColor;", decls);
            body = builtin(body, "gl_TexCoord", "lz_TexCoord", "in vec4 lz_TexCoord[4];", decls);
            body = builtin(body, "gl_FragData", "lz_FragData", "out vec4 lz_FragData[8];", decls);
            body = builtin(body, "gl_FragColor", "lz_FragColor", "out vec4 lz_FragColor;", decls);
        }
        body = builtin(
                body,
                "gl_ModelViewProjectionMatrix",
                "lz_ModelViewProjectionMatrix",
                "uniform mat4 lz_ModelViewProjectionMatrix;",
                decls);
        body = builtin(body, "gl_ModelViewMatrix", "lz_ModelViewMatrix", "uniform mat4 lz_ModelViewMatrix;", decls);
        body = builtin(body, "gl_ProjectionMatrix", "lz_ProjectionMatrix", "uniform mat4 lz_ProjectionMatrix;", decls);
        body = builtin(body, "gl_TextureMatrix", "lz_TextureMatrix", "uniform mat4 lz_TextureMatrix[2];", decls);
        body = builtin(body, "gl_NormalMatrix", "lz_NormalMatrix", "uniform mat3 lz_NormalMatrix;", decls);
        body = builtin(
                body,
                "gl_Fog",
                "lz_Fog",
                "struct lz_FogParameters { vec4 color; float density; float start; float end; float scale; };\n"
                        + "uniform lz_FogParameters lz_Fog;",
                decls);
        body = builtin(
                body, "gl_FogFragCoord", "lz_FogFragCoord", (vertex ? "out" : "in") + " float lz_FogFragCoord;", decls);
        if (body.contains("lz_ModelViewProjectionMatrix * lz_Vertex")) {
            decls.add("uniform mat4 lz_ModelViewProjectionMatrix;");
            decls.add("in vec4 lz_Vertex;");
        }

        body = call(body, "texture2DLodARB", "textureLod");
        body = call(body, "texture2DGradARB", "textureGrad");
        body = call(body, "texture2DProjLodARB", "textureProjLod");
        body = call(body, "texture3DLodARB", "textureLod");
        body = call(body, "shadow2DLodARB", "shadow2DLod");
        body = call(body, "texture2DLod", "textureLod");
        body = call(body, "texture2DGrad", "textureGrad");
        body = call(body, "texture2DProjLod", "textureProjLod");
        body = call(body, "texelFetch2D", "texelFetch");
        body = call(body, "textureSize2D", "textureSize");
        body = call(body, "texture2DProj", "textureProj");
        body = call(body, "texture2D", "texture");
        body = call(body, "texture3D", "texture");
        body = call(body, "textureCube", "texture");
        if (body.matches("(?s).*\\bshadow2DLod\\s*\\(.*")) {
            body = call(body, "shadow2DLod", "lz_shadow2DLod");
            decls.add("vec4 lz_shadow2DLod(sampler2DShadow s, vec3 c, float l) { return vec4(textureLod(s, c, l)); }");
        }
        if (body.matches("(?s).*\\bshadow2D\\s*\\(.*")) {
            body = call(body, "shadow2D", "lz_shadow2D");
            decls.add("vec4 lz_shadow2D(sampler2DShadow s, vec3 c) { return vec4(texture(s, c)); }");
        }

        StringBuilder out = new StringBuilder("#version " + target + " core\n");
        for (String decl : decls) {
            out.append(decl).append('\n');
        }
        out.append(body);
        if (!vertex) {
            alphaTest(out, decls, body);
        }
        return out.toString();
    }

    private static final Pattern MAIN = Pattern.compile("\\bvoid\\s+main\\s*\\(\\s*(void)?\\s*\\)");
    private static final Pattern OUTPUT =
            Pattern.compile("(?m)^\\s*(?:layout\\s*\\([^)]*\\)\\s*)?out\\s+vec4\\s+(\\w+)");

    /**
     * Compatibility profiles apply the fixed-function alpha test after a fragment shader, and shader packs rely on it
     * for leaves and other cutouts. Core has no alpha test, so the shader's main is wrapped in one, driven by the
     * game's alpha state ({@code lz_AlphaFunc} 0 means off).
     */
    private static void alphaTest(StringBuilder out, Set<String> decls, String body) {
        String target;
        if (decls.contains("out vec4 lz_FragColor;")) {
            target = "lz_FragColor";
        } else if (decls.contains("out vec4 lz_FragData[8];")) {
            target = "lz_FragData[0]";
        } else {
            Matcher m = OUTPUT.matcher(body);
            if (!m.find()) return;
            target = m.group(1);
        }
        Matcher main = MAIN.matcher(out);
        if (!main.find()) return;
        out.replace(main.start(), main.end(), "void lz_main()");
        out.append("\nuniform int lz_AlphaFunc;\nuniform float lz_AlphaRef;\n")
                .append("void main() {\n    lz_main();\n    float a = ")
                .append(target)
                .append(".a;\n")
                .append("    if (lz_AlphaFunc == 0x0200 || (lz_AlphaFunc == 0x0201 && !(a < lz_AlphaRef))\n")
                .append("            || (lz_AlphaFunc == 0x0202 && a != lz_AlphaRef)")
                .append(" || (lz_AlphaFunc == 0x0203 && !(a <= lz_AlphaRef))\n")
                .append("            || (lz_AlphaFunc == 0x0204 && !(a > lz_AlphaRef))")
                .append(" || (lz_AlphaFunc == 0x0205 && a == lz_AlphaRef)\n")
                .append("            || (lz_AlphaFunc == 0x0206 && !(a >= lz_AlphaRef))) discard;\n}\n");
    }

    private static final Pattern DEFINE = Pattern.compile("(?m)^[ \\t]*#[ \\t]*define[ \\t]+(\\w+)");
    private static final Pattern IDENTIFIER = Pattern.compile("\\b[A-Za-z_]\\w*");

    private static final Pattern DIRECTIVE = Pattern.compile("^[ \\t]*#[ \\t]*(\\w+)(.*)$");

    /** One {@code #if}...{@code #endif} chain: where it starts in the output, and the identifiers to guard. */
    private record Chain(int start, java.util.Set<String> guarded) {}

    /**
     * Makes macro tests behave as on drivers that follow C, where an identifier that is not defined at that point
     * counts as 0. Apple's driver rejects such lines instead. Identifiers never defined anywhere become 0; for those
     * defined only on some paths, the whole {@code #if} chain is wrapped in a guard that defines them as 0 when they
     * are undefined, and removes that definition again after the chain's {@code #endif}.
     */
    static String zeroUndefinedMacros(String source) {
        java.util.Set<String> defined = new java.util.HashSet<>();
        Matcher d = DEFINE.matcher(source);
        while (d.find()) {
            defined.add(d.group(1));
        }
        String[] lines = source.split("\n", -1);
        java.util.List<String> out = new java.util.ArrayList<>(lines.length);
        java.util.ArrayDeque<Chain> chains = new java.util.ArrayDeque<>();
        for (String line : lines) {
            Matcher directive = DIRECTIVE.matcher(line);
            String name = directive.matches() ? directive.group(1) : "";
            switch (name) {
                case "if", "ifdef", "ifndef" -> {
                    chains.push(new Chain(out.size(), new java.util.LinkedHashSet<>()));
                    out.add(name.equals("if") ? rewriteCondition(line, directive, defined, chains.peek()) : line);
                }
                case "elif" ->
                    out.add(chains.isEmpty() ? line : rewriteCondition(line, directive, defined, chains.peek()));
                case "endif" -> {
                    out.add(line);
                    Chain chain = chains.isEmpty() ? null : chains.pop();
                    if (chain != null && !chain.guarded().isEmpty()) {
                        java.util.List<String> before = new java.util.ArrayList<>();
                        for (String id : chain.guarded()) {
                            before.add("#ifndef " + id);
                            before.add("#define " + id + " 0");
                            before.add("#define LZ_ZERO_" + id);
                            before.add("#endif");
                            out.add("#ifdef LZ_ZERO_" + id);
                            out.add("#undef " + id);
                            out.add("#undef LZ_ZERO_" + id);
                            out.add("#endif");
                        }
                        out.addAll(chain.start(), before);
                    }
                }
                default -> out.add(line);
            }
        }
        return String.join("\n", out);
    }

    private static String rewriteCondition(String line, Matcher directive, java.util.Set<String> defined, Chain chain) {
        String rest = directive.group(2);
        String head = line.substring(0, directive.start(2));
        int comment = commentStart(rest);
        String expression = rest.substring(0, comment);
        StringBuilder rewritten = new StringBuilder(head);
        Matcher id = IDENTIFIER.matcher(expression);
        int last = 0;
        boolean afterDefined = false;
        while (id.find()) {
            String name = id.group();
            rewritten.append(expression, last, id.start());
            boolean builtin = name.equals("defined") || name.startsWith("GL_") || name.startsWith("__");
            if (builtin || afterDefined) {
                rewritten.append(name);
            } else if (defined.contains(name)) {
                rewritten.append(name);
                chain.guarded().add(name);
            } else {
                rewritten.append('0');
            }
            afterDefined = name.equals("defined");
            last = id.end();
        }
        return rewritten
                .append(expression.substring(last))
                .append(rest.substring(comment))
                .toString();
    }

    /** {@return where a {@code //} or {@code /*} comment starts in a preprocessor line, or its length} */
    private static int commentStart(String line) {
        int slash = line.indexOf("//");
        int block = line.indexOf("/*");
        if (slash < 0) return block < 0 ? line.length() : block;
        return block < 0 ? slash : Math.min(slash, block);
    }

    private static String word(String s, String from, String to) {
        return s.replaceAll("\\b" + from + "\\b", to);
    }

    private static String call(String s, String from, String to) {
        return s.replaceAll("\\b" + from + "(\\s*\\()", to + "$1");
    }

    private static String builtin(String s, String from, String to, String decl, Set<String> decls) {
        if (!s.matches("(?s).*\\b" + from + "\\b.*")) {
            return s;
        }
        decls.add(decl);
        return word(s, from, to);
    }
}
