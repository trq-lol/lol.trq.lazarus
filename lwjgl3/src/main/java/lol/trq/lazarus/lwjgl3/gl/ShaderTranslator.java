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
        Matcher version = VERSION.matcher(source);
        if (version.find()) {
            int number = Integer.parseInt(version.group(1));
            String profile = version.group(2) == null ? "" : version.group(2).trim();
            if (number >= 330 || (number >= 150 && profile.equals("core"))) {
                return source;
            }
            source = version.replaceFirst("");
        }
        if (type != VERTEX && type != FRAGMENT) {
            return source;
        }
        boolean vertex = type == VERTEX;
        String body = EXTENSION.matcher(source).replaceAll("");
        Set<String> decls = new LinkedHashSet<>();

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
        if (body.contains("lz_ModelViewProjectionMatrix * lz_Vertex")) {
            decls.add("uniform mat4 lz_ModelViewProjectionMatrix;");
            decls.add("in vec4 lz_Vertex;");
        }

        body = call(body, "texture2DLod", "textureLod");
        body = call(body, "texture2DProj", "textureProj");
        body = call(body, "texture2D", "texture");
        body = call(body, "texture3D", "texture");
        body = call(body, "textureCube", "texture");
        if (body.matches("(?s).*\\bshadow2D\\s*\\(.*")) {
            body = call(body, "shadow2D", "lz_shadow2D");
            decls.add("vec4 lz_shadow2D(sampler2DShadow s, vec3 c) { return vec4(texture(s, c)); }");
        }

        StringBuilder out = new StringBuilder("#version 330 core\n");
        for (String decl : decls) {
            out.append(decl).append('\n');
        }
        return out.append(body).toString();
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
