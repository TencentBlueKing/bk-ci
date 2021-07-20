/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.resolve.file.stream

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactFile.Companion.generateRandomName
import com.tencent.bkrepo.common.artifact.event.ArtifactReceivedEvent
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.resolve.file.SmartStreamReceiver
import com.tencent.bkrepo.common.artifact.stream.DigestCalculateListener
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import com.tencent.bkrepo.common.storage.util.toPath
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.NoSuchFileException

/**
 * application/octet-stream流类型ArtifactFile
 */
open class OctetStreamArtifactFile(
    private val source: InputStream,
    private val monitor: StorageHealthMonitor,
    private val storageProperties: StorageProperties,
    private val storageCredentials: StorageCredentials
) : ArtifactFile {

    private var hasInitialized: Boolean = false
    private var sha1: String? = null
    private val listener: DigestCalculateListener
    private val receiver: SmartStreamReceiver

    init {
        val path = storageCredentials.upload.location.toPath()
        val fileSizeThreshold = storageProperties.fileSizeThreshold.toBytes()
        val enableTransfer = storageProperties.monitor.enableTransfer
        val rateLimit = storageProperties.rateLimit
        receiver = SmartStreamReceiver(fileSizeThreshold, generateRandomName(), path, enableTransfer, rateLimit)
        listener = DigestCalculateListener()
        if (!storageProperties.isResolveLazily) {
            init()
        }
    }

    override fun getInputStream(): InputStream {
        init()
        return if (!isInMemory()) {
            Files.newInputStream(receiver.getFilePath())
        } else {
            ByteArrayInputStream(receiver.getCachedByteArray())
        }
    }

    override fun getSize(): Long {
        init()
        return receiver.totalSize
    }

    override fun isInMemory(): Boolean {
        init()
        return receiver.isInMemory
    }

    override fun getFile(): File? {
        init()
        return if (!isInMemory()) {
            receiver.getFilePath().toFile()
        } else null
    }

    override fun flushToFile(): File {
        init()
        if (isInMemory()) {
            receiver.flushToFile()
        }
        return receiver.getFilePath().toFile()
    }

    override fun isFallback(): Boolean {
        init()
        return receiver.fallback
    }

    override fun getFileMd5(): String {
        init()
        return listener.getMd5()
    }

    override fun getFileSha1(): String {
        init()
        return sha1 ?: getInputStream().sha1().apply { sha1 = this }
    }

    override fun getFileSha256(): String {
        init()
        return listener.getSha256()
    }

    override fun delete() {
        if (hasInitialized && !isInMemory()) {
            try {
                Files.deleteIfExists(receiver.getFilePath())
            } catch (ignored: NoSuchFileException) { // already deleted
            }
        }
    }

    override fun hasInitialized(): Boolean {
        return hasInitialized
    }

    private fun init() {
        if (hasInitialized) {
            return
        }
        try {
            if (storageCredentials == storageProperties.defaultStorageCredentials()) {
                monitor.add(receiver)
                if (!monitor.health.get()) {
                    receiver.unhealthy(monitor.getFallbackPath(), monitor.reason)
                }
            }
            val throughput = receiver.receive(source, listener)
            hasInitialized = true
            SpringContextUtils.publishEvent(ArtifactReceivedEvent(this, throughput, storageCredentials))
        } finally {
            monitor.remove(receiver)
        }
    }
}
