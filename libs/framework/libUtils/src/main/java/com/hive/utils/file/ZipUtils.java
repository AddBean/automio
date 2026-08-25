// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.file;

import com.hive.utils.io.IoUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Java utils 实现的Zip工具
 *
 * @author once
 */
public class ZipUtils {
    private static final int BUFF_SIZE = 1024 * 1024; // 1M Byte

    /**
     * 批量压缩文件（夹）
     *
     * @param resFileList 要压缩的文件（夹）列表
     * @param zipFile     生成的压缩文件
     * @throws IOException 当压缩过程出错时抛出
     */
    public static void zipFiles(Collection<File> resFileList, File zipFile, OnFileChangedListener listener) throws IOException {
        ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
                zipFile), BUFF_SIZE));
        for (File resFile : resFileList) {
            zipFile(resFile, zipout, "", listener);
        }
        zipout.close();
    }

    /**
     * 批量压缩文件（夹）
     *
     * @param resFileList 要压缩的文件（夹）列表
     * @param zipFile     生成的压缩文件
     * @param comment     压缩文件的注释
     * @throws IOException 当压缩过程出错时抛出
     */
    public static void zipFiles(Collection<File> resFileList, File zipFile, String comment, OnFileChangedListener listener)
            throws IOException {
        ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(
                zipFile), BUFF_SIZE));
        for (File resFile : resFileList) {
            zipFile(resFile, zipout, "", listener);
        }
        zipout.setComment(comment);
        zipout.close();
    }

    /**
     * 压缩文件
     *
     * @param resFile  需要压缩的文件（夹）
     * @param zipout   压缩的目的文件
     * @param rootpath 压缩的文件路径
     * @throws FileNotFoundException 找不到文件时抛出
     * @throws IOException           当压缩过程出错时抛出
     */
    private static boolean zipFile(File resFile, ZipOutputStream zipout, String rootpath, OnFileChangedListener listener)
            throws FileNotFoundException, IOException {
        if (listener != null) {
            if (listener.isStoped()) {
                return false;
            }
        }
        rootpath = rootpath + (rootpath.trim().length() == 0 ? "" : File.separator)
                + resFile.getName();
//        rootpath = new String(rootpath.getBytes(), "GBK");
        rootpath = new String(rootpath.getBytes("UTF8"), "UTF8");

        if (resFile.isDirectory()) {
            File[] fileList = resFile.listFiles();
            for (File file : fileList) {
                zipFile(file, zipout, rootpath, listener);
            }
        } else {
            byte buffer[] = new byte[BUFF_SIZE];
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(resFile),
                    BUFF_SIZE);
            zipout.putNextEntry(new ZipEntry(rootpath));
            int realLength;
            while ((realLength = in.read(buffer)) != -1) {
                zipout.write(buffer, 0, realLength);
            }
            in.close();
            zipout.flush();
            zipout.closeEntry();
            if (listener != null) {
                if (listener.isStoped()) {
                    return false;
                }
                listener.onChanged(resFile);
            }
        }
        return true;
    }

    /**
     * 查找一个文件
     *
     * @param zipFile    压缩文件
     * @throws IOException 当解压缩过程出错时抛出
     */
    public static void traversalZipFile(File zipFile, OnZipEntryTraversalListener listener) throws IOException {
        FileOutputStream fileOut = null;
        ZipInputStream zipIn = null;
        ZipEntry zipEntry = null;
        try {
            zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    if(listener!=null){
                        listener.onTraversal(zipEntry,zipIn);
                    }
                }
            }
        } catch (Exception ioe) {
            ioe.printStackTrace();

        } finally {
            IoUtil.closeQuietly(fileOut);
            if (null != zipIn) {
                try {
                    zipIn.closeEntry();
                } catch (IOException e) {

                }
                try {
                    zipIn.close();
                } catch (IOException e) {

                }
            }
        }
    }

    /**
     * 解压缩一个文件
     *
     * @param zipFile    压缩文件
     * @param mDestPath 解压缩的目标目录
     * @throws IOException 当解压缩过程出错时抛出
     */
    public static boolean startUnzipFiles(File zipFile, String mDestPath, OnFileChangedListener listener) throws ZipException, IOException {
        if (!mDestPath.endsWith("/")) {
            mDestPath = mDestPath + "/";
        }
        FileOutputStream fileOut = null;
        ZipInputStream zipIn = null;
        ZipEntry zipEntry = null;
        File file = null;
        int readedBytes = 0;
        byte buf[] = new byte[4096];
        try {
            zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            while ((zipEntry = zipIn.getNextEntry()) != null) {
                file = new File(mDestPath + zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    file.mkdirs();
                } else {
                    // 如果指定文件的目录不存在,则创建之.
                    File parent = file.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    fileOut = new FileOutputStream(file);
                    while ((readedBytes = zipIn.read(buf)) > 0) {
                        fileOut.write(buf, 0, readedBytes);
                    }
                    fileOut.close();
                    if(listener!=null){
                        if(listener.isStoped()){
                            return false;
                        }
                        listener.onChanged(file);
                    }
                }
            }

            return true;
        } catch (Exception ioe) {
            ioe.printStackTrace();

        } finally {

            IoUtil.closeQuietly(fileOut);

            if (null != zipIn) {
                try {
                    zipIn.closeEntry();
                } catch (IOException e) {

                }
                try {
                    zipIn.close();
                } catch (IOException e) {

                }
            }
        }
        return false;
    }

    /**
     * 解压文件名包含传入文字的文件
     *
     * @param zipFile      压缩文件
     * @param folderPath   目标文件夹
     * @param nameContains 传入的文件匹配名
     * @throws ZipException 压缩格式有误时抛出
     * @throws IOException  IO错误时抛出
     */
    public static ArrayList<File> upZipSelectedFile(File zipFile, String folderPath,
                                                    String nameContains) throws ZipException, IOException {
        ArrayList<File> fileList = new ArrayList<File>();

        File desDir = new File(folderPath);
        if (!desDir.exists()) {
            desDir.mkdir();
        }
        ZipFile zf = new ZipFile(zipFile);
        for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = ((ZipEntry) entries.nextElement());
            if (entry.getName().contains(nameContains)) {
                InputStream in = zf.getInputStream(entry);
                String str = folderPath + File.separator + entry.getName();
                str = new String(str.getBytes("8859_1"), "GB2312");
                // str.getBytes("GB2312"),"8859_1" 输出
                // str.getBytes("8859_1"),"GB2312" 输入
                File desFile = new File(str);
                if (!desFile.exists()) {
                    File fileParentDir = desFile.getParentFile();
                    if (!fileParentDir.exists()) {
                        fileParentDir.mkdirs();
                    }
                    desFile.createNewFile();
                }
                OutputStream out = new FileOutputStream(desFile);
                byte buffer[] = new byte[BUFF_SIZE];
                int realLength;
                while ((realLength = in.read(buffer)) > 0) {
                    out.write(buffer, 0, realLength);
                }
                in.close();
                out.close();
                fileList.add(desFile);
            }
        }
        return fileList;
    }

    /**
     * 获得压缩文件内文件列表
     *
     * @param zipFile 压缩文件
     * @return 压缩文件内文件名称
     * @throws ZipException 压缩文件格式有误时抛出
     * @throws IOException  当解压缩过程出错时抛出
     */
    public static ArrayList<String> getEntriesNames(File zipFile) throws ZipException, IOException {
        ArrayList<String> entryNames = new ArrayList<String>();
        Enumeration<?> entries = getEntriesEnumeration(zipFile);
        while (entries.hasMoreElements()) {
            ZipEntry entry = ((ZipEntry) entries.nextElement());
            entryNames.add(new String(getEntryName(entry).getBytes("GB2312"), "8859_1"));
        }
        return entryNames;
    }

    /**
     * 获得压缩文件内压缩文件对象以取得其属性
     *
     * @param zipFile 压缩文件
     * @return 返回一个压缩文件列表
     * @throws ZipException 压缩文件格式有误时抛出
     * @throws IOException  IO操作有误时抛出
     */
    public static Enumeration<?> getEntriesEnumeration(File zipFile) throws ZipException,
            IOException {
        ZipFile zf = new ZipFile(zipFile);
        return zf.entries();
    }

    /**
     * 取得压缩文件对象的注释
     *
     * @param entry 压缩文件对象
     * @return 压缩文件对象的注释
     * @throws UnsupportedEncodingException
     */
    public static String getEntryComment(ZipEntry entry) throws UnsupportedEncodingException {
        return new String(entry.getComment().getBytes("GB2312"), "8859_1");
    }

    /**
     * 取得压缩文件对象的名称
     *
     * @param entry 压缩文件对象
     * @return 压缩文件对象的名称
     * @throws UnsupportedEncodingException
     */
    public static String getEntryName(ZipEntry entry) throws UnsupportedEncodingException {
        return new String(entry.getName().getBytes("GB2312"), "8859_1");
    }


    public interface OnZipEntryTraversalListener{
        void onTraversal(ZipEntry entry, ZipInputStream zipInputStream);
    }
}

