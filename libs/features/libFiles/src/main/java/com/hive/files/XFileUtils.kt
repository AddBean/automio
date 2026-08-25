// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.files

import com.hive.files.model.FileCardData
import com.hive.files.model.FileDetailData
import com.hive.files.model.XFileSetting
import com.hive.files.utils.XMediaStoreHelper
import com.hive.libfiles.R
import com.hive.utils.BaseConst
import com.hive.utils.file.MediaFileUtil
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File

/**
 * @author jiadou
 * @date 4/7/21
 */
object XFileUtils {


    fun listVideoFolder(file: FileCardData): Observable<MutableList<FileCardData>> {
        val m = arrayOf(file.newFile())
        var observable = Observable.fromArray(*m).flatMap { file ->
            listAllFiles(FileCardData.parseFile(file)) {
                checkFileLegal(it) && MediaFileUtil.isVideoFileType(it.path) && it.parent != null
            }
        }
        return observable.collectInto(hashSetOf<String>(), { t1, t2 -> t1?.add(t2.newFile().parent) }).map {
            mutableListOf<FileCardData>().apply {
                it.forEach {
                    add(FileCardData.parsePath(it))
                }
            }
        }.cache().subscribeOn(Schedulers.io()).toObservable()
    }


    fun listFoldFolds(file: FileCardData): Observable<MutableList<FileCardData>> {
        val m = arrayOf(file)
        var observable = Observable.fromArray(*m).flatMap { file -> listFolds(file) }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<FileCardData>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listFoldFiles(file: FileCardData): Observable<MutableList<FileCardData>> {
        val m = arrayOf(file)
        var observable = Observable.fromArray(*m).flatMap { file -> listFiles(file) }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<FileCardData>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }


    fun listFolds(f: FileCardData): Observable<FileCardData> {
        return if (f.isDir) {
            Observable.fromArray(*f.newFile().listFiles()).filter { checkFileLegal(it) && it.isDirectory }.map {
                FileCardData.parseFile(it)
            }
        } else {
            Observable.just(f).filter { checkFileLegal(it.newFile()) && it.isDir }
        }
    }

    fun listFiles(f: FileCardData): Observable<FileCardData> {
        return if (f.isDir) {
            var ls = f.newFile().listFiles()
            if (ls == null) {
                return Observable.just(f).filter { checkFileLegal(it.newFile()) }
            }
            Observable.fromArray(*ls).filter { checkFileLegal(it) }.map {
                FileCardData.parseFile(it)
            }
        } else {
            Observable.just(f).filter { checkFileLegal(it.newFile()) }
        }
    }

    fun listAllFiles(f: FileCardData, condition: (f: File) -> Boolean): Observable<FileCardData> {
        return if (f.isDir) {
            Observable.fromArray(*f.newFile().listFiles()).flatMap { file -> listAllFiles(FileCardData.parseFile(file), condition) }
        } else {
            Observable.just(f).filter { condition.invoke(f.newFile()) }
        }
    }


    fun listAllFiles2(f: FileCardData, condition: (f: File) -> Boolean, callback: (f: FileCardData) -> Unit?) {
        if (f.isDir) {
            f?.newFile()?.listFiles()?.filter { condition.invoke(it) }?.forEach {
                listAllFiles2(FileCardData.parseFile(it), condition, callback)
            }
        } else {
            callback.invoke(f)
        }
    }

    /**
     * 可读、非回收站、存在的合法文件
     */
    fun checkFileLegal(f: File): Boolean {
        return f.exists()
                && f.canRead()
                && (XFileSetting.instance.showHiddenFile || !f.isHidden)
                && !isFromRecyclerBin(f)
                && (XFileSetting.instance.showHiddenFile || (!f.name.startsWith(".", true)))
    }

    /**
     * 是否来自回收站
     */
    fun isFromRecyclerBin(f: File): Boolean {
        return f.path.contains(BaseConst.getRecyclerBinPath())
    }

    fun getFileResId(f: File): Int {
        return getFileResId(f.path)
    }


    fun listApkFromMediaStore(): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryApk(list)
            list = list.filter { checkFileLegal(it.file) && it.file.length() > 0 }.toMutableList()

            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listZipFromMediaStore(): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryZip(list)
            list = list.filter { checkFileLegal(it.file) && it.file.length() > 0 }.toMutableList()

            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listTorrrentFromMediaStore(): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryTorrent(list)
            list = list.filter { checkFileLegal(it.file) && it.file.length() > 0 }.toMutableList()

            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listImagesFromMediaStore(): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryImage(list)
            list = list.filter { checkFileLegal(it.file) && it.file.length() > 0 }.toMutableList()

            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listFilesFromMediaStore(page: Int, pageSize: Int): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryFiles(page, pageSize, list)
            list = list.filter { checkFileLegal(it.file) && !it.file.isDirectory }.toMutableList()
            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listVideosFromMediaStore(): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryVideo(list);
            list = list.filter { checkFileLegal(it.file) }.toMutableList()
            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listAudiosFromMediaStore(): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryAudio(list)
            list = list.filter { checkFileLegal(it.file) }.toMutableList()
            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }

    fun listRecyclerBin(): Observable<MutableList<File>> {
        var observable = Observable.create<MutableList<File>> { ob ->
            var binDir = File(BaseConst.getRecyclerBinPath())
            ob.onNext(binDir.listFiles().toMutableList())
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable
    }

    fun listDocFromMediaStore(): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.queryDocs(list)
            list = list.filter { checkFileLegal(it.file) }.toMutableList()
            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }


    fun listSearchFromMediaStore(keyword: String): Observable<MutableList<File>> {
        var observable = Observable.create<File> { ob ->
            var list = mutableListOf<XMediaStoreHelper.MediaFile>()
            XMediaStoreHelper.querySearch(keyword, list)
            list = list.filter { checkFileLegal(it.file) && !it.file.isDirectory }.toMutableList()
            list?.forEach {
                ob.onNext(it.file)
            }
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable.collectInto<MutableList<File>>(mutableListOf(), { t1, t2 -> t1?.add(t2) }).toObservable()
    }


    fun getFileInfoMap(file: File): Observable<FileDetailData> {
        var observable = Observable.create<FileDetailData> { ob ->
            ob.onNext(FileDetailData.parseFile(file))
            ob.onComplete()
        }.subscribeOn(Schedulers.io())
        return observable
    }


    fun getFileResId(fileType: Int): Int {
        var file = MediaFileUtil.getMediaFileByFileType(fileType)
        return getFileResId(file)
    }


    fun getFileResId(filePath: String): Int {
        var fileType = MediaFileUtil.getFileType(filePath)
        return getFileResId(fileType)
    }

    fun getFileResId(fileType: MediaFileUtil.MediaFileType): Int {
        return when (fileType?.fileType) {
            MediaFileUtil.FILE_TYPE_FOLDER -> R.drawable.file_icon_folder
            MediaFileUtil.FILE_TYPE_UNKOWN -> R.drawable.file_icon_default
            MediaFileUtil.FILE_TYPE_3GPP -> R.drawable.file_icon_3gpp
            MediaFileUtil.FILE_TYPE_3GPP2 -> R.drawable.file_icon_3gpp
            MediaFileUtil.FILE_TYPE_AAC -> R.drawable.file_icon_aac
            MediaFileUtil.FILE_TYPE_AMR -> R.drawable.file_icon_amr
            MediaFileUtil.FILE_TYPE_APE -> R.drawable.file_icon_ape
            MediaFileUtil.FILE_TYPE_APK -> R.drawable.file_icon_apk

            MediaFileUtil.FILE_TYPE_BKP -> R.drawable.file_icon_backup
            MediaFileUtil.FILE_TYPE_DOC -> R.drawable.file_icon_doc
            MediaFileUtil.FILE_TYPE_DPS -> R.drawable.file_icon_dps
            MediaFileUtil.FILE_TYPE_DPT -> R.drawable.file_icon_dpt
            MediaFileUtil.FILE_TYPE_ET -> R.drawable.file_icon_et
            MediaFileUtil.FILE_TYPE_ETT -> R.drawable.file_icon_ett
            MediaFileUtil.FILE_TYPE_FLAC -> R.drawable.file_icon_flac
            MediaFileUtil.FILE_TYPE_HTML -> R.drawable.file_icon_html
            MediaFileUtil.FILE_TYPE_M4A -> R.drawable.file_icon_m4a
            MediaFileUtil.FILE_TYPE_MID -> R.drawable.file_icon_mid
            MediaFileUtil.FILE_TYPE_MP3 -> R.drawable.file_icon_mp3
            MediaFileUtil.FILE_TYPE_OGG -> R.drawable.file_icon_ogg
            MediaFileUtil.FILE_TYPE_PDF -> R.drawable.file_icon_pdf

            MediaFileUtil.FILE_TYPE_PPS -> R.drawable.file_icon_pps
            MediaFileUtil.FILE_TYPE_PPT -> R.drawable.file_icon_ppt
            MediaFileUtil.FILE_TYPE_RAR -> R.drawable.file_icon_rar
            MediaFileUtil.FILE_TYPE_THEME -> R.drawable.file_icon_theme
            MediaFileUtil.FILE_TYPE_TXT -> R.drawable.file_icon_txt
            MediaFileUtil.FILE_TYPE_VCF -> R.drawable.file_icon_vcf


            MediaFileUtil.FILE_TYPE_WAV -> R.drawable.file_icon_wav
            MediaFileUtil.FILE_TYPE_WMG -> R.drawable.file_icon_wma
            MediaFileUtil.FILE_TYPE_WPS -> R.drawable.file_icon_wps
            MediaFileUtil.FILE_TYPE_WPT -> R.drawable.file_icon_wpt
            MediaFileUtil.FILE_TYPE_XLS -> R.drawable.file_icon_xls
            MediaFileUtil.FILE_TYPE_XML -> R.drawable.file_icon_xml
            MediaFileUtil.FILE_TYPE_ZIP -> R.drawable.file_icon_zip
            MediaFileUtil.FILE_TYPE_3GPP -> R.drawable.file_icon_video
            MediaFileUtil.FILE_TYPE_LOG -> R.drawable.file_icon_txt
            MediaFileUtil.FILE_TYPE_TORRENT -> R.drawable.file_icon_bt

            else -> {
                when {
                    MediaFileUtil.isDocFileType(fileType!!.fileType) -> {
                        R.drawable.file_icon_txt
                    }
                    MediaFileUtil.isVideoFileType(fileType!!.fileType) -> {
                        R.drawable.file_icon_video
                    }
                    MediaFileUtil.isImageFileType(fileType!!.fileType) -> {
                        R.drawable.file_icon_picture
                    }
                    else -> {
                        R.drawable.file_icon_default
                    }
                }

            }

        }
    }

}