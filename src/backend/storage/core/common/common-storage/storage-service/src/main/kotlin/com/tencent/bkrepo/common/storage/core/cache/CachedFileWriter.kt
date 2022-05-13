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

package com.tencent.bkrepo.common.storage.core.cache

import com.tencent.bkrepo.common.artifact.stream.StreamReadListener
import com.tencent.bkrepo.common.artifact.stream.closeQuietly
import com.tencent.bkrepo.common.storage.util.createNewOutputStream
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * 实现[StreamReadListener]接口，用于将FileStorage读取到的数据写入到缓存文件中
 *
 * @param cachePath 缓存路径
 * @param filename 缓存文件名称
 * @param tempPath 临时路径
 *
 * 处理逻辑：
 * 1. 在该目录下原子创建一个临时文件
 * 2. 数据写入该临时文件
 * 3. 数据写完毕后，将该文件move到[cachePath]位置
 *
 * 并发处理逻辑：对于同一个文件[filename]，可能存在多个并发下载请求触发缓存
 * 1. 首先判断目录临时文件是否存在，存在则跳过(说明此时有其它请求正在进行缓存)
 * 2. 不存在则按照上述缓存逻辑执行
 * 3. 在极少数情况下第1、2存在并发问题，导致多个请求都进行缓存,但只有原子创建文件成功的才可以进行缓存
 *
 */
class CachedFileWriter(
    private val cachePath: Path,
    private val filename: String,
    tempPath: Path
) : StreamReadListener {

    private var tempFilePath: Path = tempPath.resolve(filename)
    private var outputStream: OutputStream? = null

    init {
        try {
            if (Files.exists(tempFilePath)) {
                logger.debug("Path[$tempFilePath] exists, ignore caching")
            } else {
                outputStream = tempFilePath.createNewOutputStream()
            }
        } catch (ignore: FileAlreadyExistsException) {
            // 如果目录或者文件已存在则忽略
        } catch (exception: Exception) {
            logger.error("initial CacheFileWriter error: $exception", exception)
            close()
        }
    }

    override fun data(i: Int) {
        outputStream?.let {
            try {
                it.write(i)
            } catch (ignored: Exception) {
                // ignored
                close()
            }
        }
    }

    override fun data(buffer: ByteArray, off: Int, length: Int) {
        outputStream?.let {
            try {
                it.write(buffer, off, length)
            } catch (ignored: Exception) {
                // ignored
                close()
            }
        }
    }

    override fun finish() {
        outputStream?.let {
            try {
                it.flush()
                it.closeQuietly()
                Files.createDirectories(cachePath)
                Files.move(tempFilePath, cachePath.resolve(filename), StandardCopyOption.REPLACE_EXISTING)
                logger.info("Success cache file $filename")
            } catch (exception: Exception) {
                logger.warn("finish CacheFileWriter error: $exception")
            } finally {
                close()
            }
        }
    }

    override fun close() {
        outputStream?.let {
            try {
                it.closeQuietly()
            } catch (exception: Exception) {
                logger.error("close CacheFileWriter error: $exception", exception)
            } finally {
                outputStream = null
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CachedFileWriter::class.java)
    }
}
