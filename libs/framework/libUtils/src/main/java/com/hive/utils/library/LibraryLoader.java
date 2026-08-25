// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.library;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;


/**
 * Helper used to work around native libraries loading on some systems.
 * See <a href="https://medium.com/keepsafe-engineering/the-perils-of-loading-native-libraries-on-android-befa49dce2db">ReLinker</a> for more details.
 */
public class LibraryLoader {

//	private static Context sAppContext;
//
//	/**
//	 * Initializes loader with given `Context`. Subsequent calls should have no effect since application Context is retrieved.
//	 * Libraries will not be loaded immediately but only when needed.
//	 *
//	 * @param context any Context except null
//	 */
//	public static void initialize(@NonNull final Context context) {
//		sAppContext = context.getApplicationContext();
//	}
//
//	private static Context getContext() {
//		if (sAppContext == null) {
//			try {
//				final Class<?> activityThread = Class.forName("android.app.ActivityThread");
//				final Method currentApplicationMethod = activityThread.getDeclaredMethod("currentApplication");
//				sAppContext = (Context) currentApplicationMethod.invoke(null);
//			} catch (Exception e) {
//				throw new RuntimeException("LibraryLoader not initialized. Call LibraryLoader.initialize() before using library classes.", e);
//			}
//		}
//		return sAppContext;
//	}


    public static void executeLoadLibrary(@NonNull Context context, @NonNull String library) throws Throwable {

        try {
            System.loadLibrary(library);
        } catch (UnsatisfiedLinkError e) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {

                throw new Exception("use System.loadLibrary fail; " + e.getMessage());
            }

            try {
//                if(null == context) {
//                    context = getContext();
//                }

                ReLinker.loadLibrary(context, library);
            } catch (Throwable e2) {
                throw new Exception("use ReLinker.loadLibrary fail; " + e2.getMessage());
            }
        }
    }

    public static void executeLoadLibrary(final Context context, final String library, final LibraryLoadListener listener) {
        try {

            System.loadLibrary(library);

            if (null != listener) {
                listener.onLoadLibrarySuccess();
            }
        } catch (UnsatisfiedLinkError e) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
                e.printStackTrace();

                if (null != listener) {
                    listener.onLoadLibraryFail();
                }
                return;
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ReLinker.loadLibrary(context, library);
                        if (null != listener) {
                            listener.onLoadLibrarySuccess();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();

                        if (null != listener) {
                            listener.onLoadLibraryFail();
                        }
                    }
                }
            }).start();
        }
    }

    public interface LibraryLoadListener {

        void onLoadLibrarySuccess();

        void onLoadLibraryFail();
    }
}
