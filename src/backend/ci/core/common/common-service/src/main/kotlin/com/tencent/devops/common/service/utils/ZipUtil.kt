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

package com.tencent.devops.common.service.utils

import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.ws.rs.NotFoundException

@Suppress("ALL")
object ZipUtil {

    private val logger = LoggerFactory.getLogger(ZipUtil::class.java)

    private const val BUFFER_SIZE = 1024 // 缓冲区大小

    fun unZipFile(srcFile: File, destDirPath: String, createRootDirFlag: Boolean? = true) {
        // 判断源文件是否存在
        if (!srcFile.exists()) {
            throw NotFoundException(srcFile.path + "is not exist")
        }
        // 开始解压
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(srcFile, Charset.forName("UTF-8"))
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement() as ZipEntry
                var entryName = entry.name
                if (null != createRootDirFlag && !createRootDirFlag) {
                    entryName = entryName.substring(entryName.indexOf("/") + 1) // 去掉根目录
                }
                // 如果是文件夹则需创建文件目录
                handleZipFile(entry, destDirPath, entryName, zipFile)
            }
        } catch (e: IOException) {
            logger.error("UNZIP file[${srcFile.canonicalPath}] with error: ", e)
        } finally {
            closeZipFile(zipFile)
        }
    }

    fun zipDir(srcDir: File, zipFile: String): File {
        FileOutputStream(zipFile).use { fileOutputStream ->
            BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->
                ZipOutputStream(bufferedOutputStream).use { zipOutputStream ->
                    handleZipOutputStream(srcDir, zipOutputStream)
                }
            }
        }
        return File(zipFile)
    }

    private fun handleZipOutputStream(srcDir: File, zipOutputStream: ZipOutputStream) {
        try {
            zipFiles(zipOutputStream, srcDir, "")
        } catch (e: Exception) {
            logger.error("ZIP file[${srcDir.canonicalPath}] with error: ", e)
        } finally {
            try {
                zipOutputStream.closeEntry()
                zipOutputStream.close()
            } catch (e: IOException) {
                logger.error("ZIP OutputStream close error:", e)
            }
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun zipFiles(zipOut: ZipOutputStream, sourceFile: File, parentDirPath: String) {
        val buf = ByteArray(2048)
        if (sourceFile.isFile) {
            zipFile(zipOut, sourceFile, sourceFile.name, buf)
            return
        }
        for (file in sourceFile.listFiles()) {
            val basePath = if (parentDirPath.isBlank()) {
                file.name
            } else {
                parentDirPath + File.separator + file.name
            }
            // 递归进入目录，非目录则直接压缩
            if (file.isDirectory) {
                val entry = ZipEntry(basePath + File.separator)
                entry.time = file.lastModified()
                entry.size = file.length()
                zipOut.putNextEntry(entry)
                zipFiles(zipOut, file, basePath)
            } else {
                zipFile(zipOut, file, basePath, buf)
            }
        }
    }

    private fun zipFile(zipOut: ZipOutputStream, file: File, basePath: String, buf: ByteArray) {
        FileInputStream(file).use { fi ->
            BufferedInputStream(fi).use { origin ->
                val entry = ZipEntry(basePath)
                entry.time = file.lastModified()
                entry.size = file.length()
                zipOut.putNextEntry(entry)
                writeWithBuffer(zipOut, buf, origin)
            }
        }
    }

    private fun writeWithBuffer(zipOut: ZipOutputStream, buf: ByteArray, origin: BufferedInputStream) {
        while (true) {
            val readBytes = origin.read(buf)
            if (readBytes == -1) {
                break
            }
            zipOut.write(buf, 0, readBytes)
        }
    }

    private fun handleZipFile(
        entry: ZipEntry,
        destDirPath: String,
        entryName: String?,
        zipFile: ZipFile
    ) {
        var inputStream: InputStream? = null
        var fos: OutputStream? = null
        if (entry.isDirectory) {
            val dirPath = "$destDirPath/$entryName"
            val dir = File(dirPath)
            dir.mkdirs()
        } else {
            // 如果是文件，就先创建一个文件，然后用io流把内容copy过去
            val targetFile = File("$destDirPath/$entryName")
            if (!targetFile.parentFile.exists()) {
                targetFile.parentFile.mkdirs()
            }
            targetFile.createNewFile()
            try {
                // 将压缩文件内容写入到这个文件中
                inputStream = zipFile.getInputStream(entry)
                fos = FileOutputStream(targetFile)
                copyUnzipFile(inputStream, fos)
            } finally {
                closeStream(fos, inputStream)
            }
        }
    }

    private fun copyUnzipFile(inputStream: InputStream, fos: FileOutputStream) {
        val buf = ByteArray(BUFFER_SIZE)
        var len = inputStream.read(buf)
        while (len != -1) {
            fos.write(buf, 0, len)
            len = inputStream.read(buf)
        }
        fos.flush()
    }

    private fun closeStream(fos: OutputStream?, inputStream: InputStream?) {
        if (null != fos) {
            try {
                fos.close()
            } catch (e: IOException) {
                logger.error("outputStream close error!", e)
            } finally {
                closeInputStream(inputStream)
            }
        }
    }

    private fun closeInputStream(inputStream: InputStream?) {
        if (null != inputStream) {
            try {
                inputStream.close()
            } catch (e: IOException) {
                logger.error("inputStream close error!", e)
            }
        }
    }

    private fun closeZipFile(zipFile: ZipFile?) {
        if (zipFile != null) {
            try {
                zipFile.close()
            } catch (e: IOException) {
                logger.error("zipFile close error!", e)
            }
        }
    }
}
