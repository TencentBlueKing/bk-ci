/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.util

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("ALL")
object FileUtil {
    private const val bufferSize = 8 * 1024

    fun convertTempFile(inputStream: InputStream): File {
        val logo = Files.createTempFile("default_", ".png").toFile()

        logo.outputStream().use {
            inputStream.copyTo(it)
        }

        return logo
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

    /**
     * 获取文件内容的MD5值
     * @param content 文件内容
     * @return 文件MD5值
     */
    fun getMD5(content: String): String {
        return DigestUtils.md5Hex(content)
    }

    /**
     * 获取文件内容的MD5值
     * @param bytes 文件字节
     * @return 文件MD5值
     */
    fun getMD5(bytes: ByteArray): String {
        return DigestUtils.md5Hex(bytes)
    }

    /**
     * zip文件到当前路径
     * @param file 文件对象
     * @return zip文件
     */
    fun zipToCurrentPath(file: File): File {
        val dest = File(file.canonicalPath + ".zip")
        val sourcePath = Paths.get(file.canonicalPath)
        ZipOutputStream(FileOutputStream(dest)).use { zos ->
            val bufSize = 4096
            val buf = ByteArray(bufSize)
            file.walk().filter { return@filter it.isFile }.forEach {
                val relativePath = sourcePath.relativize(Paths.get(it.canonicalPath)).toString()
                zos.putNextEntry(ZipEntry(relativePath))
                FileInputStream(it).use {
                    var len = it.read(buf)
                    while (len != -1) {
                        zos.write(buf, 0, len)
                        len = it.read(buf)
                    }
                }
                zos.closeEntry()
            }
        }
        return dest
    }

    fun matchFiles(workspace: File, filePath: String): List<File> {
        // 斜杠开头的，绝对路径
        val absPath = filePath.startsWith("/") || (filePath[0].isLetter() && filePath[1] == ':')

        val fileList: List<File>
        // 文件夹返回所有文件
        if (filePath.endsWith("/")) {
            // 绝对路径
            fileList = if (absPath) {
                File(filePath).listFiles()?.filter { return@filter it.isFile }?.toList() ?: mutableListOf()
            } else {
                File(workspace, filePath).listFiles()?.filter { return@filter it.isFile }?.toList() ?: mutableListOf()
            }
        } else {
            // 相对路径
            // get start path
            val file = File(filePath)
            val startPath = if (file.parent.isNullOrBlank()) "." else file.parent
            val regexPath = file.name

            // return result
            val pattern = Pattern.compile(transfer(regexPath))
            val startFile = if (absPath) File(startPath) else File(workspace, startPath)
            val path = Paths.get(startFile.canonicalPath)
            fileList = startFile.listFiles()?.filter {
                val rePath = path.relativize(Paths.get(it.canonicalPath)).toString()
                it.isFile && pattern.matcher(rePath).matches()
            }?.toList() ?: listOf()
        }
        val resultList = mutableListOf<File>()
        fileList.forEach { f ->
            // 文件名称不允许带有空格
            if (!f.name.contains(" ")) {
                resultList.add(f)
            }
        }
        return resultList
    }

    fun unzipTgzFile(tgzFile: String, destDir: String = "./") {
        val blockSize = 4096
        val inputStream = TarArchiveInputStream(GzipCompressorInputStream(File(tgzFile).inputStream()), blockSize)
        while (true) {
            val entry = inputStream.nextTarEntry ?: break
            if (entry.isDirectory) { // 是目录
                val dir = File(destDir, entry.name)
                if (!dir.exists()) dir.mkdirs()
            } else { // 是文件
                File(destDir, entry.name).outputStream().use { outputStream ->
                    while (true) {
                        val buf = ByteArray(4096)
                        val len = inputStream.read(buf)
                        if (len == -1) break
                        outputStream.write(buf, 0, len)
                    }
                }
            }
        }
    }

    fun unzipFile(zipFile: String, destDir: String = "./") {
        val blockSize = 4096
        val inputStream = ZipArchiveInputStream(BufferedInputStream(FileInputStream(File(zipFile)), blockSize))

        while (true) {
            val entry = inputStream.nextZipEntry ?: break
            if (entry.isDirectory) { // 是目录
                val dir = File(destDir, entry.name)
                if (!dir.exists()) dir.mkdirs()
            } else { // 是文件
                File(destDir, entry.name).outputStream().use { outputStream ->
                    while (true) {
                        val buf = ByteArray(4096)
                        val len = inputStream.read(buf)
                        if (len == -1) break
                        outputStream.write(buf, 0, len)
                    }
                }
            }
        }
    }

    fun getSHA1(file: File): String {
        if (!file.exists()) return ""
        return DigestUtils.sha1Hex(file.inputStream())
    }

    private fun transfer(regexPath: String): String {
        var resultPath = regexPath
        resultPath = resultPath.replace(".", "\\.")
        resultPath = resultPath.replace("*", ".*")
        return resultPath
    }

    /**
     * 获取文件MD5值
     * @param file 文件对象
     * @return 文件MD5值
     */
    fun copyAndGetMD5(
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
    fun mkdirs(dir: File, delete: Boolean = true) {
        if (!dir.exists()) {
            dir.mkdirs()
        } else {
            if (delete || dir.isFile) {
                dir.deleteRecursively()
                dir.mkdirs()
            }
        }
    }

    /**
     * 写文件
     */
    fun outFile(path: String, name: String, context: String) {
        val inPath = File(path)
        if (!inPath.exists()) {
            inPath.mkdirs()
        }
        val file = File("$path/$name")
        file.bufferedWriter().use { out ->
            out.write(context)
        }
    }
}
