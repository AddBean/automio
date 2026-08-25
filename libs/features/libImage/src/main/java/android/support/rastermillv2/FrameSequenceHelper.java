package android.support.rastermillv2;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;



public class FrameSequenceHelper {

    private static boolean INIT_SUCC = false;

    /**
     * init webp lib
     *
     * @param globalContext
     */
    public static void init(Context globalContext) {
        try {

            try {
                System.loadLibrary("framesequencev2");
            } catch (UnsatisfiedLinkError e) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {

                    throw new Exception("use System.loadLibrary fail; " + e.getMessage());
                }

            }
            INIT_SUCC = true;
        } catch (Throwable t) {
        }
    }

    /**
     * @return
     */
    public static boolean isInitSucc() {
        return INIT_SUCC;
    }


    public static boolean isSupported(@NonNull InputStream inputStream) throws Exception {
        return FrameSequence.isSupport(inputStream, true);
    }

    private static InputStream istreamFromFile(File f) {
        try {
            return new FileInputStream(f);
        } catch (Exception ignore) {

        }
        return null;
    }
}

