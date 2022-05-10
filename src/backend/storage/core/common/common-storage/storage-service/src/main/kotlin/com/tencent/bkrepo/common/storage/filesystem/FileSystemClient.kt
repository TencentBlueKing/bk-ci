/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.storage.filesystem

import com.tencent.bkrepo.common.storage.util.createFile
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.FileAlreadyExistsException

/**
 * 本地文件存储客户端
 */
class FileSystemClient(private val root: String) {

    constructor(path: Path) : this(path.toString())

    /**
     * 创建新文件
     * @param dir 目录
     * @param filename 文件名
     */
    fun touch(dir: String, filename: String): File {
        val filePath = Paths.get(this.root, dir, filename)
        filePath.createFile()
        return filePath.toFile()
    }

    /**
     * 存储文件
     * @param dir 目录
     * @param filename 文件名
     * @param inputStream 文件数据流
     * @param size 文件大小
     * @param overwrite 存在同名文件时是否覆盖
     */
    fun store(dir: String, filename: String, inputStream: InputStream, size: Long, overwrite: Boolean = false): File {
        val filePath = Paths.get(this.root, dir, filename)
        if (overwrite) {
            Files.deleteIfExists(filePath)
        }
        if (!Files.exists(filePath)) {
            val file = filePath.createFile()
            FileLockExecutor.executeInLock(inputStream) { input ->
                FileLockExecutor.executeInLock(file) { output ->
                    transfer(input, output, size)
                }
            }
        }
        return filePath.toFile()
    }

    /**
     * 移动文件
     * @param dir 目录
     * @param filename 文件名
     * @param file 源文件
     * @param overwrite 存在同名文件时是否覆盖
     *
     * @return 返回移动后的文件
     */
    fun move(dir: String, filename: String, file: File, overwrite: Boolean = false): File {
        val source = file.toPath()
        val target = Paths.get(this.root, dir, filename)
        if (overwrite) {
            Files.deleteIfExists(target)
        }
        if (!Files.exists(target)) {
            try {
                // 不能使用REPLACE_EXISTING/ATOMIC_MOVE，因为会删除其他客户端move的文件，
                // 且由于NFS的非强一致性，即使本客户端move成功，也会导致其他客户端发生文件找不到错误
                Files.move(source, target)
            } catch (ignore: FileAlreadyExistsException) {
                logger.info("File[$file] already exists")
            } catch (ex: IOException) {
                val message = ex.message.orEmpty()
                logger.warn("Failed to move file by Files.move(source, target), fallback to use file channel: $message")
                copyByChannel(source, target)
                Files.deleteIfExists(source)
            }
        }
        return target.toFile()
    }

    /**
     * 使用channel拷贝
     * */
    private fun copyByChannel(src: Path, target: Path) {
        if (!Files.exists(src)) {
            throw IOException("src[$src] file not exist")
        }
        val targetFile = if (!Files.exists(target)) {
            target.createFile()
        } else {
            target.toFile()
        }
        val file = src.toFile()
        FileLockExecutor.executeInLock(file.inputStream()) { input ->
            FileLockExecutor.executeInLock(targetFile) { output ->
                transfer(input, output, file.length())
            }
        }
    }

    /**
     * 删除文件
     * @param dir 目录
     * @param filename 文件名
     */
    fun delete(dir: String, filename: String) {
        val filePath = Paths.get(this.root, dir, filename)
        if (Files.exists(filePath)) {
            if (Files.isRegularFile(filePath)) {
                FileLockExecutor.executeInLock(filePath.toFile()) {
                    Files.delete(filePath)
                }
            } else {
                throw IllegalArgumentException("[$filePath] is not a regular file.")
            }
        }
    }

    /**
     * 加载文件
     * @param dir 目录
     * @param filename 文件名
     *
     * @return 文件对象
     */
    fun load(dir: String, filename: String): File? {
        val filePath = Paths.get(this.root, dir, filename)
        return if (Files.isRegularFile(filePath)) filePath.toFile() else null
    }

    /**
     * 判断文件是否存在
     * @param dir 目录
     * @param filename 文件名
     *
     * @return 是否存在
     */
    fun exist(dir: String, filename: String): Boolean {
        val filePath = Paths.get(this.root, dir, filename)
        return Files.isRegularFile(filePath)
    }

    /**
     * 追加文件内容
     * @param dir 目录
     * @param filename 文件名
     * @param inputStream 输入流
     * @param size 输入流数据大小
     *
     * @return 当前文件总大小
     */
    fun append(dir: String, filename: String, inputStream: InputStream, size: Long): Long {
        val filePath = Paths.get(this.root, dir, filename)
        if (!Files.isRegularFile(filePath)) {
            throw IllegalArgumentException("[$filePath] is not a regular file.")
        }
        val file = filePath.toFile()
        FileLockExecutor.executeInLock(inputStream) { input ->
            FileLockExecutor.executeInLock(file) { output ->
                transfer(input, output, size, true)
            }
        }
        return Files.size(filePath)
    }

    /**
     * 创建目录
     * @param dir 父目录
     * @param name 要创建的目录名称
     */
    fun createDirectory(dir: String, name: String) {
        val dirPath = Paths.get(this.root, dir, name)
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath)
        }
    }

    /**
     * 删除目录，包括子文件
     * @param dir 父目录
     * @param name 要删除的目录名称
     */
    fun deleteDirectory(dir: String, name: String) {
        val filePath = Paths.get(this.root, dir, name)
        if (Files.isDirectory(filePath)) {
            filePath.toFile().deleteRecursively()
        } else {
            throw IllegalArgumentException("[$filePath] is not a directory.")
        }
    }

    /**
     * 检查文件是否存在且为目录
     * @param dir 目录名称
     */
    fun checkDirectory(dir: String): Boolean {
        return Files.isDirectory(Paths.get(this.root, dir))
    }

    /**
     * 列出目录下所有文件并根据扩展名过滤
     * @param path 目录名称
     * @param extension 扩展名
     */
    fun listFiles(path: String, extension: String): Collection<File> {
        return FileUtils.listFiles(File(this.root, path), arrayOf(extension.trim('.')), false)
    }

    /**
     * 将多个文件合并为一个文件
     * @param fileList 待合并的文件列表
     * @param outputFile 合并后的文件
     *
     * @return 合并后的文件
     */
    fun mergeFiles(fileList: List<File>, outputFile: File): File {
        if (!outputFile.exists()) {
            if (!outputFile.createNewFile()) {
                throw IOException("Failed to create file [$outputFile]!")
            }
        }

        FileLockExecutor.executeInLock(outputFile) { output ->
            fileList.forEach { file ->
                FileLockExecutor.executeInLock(file.inputStream()) { input ->
                    transfer(input, output, file.length(), true)
                }
            }
        }
        return outputFile
    }

    /**
     * 遍历文件
     * @param visitor ArtifactFileVisitor实现类
     */
    fun walk(visitor: ArtifactFileVisitor) {
        if (visitor.needWalk()) {
            val rootPath = Paths.get(root)
            Files.walkFileTree(rootPath, visitor)
        }
    }

    private fun transfer(input: ReadableByteChannel, output: FileChannel, size: Long, append: Boolean = false) {
        val startPosition: Long = if (append) output.size() else 0L
        var bytesCopied: Long
        var totalCopied = 0L
        var count: Long
        while (totalCopied < size) {
            val remain = size - totalCopied
            count = if (remain > FILE_COPY_BUFFER_SIZE) FILE_COPY_BUFFER_SIZE else remain
            bytesCopied = output.transferFrom(input, startPosition + totalCopied, count)
            if (bytesCopied == 0L) { // can happen if file is truncated after caching the size
                break
            }
            totalCopied += bytesCopied
        }
        if (totalCopied != size) {
            throw IOException("Failed to copy full contents. Expected length: $size, Actual: $totalCopied")
        }
    }

    companion object {
        /**
         * OpenJdk中FileChannelImpl.java限定了单次传输大小:
         * private static final long MAPPED_TRANSFER_SIZE = 8L*1024L*1024L;
         * 防止不同jdk版本的不同实现，这里限定一下大小
         */
        private const val FILE_COPY_BUFFER_SIZE = 64 * 1024 * 1024L

        private val logger = LoggerFactory.getLogger(FileSystemClient::class.java)
    }
}
