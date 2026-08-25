// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.io;

import android.database.Cursor;
import android.os.MemoryFile;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.zip.ZipFile;

/**
 * Created by mengliwei on 16/5/30.
 */
public final class IoUtil {

    //默认的IO拷贝缓存大小
    private static int CACHESIZE = 1024 * 8;
    private static final int SKIP_BUFFER_SIZE = 2048;
    private static byte[] SKIP_BYTE_BUFFER;

    private IoUtil() {

    }

    /**
     * 初始化缓存大小
     *
     * @param cacheSize
     */
    public static void init(final int cacheSize) {
        CACHESIZE = cacheSize;
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable e) {
                try {
                    Method m = c.getClass().getDeclaredMethod("close");
                    m.setAccessible(true);
                    m.invoke(c);
                } catch (Throwable er) {
                }
            }
        }
    }

    public static void closeQuietly(final Reader input) {
        closeSilently((Closeable) input);
    }

    public static void closeQuietly(final Writer output) {
        closeSilently((Closeable) output);
    }

    public static void closeQuietly(final InputStream input) {
        closeSilently((Closeable) input);
    }

    public static void closeQuietly(final OutputStream out) {
        closeSilently((Closeable) out);
    }

    public static void closeQuietly(final Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        for (final Closeable closeable : closeables) {
            closeSilently(closeable);
        }
    }

    public static void closeSilently(Object c) {
        if (c != null) {
            try {
                if (c instanceof Closeable) {
                    closeSilently((Closeable) c);
                    return;
                }
                Method m = c.getClass().getDeclaredMethod("close");
                m.setAccessible(true);
                m.invoke(c);
            } catch (Throwable e) {
            }
        }
    }

    /**
     * @param file
     */
    public static void closeSilently(RandomAccessFile file) {
        if (file != null) {
            try {
                file.close();
            } catch (Exception var2) {
            }
        }
    }

    public static void closeSilently(MemoryFile memoryFile) {
        if (memoryFile == null) {
            return;
        }
        memoryFile.close();
    }

    public static void closeSilently(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    public static void closeSilently(ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
            }
        }
    }


    public static void skipFully(InputStream input, long toSkip) throws IOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Bytes to skip must not be negative: " + toSkip);
        }
        long skipped = skip(input, toSkip);
        if (skipped != toSkip) {
            throw new EOFException("Bytes to skip: " + toSkip + " actual: " + skipped);
        }
    }

    public static long skip(InputStream input, long toSkip) throws IOException {
        if (toSkip < 0) {
            throw new IllegalArgumentException("Skip count must be non-negative, actual: " + toSkip);
        }
        if (SKIP_BYTE_BUFFER == null) {
            SKIP_BYTE_BUFFER = new byte[SKIP_BUFFER_SIZE];
        }
        long remain = toSkip;
        while (remain > 0) {
            long n = input.read(SKIP_BYTE_BUFFER, 0, (int) Math.min(remain, SKIP_BUFFER_SIZE));
            if (n < 0) { // EOF
                break;
            }
            remain -= n;
        }
        return toSkip - remain;
    }

    /**
     * @param input
     * @param out
     * @return
     * @throws IOException
     */
    public static boolean copy(final InputStream input, final OutputStream out) throws IOException {
        if (input == null || out == null) {
            return false;
        }
        byte[] buffers = new byte[CACHESIZE];
        try {
            int len = 0;
            while ((len = input.read(buffers)) != -1) {
                out.write(buffers, 0, len);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * Copies all bytes from the input stream to the output stream. Does not close or flush either
     * stream.
     *
     * @param from the input stream to read from
     * @param to   the output stream to write to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs
     */
    public static long copy2(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[CACHESIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static int read(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        int total = 0;
        while (total < len) {
            int result = in.read(b, off + total, len - total);
            if (result == -1) {
                break;
            }
            total += result;
        }
        return total;
    }


    public static byte[] read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy2(in, out);
        return out.toByteArray();
    }

    /**
     * @param in
     * @param expectedSize
     * @return
     * @throws IOException
     */
    public static byte[] read(InputStream in, int expectedSize) throws IOException {
        byte[] bytes = new byte[expectedSize];
        int remaining = expectedSize;
        while (remaining > 0) {
            int off = expectedSize - remaining;
            int read = in.read(bytes, off, remaining);
            if (read == -1) {
                return Arrays.copyOf(bytes, off);
            }
            remaining -= read;
        }
        return bytes;
    }

}
