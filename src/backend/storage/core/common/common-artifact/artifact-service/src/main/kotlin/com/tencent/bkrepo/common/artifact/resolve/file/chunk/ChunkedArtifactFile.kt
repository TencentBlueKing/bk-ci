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

package com.tencent.bkrepo.common.artifact.resolve.file.chunk

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.event.ArtifactReceivedEvent
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactDataReceiver
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.common.storage.util.toPath
import java.io.File
import java.io.InputStream

/**
 * 使用数据块构造的ArtifactFile
 */
class ChunkedArtifactFile(
    private val monitor: StorageHealthMonitor,
    private val storageProperties: StorageProperties,
    private val storageCredentials: StorageCredentials
) : ArtifactFile {

    /**
     * 是否初始化
     */
    private var initialized: Boolean = false

    /**
     * 文件sha1值
     */
    private var sha1: String? = null

    /**
     * 数据接收器
     */
    private val receiver: ArtifactDataReceiver

    init {
        val path = storageCredentials.upload.location.toPath()
        receiver = ArtifactDataReceiver(
            storageProperties.receive,
            storageProperties.monitor,
            path,
            randomPath = true
        )
        monitor.add(receiver)
        if (!monitor.healthy.get()) {
            receiver.unhealthy(monitor.getFallbackPath(), monitor.fallBackReason)
        }
    }

    override fun getInputStream(): InputStream {
        return receiver.getInputStream()
    }

    override fun getSize(): Long {
        return receiver.received
    }

    override fun isInMemory(): Boolean {
        return receiver.inMemory
    }

    override fun getFile(): File? {
        require(receiver.finished)
        return if (!isInMemory()) {
            receiver.filePath.toFile()
        } else null
    }

    override fun flushToFile(): File {
        require(receiver.finished)
        receiver.flushToFile()
        return receiver.filePath.toFile()
    }

    override fun isFallback(): Boolean {
        return receiver.fallback
    }

    override fun getFileMd5(): String {
        require(receiver.finished)
        return receiver.listener.getMd5()
    }

    /**
     * sha1的计算会重新读取流
     */
    override fun getFileSha1(): String {
        require(receiver.finished)
        return sha1 ?: getInputStream().sha1().apply { sha1 = this }
    }

    override fun getFileSha256(): String {
        require(receiver.finished)
        return receiver.listener.getSha256()
    }

    override fun delete() {
        if (initialized && !isInMemory()) {
            this.close()
        }
    }

    override fun hasInitialized(): Boolean {
        return initialized
    }

    override fun isInLocalDisk() = false

    /**
     * 写入分块数据
     * @param chunk 分块数据
     * @param offset 偏移
     * @param length 数据长度
     */
    fun write(chunk: ByteArray, offset: Int, length: Int) {
        receiver.receiveChunk(chunk, offset, length)
    }

    /**
     * 数据接收完毕
     * 触发后，后续不能再接收数据
     */
    fun finish(): Throughput {
        initialized = true
        val throughput = receiver.finish()
        monitor.remove(receiver)
        SpringContextUtils.publishEvent(ArtifactReceivedEvent(this, throughput, storageCredentials))
        return throughput
    }

    /**
     * 关闭文件执行清理逻辑，因为是被动接收数据，所以需要手动关闭文件
     */
    fun close() {
        receiver.close()
        monitor.remove(receiver)
    }

    fun write(b: Int) {
        receiver.receive(b)
    }
}
