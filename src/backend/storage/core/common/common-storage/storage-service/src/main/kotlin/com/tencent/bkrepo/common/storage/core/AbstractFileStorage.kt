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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.storage.core

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.event.StoreFailureEvent
import com.tencent.bkrepo.common.storage.monitor.Throughput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import java.io.File
import java.io.InputStream
import kotlin.system.measureNanoTime

/**
 * 文件存储接口
 */
@Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught")
abstract class AbstractFileStorage<Credentials : StorageCredentials, Client> : FileStorage {

    @Autowired
    protected lateinit var storageProperties: StorageProperties

    @Autowired
    private lateinit var publisher: ApplicationEventPublisher

    private val clientCache: LoadingCache<Credentials, Client> by lazy {
        val cacheLoader = object : CacheLoader<Credentials, Client>() {
            override fun load(credentials: Credentials): Client = onCreateClient(credentials)
        }
        CacheBuilder.newBuilder().maximumSize(MAX_CACHE_CLIENT).build(cacheLoader)
    }

    val defaultClient: Client by lazy {
        onCreateClient(storageProperties.defaultStorageCredentials() as Credentials)
    }

    override fun store(path: String, filename: String, file: File, storageCredentials: StorageCredentials) {
        val client = getClient(storageCredentials)
        val size = file.length()
        val nanoTime = measureNanoTime {
            store(path, filename, file, client)
        }
        val throughput = Throughput(size, nanoTime)
        logger.info("Success to persist file [$filename], $throughput.")
    }

    override fun store(path: String, filename: String, inputStream: InputStream, size: Long, storageCredentials: StorageCredentials) {
        val client = getClient(storageCredentials)
        val nanoTime = measureNanoTime {
            store(path, filename, inputStream, size, client)
        }
        val throughput = Throughput(size, nanoTime)
        logger.info("Success to persist stream [$filename], $throughput.")
    }

    override fun load(path: String, filename: String, range: Range, storageCredentials: StorageCredentials): InputStream? {
        return try {
            val client = getClient(storageCredentials)
            load(path, filename, range, client)
        } catch (ex: Exception) {
            logger.warn("Failed to load stream[$filename]: ${ex.message}", ex)
            null
        }
    }

    override fun delete(path: String, filename: String, storageCredentials: StorageCredentials) {
        val client = getClient(storageCredentials)
        delete(path, filename, client)
    }

    override fun exist(path: String, filename: String, storageCredentials: StorageCredentials): Boolean {
        val client = getClient(storageCredentials)
        return exist(path, filename, client)
    }

    override fun copy(path: String, filename: String, fromCredentials: StorageCredentials, toCredentials: StorageCredentials) {
        val fromClient = getClient(fromCredentials)
        val toClient = getClient(toCredentials)
        copy(path, filename, fromClient, toClient)
    }

    override fun recover(exception: Exception, path: String, filename: String, file: File, storageCredentials: StorageCredentials) {
        val event = StoreFailureEvent(path, filename, file.absolutePath, storageCredentials, exception)
        publisher.publishEvent(event)
    }

    private fun getClient(storageCredentials: StorageCredentials): Client {
        return if (storageCredentials == storageProperties.defaultStorageCredentials()) {
            defaultClient
        } else {
            clientCache.get(storageCredentials as Credentials)
        }
    }

    protected abstract fun onCreateClient(credentials: Credentials): Client
    abstract fun store(path: String, filename: String, file: File, client: Client)
    abstract fun store(path: String, filename: String, inputStream: InputStream, size: Long, client: Client)
    abstract fun load(path: String, filename: String, range: Range, client: Client): InputStream?
    abstract fun delete(path: String, filename: String, client: Client)
    abstract fun exist(path: String, filename: String, client: Client): Boolean
    open fun copy(path: String, filename: String, fromClient: Client, toClient: Client) {
        throw RuntimeException("Copy operation unsupported")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractFileStorage::class.java)
        private const val MAX_CACHE_CLIENT = 10L
    }
}
