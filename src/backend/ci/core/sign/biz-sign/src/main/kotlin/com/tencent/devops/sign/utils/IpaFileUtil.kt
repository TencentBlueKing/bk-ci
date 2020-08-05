package com.tencent.devops.sign.utils

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

object IpaFileUtil {
    private val bufferSize = 8 * 1024

    /*
    * 复制流到目标文件，并计算md5
    * */
    fun copyInputStreamToFile(
        inputStream: InputStream,
        target: File
    ): String? {
        var outputStream: OutputStream? = null
        try {
            outputStream = target.outputStream()
            val md5 = MessageDigest.getInstance("MD5")
            var bytesCopied: Long = 0
            val buffer = ByteArray(bufferSize)
            var bytes = inputStream.read(buffer)
            while (bytes >= 0) {
                outputStream.write(buffer, 0, bytes)
                md5.update(buffer, 0, bytes)
                bytesCopied += bytes
                bytes = inputStream.read(buffer)
            }
            return Hex.encodeHexString(md5.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /*
    *  创建目录
    * */
    fun mkdirs(dir: File): Boolean {
        if (!dir.exists()) {
            dir.mkdirs()
            return true
        }
        if (dir.exists() && !dir.isDirectory) {
            dir.deleteOnExit()
            dir.mkdirs()
            return true
        }
        if (dir.exists() && dir.isDirectory && !dir.canWrite()) {
            dir.deleteOnExit()
            dir.mkdirs()
            return true
        }
        return true
    }

    /**
     * 获取文件MD5值
     * @param file 文件对象
     * @return 文件MD5值
     */
    fun getMD5(file: File): String {
        if (!file.exists()) return ""
        return DigestUtils.md5Hex(file.inputStream())
    }
}