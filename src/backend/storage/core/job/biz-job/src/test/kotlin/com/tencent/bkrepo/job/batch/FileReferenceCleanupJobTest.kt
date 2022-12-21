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

package com.tencent.bkrepo.job.batch

import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.job.SHARDING_COUNT
import com.tencent.bkrepo.job.config.JobProperties
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import org.bson.Document
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("文件引用清理Job测试")
@DataMongoTest
class FileReferenceCleanupJobTest : JobBaseTest() {

    @MockBean
    lateinit var storageService: StorageService

    @MockBean
    lateinit var storageCredentialsClient: StorageCredentialsClient

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @Autowired
    lateinit var fileReferenceCleanupJob: FileReferenceCleanupJob

    @Autowired
    lateinit var jobProperties: JobProperties

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @BeforeEach
    fun beforeEach() {
        Mockito.`when`(storageService.exist(anyString(), any())).thenReturn(true)
        val credentials = InnerCosCredentials()
        Mockito.`when`(storageCredentialsClient.findByKey(anyString())).thenReturn(
            ResponseBuilder.success(credentials)
        )
        SpringContextUtils().setApplicationContext(applicationContext)
    }

    @AfterEach
    fun afterEach() {
        fileReferenceCleanupJob.collectionNames().forEach {
            mongoTemplate.remove(Query(), it)
        }
        jobProperties.fileReferenceCleanupJobProperties.permitsPerSecond = 0.0
    }

    @DisplayName("测试正常运行")
    @Test
    fun run() {
        val num = 100
        fileReferenceCleanupJob.collectionNames().forEach {
            insertMany(num, it)
        }
        val deleted = AtomicInteger()
        Mockito.`when`(storageService.delete(anyString(), any())).then { deleted.incrementAndGet() }
        fileReferenceCleanupJob.start()
        Assertions.assertEquals(SHARDING_COUNT * num, deleted.get())
    }

    @DisplayName("测试排它执行")
    @Test
    fun exclusiveTest() {
        Assertions.assertTrue(fileReferenceCleanupJob.start())
        Assertions.assertFalse(fileReferenceCleanupJob.start())
    }

    @DisplayName("测试单表大数据,分页清理")
    @Test
    fun bigCollection() {
        val num = 50_000
        insertMany(num, fileReferenceCleanupJob.collectionNames().first())
        val deleted = AtomicInteger()
        Mockito.`when`(storageService.delete(anyString(), any())).then {
            deleted.incrementAndGet()
        }
        fileReferenceCleanupJob.start()
        Assertions.assertEquals(num, deleted.get())
    }

    @DisplayName("限速测试")
    @Test
    fun rateLimitTest() {
        jobProperties.fileReferenceCleanupJobProperties.permitsPerSecond = 100.0
        fileReferenceCleanupJob.collectionNames().forEach { name ->
            insertMany(1, name)
        }
        val deleted = AtomicInteger()
        Mockito.`when`(storageService.delete(anyString(), any())).then {
            deleted.incrementAndGet()
        }
        val begin = System.currentTimeMillis()
        fileReferenceCleanupJob.start()
        val spend = System.currentTimeMillis() - begin
        Assertions.assertEquals(SHARDING_COUNT, deleted.get())
        println(spend)
        // 256个boss任务，256个work任务，总共512个,限速100tps，所以这里至少要大于5s
        Assertions.assertTrue(spend > 5000)
    }

    private fun insertMany(num: Int, collectionName: String) {
        (0 until num).forEach {
            val doc = Document(
                mutableMapOf(
                    "sha256" to it.toString(),
                    "credentialsKey" to it.toString(),
                    "count" to 0
                ) as Map<String, Any>?
            )
            mongoTemplate.insert(
                doc,
                collectionName
            )
        }
    }
}
