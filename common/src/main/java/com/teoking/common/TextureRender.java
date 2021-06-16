package com.teoking.common;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class TextureRender {
    private static final String TAG = "TextureRender";

    // float size = 4 bytes
    private static final int FLOAT_SIZE_BYTES = 4;
    // 一组数据的大小，这里是5个float: X, Y, Z, U, V
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    // 顶点X, Y, Z的位置偏移
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    // U, V数据的位置偏移
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    // counterclockwise 逆时针须组织的顶点数据
    private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f, 1.0f, 0, 0.f, 1.f,
            1.0f, 1.0f, 0, 1.f, 1.f,
    };

    // 顶点数据buffer
    private FloatBuffer mTriangleVertices;

    // 顶点shader
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "  gl_Position = uMVPMatrix * aPosition;\n" +
                    "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // 片段shader
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +      // highp here doesn't seem to matter
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    // MVP矩阵
    private float[] mMVPMatrix = new float[16];
    // ST矩阵(纹理坐标矩阵)
    private float[] mSTMatrix = new float[16];

    // 程序句柄
    private int mProgram;
    // 纹理id
    private int mTextureID = -12345;
    // 统一变量uMVPMatrix句柄
    private int muMVPMatrixHandle;
    // 统一变量uSTMatrix句柄
    private int muSTMatrixHandle;
    // 属性aPosition句柄
    private int maPositionHandle;
    // 属性aTextureCoord句柄
    private int maTextureHandle;

    public TextureRender() {
        // 初始化顶点字节缓冲区
        // nativeOrder指的是当前下层平台的字节序
        mTriangleVertices = ByteBuffer.allocateDirect(
                mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        // put将顶点数据传递到缓冲区中
        // position指定了顶点缓冲区的位置
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        // 初始化纹理坐标矩阵mSTMatrix为单位矩阵(identity matrix)
        Matrix.setIdentityM(mSTMatrix, 0);
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void drawFrame(SurfaceTexture st) {
        checkGlError("onDrawFrame start");
        // 从SurfaceTexture中获取由最近一次updateTexImage设置的纹理图的4x4纹理坐标变换矩阵，并存储到mSTMatrix中
        st.getTransformMatrix(mSTMatrix);

        // 指定用以清除颜色缓冲区的颜色
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        // 使用上面设置的颜色(当前的)，清除颜色缓冲区
        // 这里同时清除GL_DEPTH_BUFFER_BIT和GL_COLOR_BUFFER_BIT缓冲区
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // 设置mProgram为活动程序，这样才可以开始渲染
        GLES20.glUseProgram(mProgram);
        checkGlError("glUseProgram");

        // 激活纹理单元GL_TEXTURE0，激活后GL_TEXTURE0成为当前活动纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // 绑定纹理mTextureID到当前活动纹理，即GL_TEXTURE0
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        // 移动顶点buffer位置到X, Y, Z顶点数据开始位置
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        // 指定顶点数组
        // index: maPositionHandle
        // size: 3 表示顶点属性的分量数量为3，也意味着数据以三个分量存储(即X, Y, Z)
        // type: GL_FLOAT 表示数据格式为浮点
        // normalized: false 非浮点数据格式在转换为浮点值时是否应该规范化
        // stride: TRIANGLE_VERTICES_DATA_STRIDE_BYTES stride指定顶点索引I和(I+1)表示的顶点数据之间的位移
        // ptr: mTriangleVertices 顶点属性数据buffer的指针
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maPosition");
        // 启用顶点属性数组maPositionHandle
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        checkGlError("glEnableVertexAttribArray maPositionHandle");

        // 移动顶点buffer到U, V顶点数据起始位置
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        // 参见maPositionHandle数组的指定
        GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
        checkGlError("glVertexAttribPointer maTextureHandle");
        // 启用顶点属性数组maTextureHandle
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        checkGlError("glEnableVertexAttribArray maTextureHandle");

        // 初始化MVP坐标矩阵为单位矩阵
        Matrix.setIdentityM(mMVPMatrix, 0);
        // glUniform这类函数用来加载统一变量类型到目标位置
        // 加载mMVPMatrix到muMVPMatrixHandle
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        // 加载mSTMatrix到muSTMatrixHandle
        GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

        // 绘制三角形图元
        // GL_TRIANGLE_STRIP指定以三角形网格(triangle mesh)方式绘制
        // triangle mesh是一系列相连的三角形，它们的顶点是共享的
        /*
        以mTriangleVerticesData数据为例：
            -1.0f, -1.0f, 0, 0.f, 0.f, --> A
            1.0f, -1.0f, 0, 1.f, 0.f,  --> B
            -1.0f, 1.0f, 0, 0.f, 1.f,  --> C
            1.0f, 1.0f, 0, 1.f, 1.f,   --> D
        那么下面的代码，就会绘制两个三角形，顶点分别是ABC, CBD，这样实际上是绘制了一个矩形
        */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("glDrawArrays");
        // 阻塞直到上述GL命令都执行完
        GLES20.glFinish();
    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    public void surfaceCreated() {
        // 根据顶点shader和片段shader代码创建程序
        // createProgram方法是一个创建程序的实现样板，后续可直接复用
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
        // 获取顶点属性aPosition的位置, 其类型为有4个分量的浮点向量
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        checkGlError("glGetAttribLocation aPosition");
        if (maPositionHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }
        // 获取顶点属性aTextureCoord的位置, 其类型为有4个分量的浮点向量
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        checkGlError("glGetAttribLocation aTextureCoord");
        if (maTextureHandle == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        // 获取统一变量uMVPMatrix的位置, 其类型为4x4浮点矩阵
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        checkGlError("glGetUniformLocation uMVPMatrix");
        if (muMVPMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        // 获取统一变量uSTMatrix的位置, 其类型为4x4浮点矩阵
        muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        checkGlError("glGetUniformLocation uSTMatrix");
        if (muSTMatrixHandle == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        // 生成一个纹理对象
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        // 指定纹理对象目标，GL_TEXTURE_EXTERNAL_OES是外部GLES纹理，其与传统GLES纹理（GL_TEXTURE_2D）的
        // 主要优势是前者能够直接从BufferQueue数据进行渲染
        // 详细区别见如下文档：
        // https://source.android.google.cn/devices/graphics/arch-st?hl=zh-cn
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        checkGlError("glBindTexture mTextureID");

        // 下面的纹理处理函数详情见文档：
        // https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glTexParameter.xhtml
        // 设置纹理对象目标的缩小处理函数为GL_NEAREST(从最靠近纹理坐标的纹理中获取一个单点样本)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        // 设置纹理对象目标的放大函数为GL_LINEAR(从最靠近纹理坐标的纹理中获取一个双线性样本)
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        // 纹理包装模式是定义坐标超出[0.0, 1.0]范围时发生的行为
        //  设置纹理对象目标的纹理包装模式(S坐标)为GL_CLAMP_TO_EDGE(限定读取纹理的边缘)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        //  设置纹理对象目标的纹理包装模式(T坐标)为GL_CLAMP_TO_EDGE(限定读取纹理的边缘)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        checkGlError("glTexParameter");
    }

    /**
     * Replaces the fragment shader.
     */
    public void changeFragmentShader(String fragmentShader) {
        GLES20.glDeleteProgram(mProgram);
        mProgram = createProgram(VERTEX_SHADER, fragmentShader);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
    }

    private int loadShader(int shaderType, String source) {
        // 创建一个着色器对象
        int shader = GLES20.glCreateShader(shaderType);
        checkGlError("glCreateShader type=" + shaderType);
        // 为着色器提供源代码
        GLES20.glShaderSource(shader, source);
        // 编译着色器
        GLES20.glCompileShader(shader);
        // 检查编译结果
        int[] compiled = new int[1];
        // 使用glGetShaderiv查询shader对象信息，指定GL_COMPILE_STATUS代表查询编译信息
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        // GL_FALSE == 0, GL_TRUE != 0
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            // 查询编译日志
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            // 编译失败时，要删除shader对象
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        // 返回最终的shader对象句柄
        return shader;
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        // 构建顶点着色器
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        // 构建片段着色器
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        // 创建一个程序
        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        // 连接顶点着色器到程序
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        // 连接片段着色器到程序
        GLES20.glAttachShader(program, pixelShader);
        checkGlError("glAttachShader");
        // 链接程序, 在这一步中将生成最终在硬件上运行的指令
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        // 链接完成后，使用glGetProgramiv来查询链接状态
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    public void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    /**
     * Saves the current frame to disk as a PNG image.  Frame starts from (0,0).
     * <p>
     * Useful for debugging.
     */
    public static void saveFrame(String filename, int width, int height) {
        // glReadPixels gives us a ByteBuffer filled with what is essentially big-endian RGBA
        // data (i.e. a byte of red, followed by a byte of green...).  We need an int[] filled
        // with native-order ARGB data to feed to Bitmap.
        //
        // If we implement this as a series of buf.get() calls, we can spend 2.5 seconds just
        // copying data around for a 720p frame.  It's better to do a bulk get() and then
        // rearrange the data in memory.  (For comparison, the PNG compress takes about 500ms
        // for a trivial frame.)
        //
        // So... we set the ByteBuffer to little-endian, which should turn the bulk IntBuffer
        // get() into a straight memcpy on most Android devices.  Our ints will hold ABGR data.
        // Swapping B and R gives us ARGB.  We need about 30ms for the bulk get(), and another
        // 270ms for the color swap.
        //
        // Making this even more interesting is the upside-down nature of GL, which means we
        // may want to flip the image vertically here.

        ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf);
        buf.rewind();

        int pixelCount = width * height;
        int[] colors = new int[pixelCount];
        buf.asIntBuffer().get(colors);
        for (int i = 0; i < pixelCount; i++) {
            int c = colors[i];
            colors[i] = (c & 0xff00ff00) | ((c & 0x00ff0000) >> 16) | ((c & 0x000000ff) << 16);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
            Bitmap bmp = Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
            bmp.recycle();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to write file " + filename, ioe);
        } finally {
            try {
                if (fos != null) fos.close();
            } catch (IOException ioe2) {
                throw new RuntimeException("Failed to close file " + filename, ioe2);
            }
        }
        Log.d(TAG, "Saved " + width + "x" + height + " frame as '" + filename + "'");
    }
}
