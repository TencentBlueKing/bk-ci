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

package com.tencent.bkrepo.common.artifact.resolve.file.bksync

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.file.chunk.ChunkedFileOutputStream
import com.tencent.bkrepo.common.bksync.BkSync
import com.tencent.bkrepo.common.bksync.BlockChannel
import java.io.File
import java.io.InputStream
import java.nio.channels.Channels

class BkSyncArtifactFile(
    private val blockChannel: BlockChannel,
    private val deltaInputStream: InputStream,
    private val blockSize: Int
) : ArtifactFile {
    private val chunkedArtifactFile = ArtifactFileFactory.buildChunked()

    /**
     * 是否初始化
     */
    private var initialized: Boolean = false

    override fun getInputStream(): InputStream {
        init()
        return chunkedArtifactFile.getInputStream()
    }

    override fun getSize(): Long {
        init()
        return chunkedArtifactFile.getSize()
    }

    override fun isInMemory(): Boolean {
        init()
        return chunkedArtifactFile.isInMemory()
    }

    override fun getFile(): File? {
        init()
        return chunkedArtifactFile.getFile()
    }

    override fun flushToFile(): File {
        init()
        return chunkedArtifactFile.flushToFile()
    }

    override fun delete() {
        chunkedArtifactFile.delete()
    }

    override fun hasInitialized(): Boolean {
        return initialized
    }

    override fun isFallback(): Boolean {
        init()
        return chunkedArtifactFile.isFallback()
    }

    override fun getFileMd5(): String {
        init()
        return chunkedArtifactFile.getFileMd5()
    }

    override fun getFileSha1(): String {
        init()
        return chunkedArtifactFile.getFileSha1()
    }

    override fun getFileSha256(): String {
        init()
        return chunkedArtifactFile.getFileSha256()
    }

    override fun isInLocalDisk(): Boolean {
        return chunkedArtifactFile.isInLocalDisk()
    }

    private fun init() {
        if (initialized) {
            return
        }
        val bkSync = BkSync(blockSize)
        try {
            // outputStream不能close,因为close后会删除临时文件。这里统一通过请求拦截器进行清理
            val outputStream = ChunkedFileOutputStream(chunkedArtifactFile).buffered()
            val channel = Channels.newChannel(outputStream)
            bkSync.merge(blockChannel, deltaInputStream, channel)
            // buffered stream所以需要flush
            outputStream.flush()
            chunkedArtifactFile.finish()
        } catch (e: Exception) {
            chunkedArtifactFile.close()
            throw e
        }
        initialized = true
    }
}
