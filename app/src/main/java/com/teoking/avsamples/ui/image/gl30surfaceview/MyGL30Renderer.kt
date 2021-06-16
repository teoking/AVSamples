package com.teoking.avsamples.ui.image.gl30surfaceview

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.teoking.avsamples.R
import com.teoking.avsamples.util.ESShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGL30Renderer(private val context: Context) : GLSurfaceView.Renderer {

    // Handle to a program object
    private var mProgramObject = 0

    // Sampler location
    private var mSamplerLoc = 0

    // Texture handle
    private var mTextureId = 0

    // Additional member variables
    private var mWidth = 0
    private var mHeight = 0
    private val mVertices: FloatBuffer
    private val mIndices: ShortBuffer

    private val mVerticesData = floatArrayOf(
        -0.5f, 0.5f, 0.0f,  // Position 0
        0.0f, 0.0f,  // TexCoord 0
        -0.5f, -0.5f, 0.0f,  // Position 1
        0.0f, 1.0f,  // TexCoord 1
        0.5f, -0.5f, 0.0f,  // Position 2
        1.0f, 1.0f,  // TexCoord 2
        0.5f, 0.5f, 0.0f,  // Position 3
        1.0f, 0.0f // TexCoord 3
    )

    private val mIndicesData = shortArrayOf(
        0, 1, 2, 0, 2, 3
    )

    init {
        mVertices = ByteBuffer.allocateDirect(mVerticesData.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mVertices.put(mVerticesData).position(0)
        mIndices = ByteBuffer.allocateDirect(mIndicesData.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        mIndices.put(mIndicesData).position(0)
    }

    ///
    // Initialize the shader and program object
    //
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vShaderStr = """
#version 300 es              				
layout(location = 0) in vec4 a_position;   
layout(location = 1) in vec2 a_texCoord;   
out vec2 v_texCoord;     	  				
void main()                  				
{                            				
   gl_Position = a_position; 				
   v_texCoord = a_texCoord;  				
}                            				
"""

        val fShaderStr = """
#version 300 es                                     
precision mediump float;                            
in vec2 v_texCoord;                            	 
layout(location = 0) out vec4 outColor;             
uniform sampler2D s_texture;                        
void main()                                         
{                                                   
  outColor = texture( s_texture, v_texCoord );      
}                                                   
"""

        // Load the shaders and get a linked program object
        mProgramObject = ESShader.loadProgram(vShaderStr, fShaderStr)

        // Get the sampler location
        mSamplerLoc = GLES30.glGetUniformLocation(mProgramObject, "s_texture")

        // Load the texture
        mTextureId = loadTexture(context, R.drawable.james)

        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 0.0f)
    }

    //
    // Draw a triangle using the shader pair created in onSurfaceCreated()
    //
    override fun onDrawFrame(gl: GL10?) {
        // Set the viewport
        GLES30.glViewport(0, 0, mWidth, mHeight)

        // Clear the color buffer
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        // Use the program object
        GLES30.glUseProgram(mProgramObject)

        // Load the vertex position
        mVertices.position(0)
        GLES30.glVertexAttribPointer(
            0, 3, GLES30.GL_FLOAT,
            false,
            5 * 4, mVertices
        )
        // Load the texture coordinate
        mVertices.position(3)
        GLES30.glVertexAttribPointer(
            1, 2, GLES30.GL_FLOAT,
            false,
            5 * 4,
            mVertices
        )

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)

        // Bind the texture
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId)

        // Set the sampler texture unit to 0
        GLES30.glUniform1i(mSamplerLoc, 0)

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_SHORT, mIndices)
    }

    ///
    // Handle surface changes
    //
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    // Load resource to a texture
    /**
     * Load image resource to a texture.
     */
    private fun loadTexture(context: Context, resourceId: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] != 0) {
            val options = BitmapFactory.Options()
            options.inScaled = false // No pre-scaling

            // Read in the resource
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Bind to the texture in OpenGL
            GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            // Set filtering
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_NEAREST
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_NEAREST
            )

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle()
        }
        if (textureHandle[0] == 0) {
            throw RuntimeException("Error loading texture.")
        }
        return textureHandle[0]
    }

}