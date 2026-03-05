package com.customphase.hdrezkacustom

import android.content.Context
import android.opengl.GLES20
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram

@UnstableApi
class BrightnessShaderProgram(
    context: Context,
    useHdr: Boolean,
    private val brightness: Float
) : BaseGlShaderProgram(useHdr, /* inputCapacity= */ 1) {

    private val glProgram: GlProgram
    private val vertPath = "vert_shader.glsl"
    private val fragPath = "frag_shader.glsl"

    init {
        glProgram = GlProgram(context, vertPath, fragPath)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        glProgram.use()
        glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
        glProgram.setFloatUniform("uBrightness", brightness)
        glProgram.setBufferAttribute(
            "aFramePosition",
            GlUtil.getNormalizedCoordinateBounds(), // Стандартные границы [-1, 1]
            4
        )
        glProgram.bindAttributesAndUniforms()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}

@UnstableApi
class BrightnessEffect(val brightness: Float) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return BrightnessShaderProgram(context, useHdr, brightness)
    }
}