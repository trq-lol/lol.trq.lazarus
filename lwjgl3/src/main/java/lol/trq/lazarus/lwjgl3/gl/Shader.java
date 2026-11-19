/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import static org.lwjgl.opengl.GL33C.*;

/**
 * The fixed-function pipeline as one GLSL 3.30 core shader driven by uniforms: transforms, texture matrices, two
 * texture units (the second is Minecraft's lightmap), two directional lights with colour material, linear and
 * exponential fog, and the alpha test.
 */
final class Shader {
    static final int POS = 0;
    static final int COLOR = 1;
    static final int UV0 = 2;
    static final int UV1 = 3;
    static final int NORMAL = 4;

    private static final String VERTEX = """
            #version 330 core
            layout(location = 0) in vec4 aPos;
            layout(location = 1) in vec4 aColor;
            layout(location = 2) in vec4 aUv0;
            layout(location = 3) in vec4 aUv1;
            layout(location = 4) in vec3 aNormal;
            uniform mat4 uModelView;
            uniform mat4 uProjection;
            uniform mat4 uTex0;
            uniform mat4 uTex1;
            uniform mat3 uNormalMat;
            uniform bool uLighting;
            uniform vec3 uLightDir0;
            uniform vec3 uLightDir1;
            uniform vec3 uLightDiffuse0;
            uniform vec3 uLightDiffuse1;
            uniform vec3 uLightAmbient;
            uniform bool uLight0;
            uniform bool uLight1;
            out vec4 vColor;
            out vec2 vUv0;
            out vec2 vUv1;
            out float vFogDist;
            void main() {
                vec4 eye = uModelView * aPos;
                gl_Position = uProjection * eye;
                vColor = aColor;
                if (uLighting) {
                    vec3 n = normalize(uNormalMat * aNormal);
                    vec3 lit = uLightAmbient;
                    if (uLight0) lit += max(dot(n, uLightDir0), 0.0) * uLightDiffuse0;
                    if (uLight1) lit += max(dot(n, uLightDir1), 0.0) * uLightDiffuse1;
                    vColor.rgb *= clamp(lit, 0.0, 1.0);
                }
                vUv0 = (uTex0 * vec4(aUv0.xy, 0.0, 1.0)).xy;
                vUv1 = (uTex1 * vec4(aUv1.xy, 0.0, 1.0)).xy;
                vFogDist = abs(eye.z);
            }
            """;

    private static final String FRAGMENT = """
            #version 330 core
            in vec4 vColor;
            in vec2 vUv0;
            in vec2 vUv1;
            in float vFogDist;
            uniform sampler2D uSampler0;
            uniform sampler2D uSampler1;
            uniform bool uTexture0;
            uniform bool uTexture1;
            uniform int uAlphaFunc;
            uniform float uAlphaRef;
            uniform int uFogMode;
            uniform float uFogStart;
            uniform float uFogEnd;
            uniform float uFogDensity;
            uniform vec4 uFogColor;
            out vec4 fragColor;
            bool passes(float a) {
                if (uAlphaFunc == 0x0201) return a < uAlphaRef;
                if (uAlphaFunc == 0x0202) return a == uAlphaRef;
                if (uAlphaFunc == 0x0203) return a <= uAlphaRef;
                if (uAlphaFunc == 0x0204) return a > uAlphaRef;
                if (uAlphaFunc == 0x0205) return a != uAlphaRef;
                if (uAlphaFunc == 0x0206) return a >= uAlphaRef;
                if (uAlphaFunc == 0x0200) return false;
                return true;
            }
            void main() {
                vec4 c = vColor;
                if (uTexture0) c *= texture(uSampler0, vUv0);
                if (uTexture1) c *= texture(uSampler1, vUv1);
                if (uAlphaFunc != 0 && !passes(c.a)) discard;
                if (uFogMode != 0) {
                    float f;
                    if (uFogMode == 0x2601) f = (uFogEnd - vFogDist) / (uFogEnd - uFogStart);
                    else if (uFogMode == 0x0800) f = exp(-uFogDensity * vFogDist);
                    else f = exp(-pow(uFogDensity * vFogDist, 2.0));
                    c.rgb = mix(uFogColor.rgb, c.rgb, clamp(f, 0.0, 1.0));
                }
                fragColor = c;
            }
            """;

    final int program;
    final int modelView;
    final int projection;
    final int tex0;
    final int tex1;
    final int normalMat;
    final int lighting;
    final int lightDir0;
    final int lightDir1;
    final int lightDiffuse0;
    final int lightDiffuse1;
    final int lightAmbient;
    final int light0;
    final int light1;
    final int texture0;
    final int texture1;
    final int alphaFunc;
    final int alphaRef;
    final int fogMode;
    final int fogStart;
    final int fogEnd;
    final int fogDensity;
    final int fogColor;
    final Uniforms uniforms;

    Shader() {
        int vs = compile(GL_VERTEX_SHADER, VERTEX);
        int fs = compile(GL_FRAGMENT_SHADER, FRAGMENT);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("fixed-function shader link failed: " + glGetProgramInfoLog(program));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
        modelView = glGetUniformLocation(program, "uModelView");
        projection = glGetUniformLocation(program, "uProjection");
        tex0 = glGetUniformLocation(program, "uTex0");
        tex1 = glGetUniformLocation(program, "uTex1");
        normalMat = glGetUniformLocation(program, "uNormalMat");
        lighting = glGetUniformLocation(program, "uLighting");
        lightDir0 = glGetUniformLocation(program, "uLightDir0");
        lightDir1 = glGetUniformLocation(program, "uLightDir1");
        lightDiffuse0 = glGetUniformLocation(program, "uLightDiffuse0");
        lightDiffuse1 = glGetUniformLocation(program, "uLightDiffuse1");
        lightAmbient = glGetUniformLocation(program, "uLightAmbient");
        light0 = glGetUniformLocation(program, "uLight0");
        light1 = glGetUniformLocation(program, "uLight1");
        texture0 = glGetUniformLocation(program, "uTexture0");
        texture1 = glGetUniformLocation(program, "uTexture1");
        alphaFunc = glGetUniformLocation(program, "uAlphaFunc");
        alphaRef = glGetUniformLocation(program, "uAlphaRef");
        fogMode = glGetUniformLocation(program, "uFogMode");
        fogStart = glGetUniformLocation(program, "uFogStart");
        fogEnd = glGetUniformLocation(program, "uFogEnd");
        fogDensity = glGetUniformLocation(program, "uFogDensity");
        fogColor = glGetUniformLocation(program, "uFogColor");
        int maxLocation = 0;
        for (int location : new int[] {
            modelView,
            projection,
            tex0,
            tex1,
            normalMat,
            lighting,
            lightDir0,
            lightDir1,
            lightDiffuse0,
            lightDiffuse1,
            lightAmbient,
            light0,
            light1,
            texture0,
            texture1,
            alphaFunc,
            alphaRef,
            fogMode,
            fogStart,
            fogEnd,
            fogDensity,
            fogColor
        }) {
            maxLocation = Math.max(maxLocation, location);
        }
        uniforms = new Uniforms(maxLocation + 1);
        int previous = glGetInteger(GL_CURRENT_PROGRAM);
        glUseProgram(program);
        glUniform1i(glGetUniformLocation(program, "uSampler0"), 0);
        glUniform1i(glGetUniformLocation(program, "uSampler1"), 1);
        glUseProgram(previous);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("fixed-function shader compile failed: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }
}
