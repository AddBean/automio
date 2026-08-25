package android.support.rastermillv2;

import android.content.Context;
import android.opengl.GLES10;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.bumptech.glide.integration.webp.WebpHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * 图片类型判断
 */
public final class ImageTypeHelper {

    public static boolean isAnimalImage(final Context context, File inputFile) {
        if (context == null || inputFile == null) {
            return false;
        }
        FileInputStream inputStream = null;
        try {
            ArrayPool arrayPool = Glide.get(context).getArrayPool();
            List<ImageHeaderParser> parsers = Glide.get(context).getRegistry().getImageHeaderParsers();
            inputStream = new FileInputStream(inputFile);
            ImageHeaderParser.ImageType imageType = ImageHeaderParserUtils.getType(parsers, inputStream, arrayPool);
            if (imageType == ImageHeaderParser.ImageType.GIF) {
                return true;
            } else if (imageType == ImageHeaderParser.ImageType.WEBP || imageType == ImageHeaderParser.ImageType.WEBP_A) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                inputStream = new FileInputStream(inputFile);
                WebpHeaderParser.WebpImageType webpImageType = WebpHeaderParser.getType(inputStream, arrayPool);
                return WebpHeaderParser.isAnimatedWebpType(webpImageType);
            }
        } catch (Throwable t) {

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    /**
     * https://blog.csdn.net/huyawenz/article/details/78863636?utm_source=blogxgwz7
     * <p>
     * 获取当前硬件加速可处理的纹理大小（当图片的宽或高超过上限时，开启硬件加速时，图片无法处理）
     *
     * @return
     */
    public static int[] getGLESTextureLimit() {
        int[] maxTextureSize = new int[1];
        //The value gives a rough estimate of the largest texture that the GL can handle. The value must be at least 64
        maxTextureSize[0] = 1280;
        try {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                getGLESTextureLimitBelowLollipop(maxTextureSize);
            } else {
                getGLESTextureLimitEqualAboveLollipop(maxTextureSize);
            }
        } catch (Throwable t) {

        }
        return maxTextureSize;
    }

    /**
     * 获取OpenGL硬件加速最大限制方式如下
     * 在Lollipop版本之前可以直接获取硬件加速值
     */
    private static void getGLESTextureLimitBelowLollipop(int[] maxTextureSize) {
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
    }

    /**
     * 在Lollipop版本之后，需要用下面的方式获取
     * 拿到OpenGL硬件加速所允许的最大长宽，用来做二次bitmap压缩
     */
    private static void getGLESTextureLimitEqualAboveLollipop(int[] maxTextureSize) {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] vers = new int[2];
        egl.eglInitialize(dpy, vers);
        int[] configAttr = {
                EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER, EGL10.EGL_LEVEL, 0,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT, EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(dpy, configAttr, configs, 1, numConfig);
        if (numConfig[0] == 0) {// TROUBLE! No config found.

        }
        EGLConfig config = configs[0];
        int[] surfAttr = {
                EGL10.EGL_WIDTH, 64, EGL10.EGL_HEIGHT, 64, EGL10.EGL_NONE
        };
        EGLSurface surf = egl.eglCreatePbufferSurface(dpy, config, surfAttr);
        final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;  // missing in EGL10
        int[] ctxAttrib = {
                EGL_CONTEXT_CLIENT_VERSION, 1, EGL10.EGL_NONE
        };
        EGLContext ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, ctxAttrib);
        egl.eglMakeCurrent(dpy, surf, surf, ctx);
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
        egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(dpy, surf);
        egl.eglDestroyContext(dpy, ctx);
        egl.eglTerminate(dpy);
    }

}
