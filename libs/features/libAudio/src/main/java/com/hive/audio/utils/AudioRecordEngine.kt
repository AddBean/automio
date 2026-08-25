package com.hive.audio.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class AudioRecordEngine(val saveName: String?) {

    private var outputStream: FileOutputStream? = null
    private val tempFilePath = "$saveName.temp"
    private val isInitialized = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    val sampleRateInHz = 16000
    val channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO
    val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    var recBufSize = 0
    var playBufSize = 0


    init {
        recBufSize = AudioRecord.getMinBufferSize(
            sampleRateInHz, channelConfiguration, audioEncoding
        )

        playBufSize = AudioTrack.getMinBufferSize(
            sampleRateInHz, channelConfiguration, audioEncoding
        )
        // 延迟文件创建，避免在主线程进行磁盘操作
        // 文件将在第一次 write 时创建
    }
    
    /**
     * 确保输出流已初始化
     * 延迟初始化以避免在主线程进行磁盘操作
     */
    private fun ensureOutputStreamInitialized() {
        if (!isInitialized.get()) {
            synchronized(this) {
                if (!isInitialized.get()) {
                    try {
                        outputStream = FileOutputStream(tempFilePath)
                        isInitialized.set(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw RuntimeException("Cannot create AudioRecord", e)
                    }
                }
            }
        }
    }

    fun write(b: ByteArray) {
        ensureOutputStreamInitialized()
        outputStream?.write(b)
    }

    fun finishRecording() {
        copyWaveFile(tempFilePath, saveName ?: "")
        File(tempFilePath).delete()
        outputStream?.close()
    }

    private fun copyWaveFile(inFilename: String, outFilename: String) {
        var inputStream: FileInputStream? = null
        var outputStream: FileOutputStream? = null
        var totalAudioLen: Long = 0
        val longSampleRate = sampleRateInHz.toLong()
        val channels = 1
        val byteRate: Long = 16 * sampleRateInHz.toLong() * channels / 8
        val data = ByteArray(recBufSize)
        try {
            inputStream = FileInputStream(inFilename)
            outputStream = FileOutputStream(outFilename)
            totalAudioLen = inputStream.channel.size()
            val totalDataLen = totalAudioLen + 36
            writeWaveFileHeader(
                outputStream, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate
            )
            while (inputStream.read(data) != -1) {
                outputStream.write(data)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 添加头信息，否则无法播放。
     */
    @Throws(IOException::class)
    private fun writeWaveFileHeader(
        out: FileOutputStream,
        totalAudioLen: Long,
        totalDataLen: Long,
        longSampleRate: Long,
        channels: Int,
        byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = (totalDataLen shr 8 and 0xffL).toByte()
        header[6] = (totalDataLen shr 16 and 0xffL).toByte()
        header[7] = (totalDataLen shr 24 and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xffL).toByte()
        header[25] = (longSampleRate shr 8 and 0xffL).toByte()
        header[26] = (longSampleRate shr 16 and 0xffL).toByte()
        header[27] = (longSampleRate shr 24 and 0xffL).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = (byteRate shr 8 and 0xffL).toByte()
        header[30] = (byteRate shr 16 and 0xffL).toByte()
        header[31] = (byteRate shr 24 and 0xffL).toByte()
        header[32] = (2 * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = (totalAudioLen shr 8 and 0xffL).toByte()
        header[42] = (totalAudioLen shr 16 and 0xffL).toByte()
        header[43] = (totalAudioLen shr 24 and 0xffL).toByte()
        out.write(header, 0, 44)
    }

    fun destroy() {
        outputStream?.close()
    }

}