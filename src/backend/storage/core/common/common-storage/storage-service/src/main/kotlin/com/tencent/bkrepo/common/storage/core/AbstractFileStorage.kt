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

package com.tencent.bkrepo.common.storage.core

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.common.storage.listener.FileStoreRetryListener
import com.tencent.bkrepo.common.storage.monitor.measureThroughput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.retry.RetryContext
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 文件存储抽象模板类
 * 抽象模板类实现了如客户端缓存、重试机制、错误处理、吞吐计算等逻辑，子类只需要关心具体存储实现
 */
@Suppress("UNCHECKED_CAST")
abstract class AbstractFileStorage<Credentials : StorageCredentials, Client> : FileStorage {

    @Suppress("LateinitUsage")
    @Autowired
    protected lateinit var storageProperties: StorageProperties

    private val retryTemplate = RetryTemplate()

    private val clientCache: LoadingCache<Credentials, Client> by lazy {
        val cacheLoader = object : CacheLoader<Credentials, Client>() {
            override fun load(credentials: Credentials): Client = onCreateClient(credentials)
        }
        CacheBuilder.newBuilder().maximumSize(MAX_CACHE_CLIENT).build(cacheLoader)
    }

    val defaultClient: Client by lazy {
        onCreateClient(storageProperties.defaultStorageCredentials() as Credentials)
    }

    init {
        // 重试策略：次数重试策略
        val retryPolicy = SimpleRetryPolicy(RETRY_MAX_ATTEMPTS)
        // 退避策略：指数退避策略
        val backOffPolicy = ExponentialBackOffPolicy().apply {
            initialInterval = RETRY_INITIAL_INTERVAL
            maxInterval = RETRY_MAX_INTERVAL
            multiplier = RETRY_MULTIPLIER
        }
        retryTemplate.setRetryPolicy(retryPolicy)
        retryTemplate.setBackOffPolicy(backOffPolicy)
        retryTemplate.registerListener(FileStoreRetryListener())
    }

    override fun store(path: String, name: String, file: File, storageCredentials: StorageCredentials) {
        retryTemplate.execute<Unit, Exception> {
            it.setAttribute(RetryContext.NAME, RETRY_NAME_STORE_FILE)
            val client = getClient(storageCredentials)
            val size = file.length()
            val throughput = measureThroughput(size) {
                store(path, name, file, client)
            }
            logger.info("Success to persist file [$name], $throughput.")
        }
    }

    override fun store(
        path: String,
        name: String,
        inputStream: InputStream,
        size: Long,
        storageCredentials: StorageCredentials
    ) {
        val client = getClient(storageCredentials)
        retryTemplate.execute<Unit, RuntimeException> {
            it.setAttribute(RetryContext.NAME, RETRY_NAME_STORE_STREAM)
            if (!inputStream.markSupported()) {
                it.setExhaustedOnly()
            } else {
                inputStream.reset()
            }
            val throughput = measureThroughput(size) {
                store(path, name, inputStream, size, client)
            }
            logger.info("Success to persist stream [$name], $throughput.")
        }
    }

    override fun load(
        path: String,
        name: String,
        range: Range,
        storageCredentials: StorageCredentials
    ): InputStream? {
        return try {
            val client = getClient(storageCredentials)
            load(path, name, range, client)
        } catch (exception: IOException) {
            logger.error("Failed to load stream[$name]: ${exception.message}", exception)
            null
        }
    }

    override fun delete(path: String, name: String, storageCredentials: StorageCredentials) {
        val client = getClient(storageCredentials)
        delete(path, name, client)
    }

    override fun exist(path: String, name: String, storageCredentials: StorageCredentials): Boolean {
        val client = getClient(storageCredentials)
        return exist(path, name, client)
    }

    override fun copy(
        path: String,
        name: String,
        fromCredentials: StorageCredentials,
        toCredentials: StorageCredentials
    ) {
        val fromClient = getClient(fromCredentials)
        val toClient = getClient(toCredentials)
        copy(path, name, fromClient, toClient)
    }

    private fun getClient(storageCredentials: StorageCredentials): Client {
        return if (storageCredentials == storageProperties.defaultStorageCredentials()) {
            defaultClient
        } else {
            clientCache.get(storageCredentials as Credentials)
        }
    }

    protected abstract fun onCreateClient(credentials: Credentials): Client
    abstract fun store(path: String, name: String, file: File, client: Client)
    abstract fun store(path: String, name: String, inputStream: InputStream, size: Long, client: Client)
    abstract fun load(path: String, name: String, range: Range, client: Client): InputStream?
    abstract fun delete(path: String, name: String, client: Client)
    abstract fun exist(path: String, name: String, client: Client): Boolean
    open fun copy(path: String, name: String, fromClient: Client, toClient: Client) {
        throw UnsupportedOperationException("Copy operation unsupported")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractFileStorage::class.java)
        private const val MAX_CACHE_CLIENT = 10L
        private const val RETRY_MAX_ATTEMPTS = 5
        private const val RETRY_INITIAL_INTERVAL = 1000L
        private const val RETRY_MAX_INTERVAL = 20 * 1000L
        private const val RETRY_MULTIPLIER = 2.0
        private const val RETRY_NAME_STORE_FILE = "FileStorage.storeFile"
        private const val RETRY_NAME_STORE_STREAM = "FileStorage.storeStream"
    }
}
