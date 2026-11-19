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
}
