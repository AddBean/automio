// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;

import android.net.Uri;
import android.text.TextUtils;

import com.hive.base.image.ImageUploadCompressor;
import com.hive.exception.BaseException;
import com.hive.net.resp.UploadResp;
import com.hive.net.upload.FormFile;
import com.hive.net.upload.HttpFilePostRequester;
import com.hive.net.upload.IUploadListener;
import com.hive.utils.GlobalApp;
import com.hive.utils.debug.DLog;
import com.hive.utils.file.ContentUriFileHelper;
import com.hive.utils.file.FileUtils;
import com.hive.utils.system.CommonUtils;
import com.hive.utils.utils.GsonHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BaseHttpService {

    public static void postFiles(String cookie, Map<String, String> maps, List<FormFile> files, IUploadListener mIUploadListener) {
        // 与 HiveAi ApiUploadController：POST /api/v1/uploads/raw（原 api/upload/doUpload.do 已废弃）
        String url = ApiDnsManager.getDataDomain() + "api/v1/uploads/raw";
        HttpFilePostRequester.getInstance().post(url, cookie, maps, files, mIUploadListener);
    }

    public static void postFiles(final String path, String cookie, Map<String, String> maps, List<FormFile> files, IUploadListener mIUploadListener) {
        HttpFilePostRequester.getInstance().post(path, cookie, maps, files, mIUploadListener);
    }

    /**
     * 上传图片
     *
     * @param maps
     * @param size           kb单位
     * @param uri
     * @param onHttpListener
     */
    public static void uploadImage(final Map<String, String> maps, final double newWidth, final int size, Uri uri, final OnHttpListener<String> onHttpListener) {

        final File sourceFile = copyUriToCache(uri);
        if (sourceFile == null || !sourceFile.exists()) {
            if (onHttpListener != null) {
                onHttpListener.onError(new BaseException(GlobalApp.getString(com.hive.i8n.R.string.base_upload_error)));
            }
            return;
        }
        Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> emitter) throws Exception {
                ImageUploadCompressor.Spec spec = ImageUploadCompressor.specFromLegacyParams(newWidth, size);
                ImageUploadCompressor.Result compressed = ImageUploadCompressor.compress(sourceFile, spec);
                DLog.e("uploadImage before=" + (sourceFile.length() / 1024) + "KB after=" + (compressed.getFile().length() / 1024) + "KB");
                emitter.onNext(compressed.getFile());
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        List<FormFile> files = new ArrayList<>();
                        String filePath = file.getPath();
                        String name = CommonUtils.getRandomName() + filePath.substring(filePath.lastIndexOf("."), filePath.length());
                        final File tempFile = new File(filePath);
                        files.add(new FormFile(name, tempFile, "files"));
                        BaseHttpService.postFiles("", maps, files, new IUploadListener() {
                            @Override
                            public void onAllUploadSuccess(String content) {
                                try {
                                    FileUtils.deleteFile(sourceFile);
                                    FileUtils.deleteFile(tempFile);
                                    if (onHttpListener != null)
                                        onHttpListener.onSuccess(content);
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                    if (onHttpListener != null)
                                        onHttpListener.onFailure(throwable);
                                }
                            }

                            @Override
                            public void onAllUploadFailed(String msg) {
                                super.onAllUploadFailed(msg);
                                FileUtils.deleteFile(sourceFile);
                                FileUtils.deleteFile(tempFile);
                                if (onHttpListener != null)
                                    onHttpListener.onFailure(new BaseException(msg));
                            }
                        });
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        FileUtils.deleteFile(sourceFile);
                        if (onHttpListener != null)
                            onHttpListener.onError(throwable);
                    }
                });
    }

    private static File copyUriToCache(Uri uri) {
        return ContentUriFileHelper.copyToCache(GlobalApp.sContext, uri, ".jpg");
    }


    public static void uploadImageDefault(Uri uri, final OnHttpListener<UploadResp> onHttpListener) {
        final File sourceFile = copyUriToCache(uri);
        if (sourceFile == null || !sourceFile.exists()) {
            if (onHttpListener != null) {
                onHttpListener.onError(new BaseException(GlobalApp.getString(com.hive.i8n.R.string.base_upload_error)));
            }
            return;
        }
        Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> emitter) throws Exception {
                ImageUploadCompressor.Result compressed = ImageUploadCompressor.compress(
                        sourceFile,
                        ImageUploadCompressor.specFor(ImageUploadCompressor.Preset.LEGACY_DEFAULT)
                );
                DLog.e("uploadImageDefault before=" + (sourceFile.length() / 1024) + "KB after=" + (compressed.getFile().length() / 1024) + "KB");
                emitter.onNext(compressed.getFile());
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        List<FormFile> files = new ArrayList<>();
                        String filePath = file.getPath();
                        String name = CommonUtils.getRandomName() + filePath.substring(filePath.lastIndexOf("."), filePath.length());
                        final File tempFile = new File(filePath);
                        files.add(new FormFile(name, tempFile, "files"));
                        BaseHttpService.postFiles("", null, files, new IUploadListener() {
                            @Override
                            public void onAllUploadSuccess(String data) {
                                try {
                                    FileUtils.deleteFile(sourceFile);
                                    FileUtils.deleteFile(tempFile);
                                    if (TextUtils.isEmpty(data)) {
                                        onHttpListener.onFailure(new BaseException(GlobalApp.getString(com.hive.i8n.R.string.base_upload_error)));
                                        return;
                                    }
                                    UploadResp resp = GsonHelper.getInstance().fromJson(data, UploadResp.class);
                                    onHttpListener.onSuccess(resp);
                                } catch (Throwable throwable) {
                                    throwable.printStackTrace();
                                    if (onHttpListener != null) {
                                        onHttpListener.onFailure(throwable);
                                    }
                                }
                            }

                            @Override
                            public void onAllUploadFailed(String msg) {
                                super.onAllUploadFailed(msg);
                                FileUtils.deleteFile(sourceFile);
                                FileUtils.deleteFile(tempFile);
                                if (onHttpListener != null) {
                                    onHttpListener.onFailure(new BaseException(msg));
                                }
                            }
                        });
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        FileUtils.deleteFile(sourceFile);
                        if (onHttpListener != null) {
                            onHttpListener.onError(throwable);
                        }
                    }
                });
    }

}
