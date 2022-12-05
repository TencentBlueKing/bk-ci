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

package com.tencent.bkrepo.common.artifact.resolve.file.stream

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.event.ArtifactReceivedEvent
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactDataReceiver
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.util.toPath
import java.io.File
import java.io.InputStream

/**
 * 基于数据流的ArtifactFile
 */
open class StreamArtifactFile(
    private val source: InputStream,
    private val monitor: StorageHealthMonitor,
    private val storageProperties: StorageProperties,
    private val storageCredentials: StorageCredentials,
    private val contentLength: Long? = null
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
     *
     * */
    private val useLocalPath: Boolean

    /**
     * 数据接收器
     */
    private val receiver: ArtifactDataReceiver

    init {
        // 主要路径，可以为DFS路径
        val path = storageCredentials.upload.location.toPath()
        // 本地路径
        val localPath = storageCredentials.upload.localPath.toPath()
        // 本地路径阈值
        val localThreshold = storageProperties.receive.localThreshold
        useLocalPath = contentLength != null && contentLength > 0 && contentLength < localThreshold.toBytes()
        val receivePath = if (useLocalPath) localPath else path
        receiver = ArtifactDataReceiver(
            storageProperties.receive,
            storageProperties.monitor,
            receivePath,
            randomPath = !useLocalPath
        )
        if (!storageProperties.receive.resolveLazily) {
            init()
        }
    }

    override fun getInputStream(): InputStream {
        init()
        return receiver.getInputStream()
    }

    override fun getSize(): Long {
        init()
        return receiver.received
    }

    override fun isInMemory(): Boolean {
        init()
        return receiver.inMemory
    }

    override fun getFile(): File? {
        init()
        return if (!isInMemory()) {
            receiver.filePath.toFile()
        } else null
    }

    override fun flushToFile(): File {
        init()
        receiver.flushToFile()
        return receiver.filePath.toFile()
    }

    override fun isFallback(): Boolean {
        init()
        return receiver.fallback
    }

    override fun getFileMd5(): String {
        init()
        return receiver.listener.getMd5()
    }

    /**
     * sha1的计算会重新读取流
     */
    override fun getFileSha1(): String {
        init()
        return sha1 ?: getInputStream().sha1().apply { sha1 = this }
    }

    override fun getFileSha256(): String {
        init()
        return receiver.listener.getSha256()
    }

    override fun delete() {
        if (initialized && !isInMemory()) {
            receiver.close()
        }
    }

    override fun hasInitialized(): Boolean {
        return initialized
    }

    override fun isInLocalDisk() = useLocalPath

    private fun init() {
        if (initialized) {
            return
        }
        try {
            // 本地磁盘不需要fallback
            if (!isInLocalDisk()) {
                monitor.add(receiver)
                if (!monitor.healthy.get()) {
                    receiver.unhealthy(monitor.getFallbackPath(), monitor.fallBackReason)
                }
            }
            receiver.receiveStream(source)
            val throughput = receiver.finish()
            initialized = true
            SpringContextUtils.publishEvent(ArtifactReceivedEvent(this, throughput, storageCredentials))
        } finally {
            monitor.remove(receiver)
        }
    }
}
