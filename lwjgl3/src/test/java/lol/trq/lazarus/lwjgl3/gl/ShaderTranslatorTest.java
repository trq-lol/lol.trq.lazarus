/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShaderTranslatorTest {
    private static final String SOBEL_VSH = """
            #version 120

            attribute vec4 Position;

            uniform mat4 ProjMat;
            uniform vec2 OutSize;

            varying vec2 texCoord;

            void main(){
                vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
                gl_Position = vec4(outPos.xy, 0.2, 1.0);
                texCoord = Position.xy / OutSize;
            }
            """;

    private static final String SOBEL_FSH = """
            #version 120

            uniform sampler2D DiffuseSampler;
            varying vec2 texCoord;

            void main(){
                vec4 center = texture2D(DiffuseSampler, texCoord);
                gl_FragColor = vec4(center.rgb, 1.0);
            }
            """;

    @Test
    void vanillaPostVertexShader() {
        String out = ShaderTranslator.translate(ShaderTranslator.VERTEX, SOBEL_VSH);
        assertTrue(out.startsWith("#version 330 core\n"), out);
        assertTrue(out.contains("in vec4 Position;"), out);
        assertTrue(out.contains("out vec2 texCoord;"), out);
        assertFalse(out.contains("#version 120"), out);
        assertFalse(out.contains("attribute"), out);
        assertFalse(out.contains("varying"), out);
    }

    @Test
    void vanillaPostFragmentShader() {
        String out = ShaderTranslator.translate(ShaderTranslator.FRAGMENT, SOBEL_FSH);
        assertTrue(out.contains("in vec2 texCoord;"), out);
        assertTrue(out.contains("out vec4 lz_FragColor;"), out);
        assertTrue(out.contains("lz_FragColor = vec4(center.rgb, 1.0);"), out);
        assertTrue(out.contains("texture(DiffuseSampler, texCoord)"), out);
        assertFalse(out.contains("gl_FragColor"), out);
        assertFalse(out.contains("texture2D"), out);
    }

    @Test
    void fixedFunctionBuiltinsBecomeEmulatorInputs() {
        String vsh = """
                varying vec4 color;
                void main() {
                    gl_Position = ftransform();
                    gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;
                    gl_FrontColor = gl_Color;
                    vec3 n = gl_NormalMatrix * gl_Normal;
                }
                """;
        String out = ShaderTranslator.translate(ShaderTranslator.VERTEX, vsh);
        for (String expected : new String[] {
            "in vec4 lz_Vertex;",
            "in vec4 lz_Color;",
            "in vec4 lz_MultiTexCoord0;",
            "in vec3 lz_Normal;",
            "out vec4 lz_FrontColor;",
            "out vec4 lz_TexCoord[4];",
            "uniform mat4 lz_ModelViewProjectionMatrix;",
            "uniform mat4 lz_TextureMatrix[2];",
            "uniform mat3 lz_NormalMatrix;",
            "gl_Position = (lz_ModelViewProjectionMatrix * lz_Vertex);"
        }) {
            assertTrue(out.contains(expected), expected + " in\n" + out);
        }
        assertFalse(out.contains("gl_Color"), out);
        assertFalse(out.contains("gl_MultiTexCoord0"), out);
    }

    @Test
    void fragmentColorInputIsTheInterpolatedFrontColor() {
        String out = ShaderTranslator.translate(
                ShaderTranslator.FRAGMENT, "void main() { gl_FragData[0] = gl_Color * gl_TexCoord[0]; }");
        assertTrue(out.contains("in vec4 lz_FrontColor;"), out);
        assertTrue(out.contains("in vec4 lz_TexCoord[4];"), out);
        assertTrue(out.contains("out vec4 lz_FragData[8];"), out);
        assertTrue(out.contains("lz_FragData[0] = lz_FrontColor * lz_TexCoord[0];"), out);
    }

    @Test
    void identifiersThatOnlyContainABuiltinNameAreLeftAlone() {
        String out = ShaderTranslator.translate(
                ShaderTranslator.FRAGMENT,
                "uniform vec4 my_gl_ColorTint; void main() { gl_FragColor = my_gl_ColorTint; }");
        assertTrue(out.contains("my_gl_ColorTint"), out);
        assertFalse(out.contains("lz_FrontColor"), out);
    }

    @Test
    void extensionsAreDroppedAndShadowLookupsKeepTheirType() {
        String out = ShaderTranslator.translate(
                ShaderTranslator.FRAGMENT,
                "#version 120\n#extension GL_EXT_gpu_shader4 : enable\nuniform sampler2DShadow s;\n"
                        + "void main() { gl_FragColor = shadow2D(s, vec3(0.5)); }");
        assertFalse(out.contains("#extension"), out);
        assertTrue(out.contains("lz_shadow2D(s, vec3(0.5))"), out);
        assertTrue(out.contains("vec4 lz_shadow2D(sampler2DShadow s, vec3 c)"), out);
    }

    @Test
    void modernShadersPassThrough() {
        String core = "#version 330 core\nout vec4 c; void main() { c = vec4(1); }";
        assertEquals(core, ShaderTranslator.translate(ShaderTranslator.FRAGMENT, core));
        String v410 = "#version 410 core\nvoid main() {}";
        assertEquals(v410, ShaderTranslator.translate(ShaderTranslator.VERTEX, v410));
    }

    @Test
    void fogBuiltinsBecomeUniformsAndVaryings() {
        String vsh = ShaderTranslator.translate(
                ShaderTranslator.VERTEX, "void main() { gl_Position = ftransform(); gl_FogFragCoord = 1.0; }");
        assertTrue(vsh.contains("out float lz_FogFragCoord;"), vsh);
        String fsh = ShaderTranslator.translate(
                ShaderTranslator.FRAGMENT,
                "void main() { float f = clamp((gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale, 0.0, 1.0);"
                        + " gl_FragColor = mix(gl_Fog.color, vec4(1.0), f); }");
        assertTrue(fsh.contains("uniform lz_FogParameters lz_Fog;"), fsh);
        assertTrue(fsh.contains("in float lz_FogFragCoord;"), fsh);
        assertTrue(fsh.contains("lz_Fog.end - lz_FogFragCoord"), fsh);
        assertFalse(fsh.contains("gl_Fog"), fsh);
    }

    @Test
    void gpuShader4FunctionsBecomeCore() {
        String out = ShaderTranslator.translate(
                ShaderTranslator.FRAGMENT,
                "#version 120\n#extension GL_EXT_gpu_shader4 : require\nuniform sampler2D t;\n"
                        + "void main() { ivec2 s = textureSize2D(t, 0); gl_FragColor = texelFetch2D(t, s / 2, 0); }");
        assertTrue(out.contains("textureSize(t, 0)"), out);
        assertTrue(out.contains("texelFetch(t, s / 2, 0)"), out);
        assertFalse(out.contains("#extension"), out);
    }

    @Test
    void samplerNamedTextureIsRenamedAwayFromTheBuiltin() {
        String out = ShaderTranslator.translate(
                ShaderTranslator.FRAGMENT,
                "uniform sampler2D texture; varying vec2 uv;"
                        + " void main() { gl_FragColor = texture2D(texture, uv) + texture2DGradARB(texture, uv, uv, uv); }");
        assertTrue(out.contains("uniform sampler2D lz_texture;"), out);
        assertTrue(out.contains("texture(lz_texture, uv)"), out);
        assertTrue(out.contains("textureGrad(lz_texture, uv, uv, uv)"), out);
    }

    @Test
    void fragmentShadersGetTheCompatibilityAlphaTest() {
        String out = ShaderTranslator.translate(
                ShaderTranslator.FRAGMENT, "uniform sampler2D t; void main() { gl_FragData[0] = vec4(1.0); }");
        assertTrue(out.contains("void lz_main()"), out);
        assertTrue(out.contains("uniform int lz_AlphaFunc;"), out);
        assertTrue(out.contains("float a = lz_FragData[0].a;"), out);
        assertTrue(out.contains("discard;"), out);
        String vertex =
                ShaderTranslator.translate(ShaderTranslator.VERTEX, "void main() { gl_Position = ftransform(); }");
        assertFalse(vertex.contains("lz_AlphaFunc"), vertex);
    }

    @Test
    void undefinedMacrosInConditionsBecomeZero() {
        String src = "#define MC_VERSION 10809\n#if IRIS_VERSION >= 10800 && MC_VERSION > 0x10\n#endif\n"
                + "#if !defined EXTRA && defined(OTHER) || FOO\n#elif BAR\n#endif\n";
        String out = ShaderTranslator.zeroUndefinedMacros(src);
        assertTrue(out.contains("#if 0 >= 10800 && MC_VERSION > 0x10"), out);
        assertTrue(out.contains("#if !defined EXTRA && defined(OTHER) || 0"), out);
        assertTrue(out.contains("#elif 0"), out);
    }

    @Test
    void compatibilityProfileShadersAreTranslatedAtTheirVersion() {
        String out = ShaderTranslator.translate(
                ShaderTranslator.VERTEX,
                "#version 400 compatibility\nattribute vec4 a; void main() { gl_Position = a; }");
        assertTrue(out.startsWith("#version 400 core\n"), out);
        assertTrue(out.contains("in vec4 a;"), out);
        assertFalse(out.contains("attribute"), out);
    }

    @Test
    void conditionCommentsAreLeftAlone() {
        String out = ShaderTranslator.zeroUndefinedMacros("#if FOO == 1 // FOO is 0 or 1"\"n#endif"\"n");
        assertTrue(out.contains("#if 0 == 1 // FOO is 0 or 1"), out);
    }
}
