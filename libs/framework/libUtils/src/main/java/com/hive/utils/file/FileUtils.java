// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.file;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.io.IoUtil;
import com.hive.utils.system.CommonTools;
import com.hive.utils.utils.StringUtils;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * file utils
 * Created by gzg on 2016/4/5.
 */
public class FileUtils {
    public final static String FILE_EXTENSION_SEPARATOR = "";
    public static final String PNG = ".jpg";
    private static final String UGC_DIR = "UGC";
    private static final String UGC_COVER = "cover";

    /**
     * 判断 sdcard 是否可用
     *
     * @return true：可用 or false
     */
    public static boolean ExistSDCard() {

        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    public static String getFinalVideoCoverPath(Context context, String name) {
        return getFinalVideoSaveDir(context) + name + PNG;
    }

    public static String getFinalVideoSaveDir(@NonNull Context context) {
        String path = context.getExternalFilesDir(UGC_DIR) + File.separator;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }


    public static String getCapturePictureSavePath(String name) {
        String cachePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        if (!TextUtils.isEmpty(cachePath)) {
            File file = new File(cachePath);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        return cachePath + name;
    }

    /**
     * 清空某个目录，或刪除某个文件
     */
    public static void clearDir(File fileToDelete) {

        if (null == fileToDelete) {
            return;
        }

        // 先改名再删除，防止出现EBUSY(Device or resource busy)
        File file;
        final File renamedFile = new File(fileToDelete.getAbsolutePath() + System.currentTimeMillis());
        boolean res = fileToDelete.renameTo(renamedFile);

        if (res) {
            file = renamedFile;
        } else {
            file = fileToDelete;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (null != files && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        clearDir(files[i]);
                    } else if (files[i].isFile()) {
                        files[i].delete();
                    }
                }
            }
            file.delete();

        } else if (file.isFile()) {
            file.delete();
        }
    }

    public static File makeDirAndCreateFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.isDirectory()) {
            file.mkdirs();
            return file;

        }
        String parent = file.getParent();
        File parentFile = new File(parent);
        if (!(parentFile.exists())) {
            if (parentFile.mkdirs()) {
                file.createNewFile();
            }
        }

        if (!(file.exists())) {
            file.createNewFile();
        }

        return file;
    }

    public static boolean writeAssetsFile(Context context, String path, String name) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(name);
            outputStream = new FileOutputStream(path);
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 拷贝文件
     *
     * @param path
     * @param inputStream
     */
    public static boolean writeFile(String path, InputStream inputStream) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(path));
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            return true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("FileNotFoundException occurred. ", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 拷贝文件
     *
     * @param path
     * @param inputStream
     */
    public static void writeFile(String path, FileInputStream inputStream) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(path));
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * String转换为File。
     * 如果创建失败，会删除文件。
     * @param string 字符内容
     * @param file   文件
     * @return 如果创建成功，返回true；否则返回false
     */
    public static boolean string2File(String string, File file) {
        if (file == null || string == null) return false;
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(string);
            bufferedWriter.flush();
            return true;
        } catch (IOException e) {
            deleteFile(file);
            return false;
        } finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * 删除文件:先重名下，然后再删除，这样即使删除不成功，也能保证下原来的视频不在了
     *
     * @param file
     * @return
     */
    public static boolean deleteFile(File file) {
        if (file != null && file.exists()) {
            final File copyFile = new File(file.getAbsolutePath() + System.currentTimeMillis());
            if (file.renameTo(copyFile)) {
                copyFile.delete();
                return true;
            } else {
                file.delete();
            }
        }
        return false;
    }

    /**
     * 删除文件
     *
     * @param file
     * @return
     */
    public static boolean simpleDeleteFile(File file) {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }

    public static boolean deleteDir(File dirfile) {
        if (dirfile != null && dirfile.exists()) {
            final File copyFile = new File(dirfile.getAbsolutePath() + System.currentTimeMillis());
            if (dirfile.renameTo(copyFile)) {
                copyFile.delete();
                return true;
            } else {
                dirfile.delete();
            }
        }
        return false;
    }

    /**
     * 删除文件(夹)及包含文件
     *
     * @param path
     * @return
     */
//    public static boolean clearDir(final File path) {
//        return clearDirectory(path, true);
//    }


    /**
     * 清空文件夹。
     *
     * @param path
     * @param removeSelf true 删除文件夹及内容，false 仅仅删除文件夹内容
     */
    public static boolean clearDirectory(File path, boolean removeSelf) {
        if (path == null || !path.exists()) {
            return true;
        }
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (File child : files) {
                    clearDirectory(child, true);
                }
            }
        }
        if (removeSelf) {
            return path.delete();
        }
        return true;
    }

    /**
     * File转换为String。
     *
     * @param file 文件
     * @return 如果读取失败，返回null；否则返回字符串形式。
     */
    public static String file2String(File file) {
        FileInputStream fis = null;
        ByteArrayOutputStream baos = null;
        try {
            fis = new FileInputStream(file);
            baos = new ByteArrayOutputStream();
            IoUtil.copy(fis, baos);
            return baos.toString();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            IoUtil.closeSilently(fis);
            IoUtil.closeSilently(baos);
        }
        return null;
    }

    /**
     * @param path 文件路径  eg path/test.mp4 ===> test;path/test===>test
     *             path==>path;path.mp4=> path
     * @return 文件名，去除文件名后缀
     */
    public static String getAbsFileName(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        int startIndex = path.lastIndexOf("/");
        if (startIndex <= -1) {
            startIndex = -1;
        }
        String filename = path.substring(startIndex + 1, path.length());
        int lastIndex = filename.lastIndexOf(".");
        if (lastIndex <= -1) {
            return filename;
        }
        return filename.substring(0, lastIndex);
    }


    public static void makeSureFileDirExist(String downloadPath) {
        if (!TextUtils.isEmpty(downloadPath)) {
            File file = new File(downloadPath);
            file.mkdirs();
        }
    }

    public static void makeSureFileExist(File file) {
        if (!file.exists()) {
            try {
                File parentDirectory = new File(file.getParent());
                if (!parentDirectory.exists()) {
                    parentDirectory.mkdirs();
                }
                file.createNewFile();
            } catch (IOException e) {

            }
        }
    }


    public static boolean copyAssetFile(Context context, String originFileName,
                                        String destFilePath, String destFileName) {
        InputStream is = null;
        BufferedOutputStream bos = null;
        try {
            is = context.getAssets().open(originFileName);
            File destPathFile = new File(destFilePath);
            if (!destPathFile.exists()) {
                destPathFile.mkdirs();
            }

            File destFile = new File(destFilePath + File.separator + destFileName);
            if (!destFile.exists()) {
                destFile.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(destFile);
            bos = new BufferedOutputStream(fos);

            byte[] buffer = new byte[256];
            int length = 0;
            while ((length = is.read(buffer)) > 0) {
                bos.write(buffer, 0, length);
            }
            bos.flush();

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != bos) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }


    /**
     * read file
     *
     * @param filePath
     * @param charsetName The name of a supported {@link java.nio.charset.Charset </code>charset<code>}
     * @return if file not exist, return null, else return content of file
     * @throws RuntimeException if an error occurs while operator BufferedReader
     */
    public static StringBuilder readFile(String filePath, String charsetName) {
        File file = new File(filePath);
        StringBuilder fileContent = new StringBuilder("");
        if (file == null || !file.isFile()) {
            return null;
        }

        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file), charsetName);
            reader = new BufferedReader(is);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!fileContent.toString().equals("")) {
                    fileContent.append("\r\n");
                }
                fileContent.append(line);
            }
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            close(reader);
        }
    }

    public static StringBuilder readFile(InputStream inputStream, String charsetName) {
        StringBuilder fileContent = new StringBuilder("");
        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(inputStream, charsetName);
            reader = new BufferedReader(is);
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (!fileContent.toString().equals("")) {
                    fileContent.append("\r\n");
                }
                fileContent.append(line);
            }
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            close(reader);
        }
    }

    /**
     * write file
     *
     * @param filePath
     * @param content
     * @param append   is append, if true, write to the end of file, else clear content of file and write into it
     * @return return false if content is empty, true otherwise
     * @throws RuntimeException if an error occurs while operator FileWriter
     */
    public static boolean writeFile(String filePath, String content, boolean append) {
        if (StringUtils.isEmpty(content)) {
            return false;
        }

        FileWriter fileWriter = null;
        try {
            makeDirs(filePath);
            fileWriter = new FileWriter(filePath, append);
            fileWriter.write(content);
            return true;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            close(fileWriter);
        }
    }

    /**
     * write file
     *
     * @param filePath
     * @param contentList
     * @param append      is append, if true, write to the end of file, else clear content of file and write into it
     * @return return false if contentList is empty, true otherwise
     * @throws RuntimeException if an error occurs while operator FileWriter
     */
    public static boolean writeFile(String filePath, List<String> contentList, boolean append) {
        if (contentList == null || contentList.size() == 0) {
            return false;
        }

        FileWriter fileWriter = null;
        try {
            makeDirs(filePath);
            fileWriter = new FileWriter(filePath, append);
            int i = 0;
            for (String line : contentList) {
                if (i++ > 0) {
                    fileWriter.write("\r\n");
                }
                fileWriter.write(line);
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            close(fileWriter);
        }
    }

    /**
     * write file, the string will be written to the begin of the file
     *
     * @param filePath
     * @param content
     * @return
     */
    public static boolean writeFile(String filePath, String content) {
        return writeFile(filePath, content, false);
    }

    /**
     * 使用utf-8来写文件
     *
     * @param file    文件
     * @param content 内容
     * @return 成功或者失败
     */
    public static boolean writeFile(File file, String content, String charset) {
        if (StringUtils.isEmpty(content)) {
            return false;
        }

        BufferedWriter writer = null;
        try {
            makeDirs(file.getPath());
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), charset));
            writer.write(content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(writer);
        }

        return false;
    }

    /**
     * write file
     *
     * @param filePath the file to be opened for writing.
     * @param stream   the input stream
     * @param append   if <code>true</code>, then bytes will be written to the end of the file rather than the beginning
     * @return return true
     * @throws RuntimeException if an error occurs while operator FileOutputStream
     */
    public static boolean writeFile(String filePath, InputStream stream, boolean append) {
        return writeFile(filePath != null ? new File(filePath) : null, stream, append);
    }

    /**
     * write file
     *
     * @param file   the file to be opened for writing.
     * @param stream the input stream
     * @param append if <code>true</code>, then bytes will be written to the end of the file rather than the beginning
     * @return return true
     * @throws RuntimeException if an error occurs while operator FileOutputStream
     */
    public static boolean writeFile(File file, InputStream stream, boolean append) {
        OutputStream o = null;
        try {
            makeDirs(file.getAbsolutePath());
            o = new FileOutputStream(file, append);
            byte data[] = new byte[1024];
            int length = -1;
            while ((length = stream.read(data)) != -1) {
                o.write(data, 0, length);
            }
            o.flush();
            return true;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("FileNotFoundException occurred. ", e);
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            close(o);
            close(stream);
        }
    }

    /**
     * read file to string list, a element of list is a line
     *
     * @param filePath
     * @param charsetName The name of a supported {@link java.nio.charset.Charset </code>charset<code>}
     * @return if file not exist, return null, else return content of file
     * @throws RuntimeException if an error occurs while operator BufferedReader
     */
    public static List<String> readFileToList(String filePath, String charsetName) {
        File file = new File(filePath);
        List<String> fileContent = new ArrayList<String>();
        if (file == null || !file.isFile()) {
            return null;
        }

        BufferedReader reader = null;
        try {
            InputStreamReader is = new InputStreamReader(new FileInputStream(file), charsetName);
            reader = new BufferedReader(is);
            String line = null;
            while ((line = reader.readLine()) != null) {
                fileContent.add(line);
            }
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException("IOException occurred. ", e);
        } finally {
            close(reader);
        }
    }

    /**
     * get file name from path, not include suffix
     * <p>
     * <pre>
     *      getFileNameWithoutExtension(null)               =   null
     *      getFileNameWithoutExtension("")                 =   ""
     *      getFileNameWithoutExtension("   ")              =   "   "
     *      getFileNameWithoutExtension("abc")              =   "abc"
     *      getFileNameWithoutExtension("a.mp3")            =   "a"
     *      getFileNameWithoutExtension("a.b.rmvb")         =   "a.b"
     *      getFileNameWithoutExtension("c:\\")              =   ""
     *      getFileNameWithoutExtension("c:\\a")             =   "a"
     *      getFileNameWithoutExtension("c:\\a.b")           =   "a"
     *      getFileNameWithoutExtension("c:a.txt\\a")        =   "a"
     *      getFileNameWithoutExtension("/home/admin")      =   "admin"
     *      getFileNameWithoutExtension("/home/admin/a.txt/b.mp3")  =   "b"
     * </pre>
     *
     * @param filePath
     * @return file name from path, not include suffix
     * @see
     */
    public static String getFileNameWithoutExtension(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }

        int extenPosi = filePath.lastIndexOf(FILE_EXTENSION_SEPARATOR);
        int filePosi = filePath.lastIndexOf(File.separator);
        if (filePosi == -1) {
            return (extenPosi == -1 ? filePath : filePath.substring(0, extenPosi));
        }
        if (extenPosi == -1) {
            return filePath.substring(filePosi + 1);
        }
        return (filePosi < extenPosi ? filePath.substring(filePosi + 1, extenPosi) : filePath.substring(filePosi + 1));
    }

    /**
     * get file name from path, include suffix
     * <p>
     * <pre>
     *      getFileName(null)               =   null
     *      getFileName("")                 =   ""
     *      getFileName("   ")              =   "   "
     *      getFileName("a.mp3")            =   "a.mp3"
     *      getFileName("a.b.rmvb")         =   "a.b.rmvb"
     *      getFileName("abc")              =   "abc"
     *      getFileName("c:\\")              =   ""
     *      getFileName("c:\\a")             =   "a"
     *      getFileName("c:\\a.b")           =   "a.b"
     *      getFileName("c:a.txt\\a")        =   "a"
     *      getFileName("/home/admin")      =   "admin"
     *      getFileName("/home/admin/a.txt/b.mp3")  =   "b.mp3"
     * </pre>
     *
     * @param filePath
     * @return file name from path, include suffix
     */
    public static String getFileName(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }

        int filePosi = filePath.lastIndexOf(File.separator);
        return (filePosi == -1) ? filePath : filePath.substring(filePosi + 1);
    }

    /**
     * get folder name from path
     * <p>
     * <pre>
     *      getFolderName(null)               =   null
     *      getFolderName("")                 =   ""
     *      getFolderName("   ")              =   ""
     *      getFolderName("a.mp3")            =   ""
     *      getFolderName("a.b.rmvb")         =   ""
     *      getFolderName("abc")              =   ""
     *      getFolderName("c:\\")              =   "c:"
     *      getFolderName("c:\\a")             =   "c:"
     *      getFolderName("c:\\a.b")           =   "c:"
     *      getFolderName("c:a.txt\\a")        =   "c:a.txt"
     *      getFolderName("c:a\\b\\c\\d.txt")    =   "c:a\\b\\c"
     *      getFolderName("/home/admin")      =   "/home"
     *      getFolderName("/home/admin/a.txt/b.mp3")  =   "/home/admin/a.txt"
     * </pre>
     *
     * @param filePath
     * @return
     */
    public static String getFolderName(String filePath) {

        if (StringUtils.isEmpty(filePath)) {
            return filePath;
        }

        int filePosi = filePath.lastIndexOf(File.separator);
        return (filePosi == -1) ? "" : filePath.substring(0, filePosi);
    }

    /**
     * Close closable object and wrap {@link IOException} with {@link RuntimeException}
     *
     * @param closeable closeable object
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new RuntimeException("IOException occurred. ", e);
            }
        }
    }

    /**
     * 创建指定路径的目录（含多级父目录），兼容所有情况。
     * - 传入目录路径（如 /a/b/c）：创建该目录
     * - 传入文件路径（如 /a/b/c/file.txt）：创建其父目录 /a/b/c
     * 若目录已存在则直接返回；若创建失败则抛出异常。
     *
     * @param filePath 目录或文件路径，支持多级路径
     * @throws IllegalArgumentException 若 path 为空
     * @throws RuntimeException 若目录创建失败
     */
    public static void makeDirs(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            throw new IllegalArgumentException("makeDirs: path cannot be null or empty");
        }
        String normalized = filePath.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("makeDirs: path cannot be empty");
        }
        File file = new File(normalized);
        File dirToCreate;
        String name = file.getName();
        if (!name.isEmpty() && name.contains(".") && !name.equals("..") && !name.equals(".")) {
            File parent = file.getParentFile();
            dirToCreate = parent != null ? parent : file;
        } else {
            dirToCreate = file;
        }
        if (dirToCreate.exists() && dirToCreate.isDirectory()) {
            return;
        }
        if (!dirToCreate.mkdirs()) {
            throw new RuntimeException("makeDirs failed: cannot create directory: " + filePath);
        }
    }

    /**
     * //     * copy file
     * //     *
     * //     * @param sourceFilePath
     * //     * @param destFilePath
     * //     * @return
     * //     * @throws RuntimeException if an error occurs while operator FileOutputStream
     * //
     */
    public static boolean copyFile(String sourceFilePath, String destFilePath) {
        try {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(sourceFilePath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("FileNotFoundException occurred. ", e);
            }
            return writeFile(destFilePath, inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void saveBitmapToFile(String filePath, Bitmap bitmap) {
        if (TextUtils.isEmpty(filePath) || bitmap == null) return;
        try {
            File f = new File(filePath);
            File fp = f.getParentFile();
            if (!fp.exists()) {
                fp.mkdirs();
            }
            f.createNewFile();
            FileOutputStream fOut = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fOut);
            fOut.flush();
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String loadAssetDataForGson(Context context, String fileName) {
        InputStream is = null;
        try {
            is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            return new String(buffer, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IoUtil.closeQuietly(is);
        }
        return "";
    }

    public static String getFileExtension(String filePath) {
        //部分中文手机路径取出的ext为""，因此先编码
        return MimeTypeMap.getFileExtensionFromUrl(CommonTools.encodeUrl(filePath));
    }

    public static long getFileSize(File file) {
        if (file.isFile())
            return file.length();
        final File[] children = file.listFiles();
        long total = 0;
        if (children != null)
            for (final File child : children)
                total += getFileSize(child);
        return total;
    }

    public static int getFileCount(File file) {
        if (file.isFile())
            return 1;
        final File[] children = file.listFiles();
        int count = 0;
        if (children != null)
            for (final File child : children)
                count += getFileCount(child);
        return count;
    }


    public static String getFromatedStroageSize(double size) {
//        ULog.e("getFromatedStroageSize:" + size);
        if (size < 0) size = -size;
        if (size / 1024 < 1024) {
            DecimalFormat df = new DecimalFormat("####.0");
            String xs = df.format(size / 1024);
            return ((size / 1024) <= 1 ? "0" : "") + xs + "KB";
        } else if ((size / 1024 / 1024) < 1024) {
            DecimalFormat df = new DecimalFormat("####.0");
            String xs = df.format(size / 1024 / 1024);
            return ((size / 1024 / 1024) <= 1 ? "0" : "") + xs + "MB";
        } else {
            DecimalFormat df = new DecimalFormat("####.0");
            String xs = df.format(size / 1024 / 1024 / 1024);
            return ((size / 1024 / 1024 / 1024) <= 1 ? "0" : "") + xs + "GB";
        }
    }

    /**
     * 获得SD卡总大小
     *
     * @return
     */
    public static long getSDTotalSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return blockSize * totalBlocks;
    }

    /**
     * 获得sd卡剩余容量，即可用大小
     *
     * @return
     */
    public static long getSDAvailableSize() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return blockSize * availableBlocks;
    }

    public static String getFileSuffix(String pathName) {
        String suffix = "";

        if (null != pathName) {
            int lastIndexOf = pathName.lastIndexOf(".");
            if (-1 != lastIndexOf) {
                suffix = pathName.substring(lastIndexOf);
            }
        }

        return suffix;
    }

    /**
     * whether the file exist
     *
     * @param filePath the check file path
     * @return true means exist
     */
    public static boolean isFileExist(String filePath) {

        if (!isFilePath(filePath)) {
            return false;
        }

        File file = new File(filePath);
        if (file != null && file.exists() && file.isFile()) {
            return true;
        }

        return false;
    }

    public static boolean isDirectory(String path) {
        return new File(path).isDirectory();
    }

    public static boolean isFilePath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        if (path.startsWith(File.separator)) {
            return true;
        }
        return false;
    }

    /**
     * 移动文件夹
     *
     * @param resource 源路径
     * @param target   目标路径
     */
    public static void moveAllFolder(String resource, String target, final OnFileChangedListener listener) throws Exception {
        copyAllFolder(resource, target, new OnFileChangedListener() {
            @Override
            public void onChanged(File f) {
                if (listener != null)
                    listener.onChanged(f);
            }

            @Override
            public boolean isStoped() {
                if (listener != null)
                    return listener.isStoped();
                return false;
            }
        });
        clearDirectory(new File(resource), true);
    }

    public static void copyFolderTo(String srcFile, String target) throws Exception {
        File[] files = new File(srcFile).listFiles();
        for (File file : files) {
            copyAllFolder(file.getAbsolutePath(), target, null);
        }
    }

    /**
     * 复制文件夹
     *
     * @param resource 源路径
     * @param target   目标路径
     */
    public static void copyAllFolder(String resource, String target, OnFileChangedListener listener) throws Exception {

        File resourceFile = new File(resource);
        if (!resourceFile.exists()) {
            throw new Exception(GlobalApp.getString(com.hive.i8n.R.string.utils_file_source_not_exist, resource));
        }
        File targetFile = new File(target);
        if (!targetFile.exists()) {
//            throw new Exception("存放的目标路径：[" + target + "] 不存在...");
            targetFile.mkdirs();
        }

        if (resourceFile.isFile()) {
            File targetFileFinal = new File(targetFile.getPath() + File.separator + resourceFile.getName());
            copyFile(resourceFile, targetFileFinal);
            return;
        }

        // 获取源文件夹下的文件夹或文件
        File[] resourceFiles = resourceFile.listFiles();

        for (File file : resourceFiles) {
            File file1 = new File(targetFile.getAbsolutePath() + File.separator + resourceFile.getName());
            // 复制文件
            if (file.isFile()) {
                System.out.println("文件" + file.getName());
                // 在 目标文件夹（B） 中 新建 源文件夹（A），然后将文件复制到 A 中
                // 这样 在 B 中 就存在 A
                if (!file1.exists()) {
                    file1.mkdirs();
                }
                File targetFile1 = new File(file1.getAbsolutePath() + File.separator + file.getName());
                copyFile(file, targetFile1);
                if (listener != null) {
                    listener.onChanged(file);
                    if (listener.isStoped()) {
                        return;
                    }
                }
            }
            // 复制文件夹
            else if (file.isDirectory()) {// 复制源文件夹
                String dir1 = file.getAbsolutePath();
                // 目的文件夹
                String dir2 = file1.getAbsolutePath();
                copyAllFolder(dir1, dir2, listener);
            }
        }
    }

    /**
     * 复制文件
     *
     * @param resource
     * @param target
     */
    public static void copyFile(File resource, File target) throws Exception {
        // 输入流 --> 从一个目标读取数据
        // 输出流 --> 向一个目标写入数据
        if (target.exists()) return;

        long start = System.currentTimeMillis();

        // 文件输入流并进行缓冲
        FileInputStream inputStream = new FileInputStream(resource);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

        // 文件输出流并进行缓冲
        FileOutputStream outputStream = new FileOutputStream(target);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

        // 缓冲数组
        // 大文件 可将 1024 * 2 改大一些，但是 并不是越大就越快
        byte[] bytes = new byte[1024 * 2];
        int len = 0;
        while ((len = inputStream.read(bytes)) != -1) {
            bufferedOutputStream.write(bytes, 0, len);
        }
        // 刷新输出缓冲流
        bufferedOutputStream.flush();
        //关闭流
        bufferedInputStream.close();
        bufferedOutputStream.close();
        inputStream.close();
        outputStream.close();

        long end = System.currentTimeMillis();

        System.out.println("耗时：" + (end - start) / 1000 + " s");

    }


    public static List<File> listAllFiles(List<File> fileList, String path) {
        File[] allFiles = new File(path).listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            File file = allFiles[i];
            if (file.isFile()) {
                fileList.add(file);
            } else {
                listAllFiles(fileList, file.getAbsolutePath());
            }
        }
        return fileList;
    }

    /**
     * 判断是否是本地文件
     *
     * @param path
     * @return
     */
    public static boolean isLocalFile(String path) {
        if (path.startsWith("/")) return true;
        return false;
    }


    public static String getPath(final Context context, final Uri uri) {
        if (uri == null) return null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return ContentUriFileHelper.getAccessiblePath(context, uri, ".tmp");
        }

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return ContentUriFileHelper.getAccessiblePath(context, uri, ".tmp");
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static String getFilePathFromURI(Context context, Uri contentUri) {
        File rootDataDir = context.getFilesDir();
        String fileName = getFileName(contentUri);
        if (!TextUtils.isEmpty(fileName)) {
            File copyFile = new File(rootDataDir + File.separator + fileName);
            copyFile(context, contentUri, copyFile);
            return copyFile.getAbsolutePath();
        }
        return null;
    }

    private static String getFileName(Uri uri) {
        if (uri == null) return null;
        String fileName = null;
        String path = uri.getPath();
        assert path != null;
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            fileName = path.substring(cut + 1);
        }
        return fileName;
    }

    private static void copyFile(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            int i = copyStream(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int copyStream(InputStream input, OutputStream output) throws Exception {
        final int BUFFER_SIZE = 1024 * 2;
        byte[] buffer = new byte[BUFFER_SIZE];
        BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);
        BufferedOutputStream out = new BufferedOutputStream(output, BUFFER_SIZE);
        int count = 0, n = 0;
        try {
            while ((n = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                out.write(buffer, 0, n);
                count += n;
            }
            out.flush();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        final String column = "_data";
        final String[] projection = {
                column
        };
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * huoqu
     *
     * @param scrPath
     * @return
     */
    @NotNull
    public static String getLastFoldName(@NotNull String scrPath) {
        return new File(scrPath).getName();
    }

    /**
     * 将本地文件读取为字节数组
     *
     * @param filePath 文件路径
     * @return 字节数组，失败返回 null
     */
    public static byte[] readFileToBytes(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Files.readAllBytes(Paths.get(file.getPath()));
            }
            FileInputStream fis = null;
            ByteArrayOutputStream baos = null;
            try {
                fis = new FileInputStream(file);
                baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            } finally {
                IoUtil.closeQuietly(fis);
                IoUtil.closeQuietly(baos);
            }
        } catch (Exception e) {
            DLog.e("FileUtils", "readFileToBytes failed: " + filePath + ", " + e.getMessage());
            return null;
        }
    }

    /**
     * 将本地文件转换为base64
     */
    public static String convertLocalFileToBase64(String filePath, String mimeType) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                DLog.w("OpenRouterProvider", "文件不存在: " + filePath);
                return null;
            }

            byte[] bytes = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bytes = Files.readAllBytes(Paths.get(file.getPath()));
            }
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            String mime = mimeType != null ? mimeType : getMimeTypeFromFile(file);

            return "data:" + mime + ";base64," + base64;
        } catch (Exception e) {
            DLog.e("OpenRouterProvider", "转换文件到base64失败: " + filePath + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据文件扩展名获取MIME类型
     */
    public static String getMimeTypeFromFile(File file) {
        String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg"; // 默认
        };
    }
}
