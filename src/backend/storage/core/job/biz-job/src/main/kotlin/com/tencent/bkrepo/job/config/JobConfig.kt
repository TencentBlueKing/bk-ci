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

package com.tencent.bkrepo.job.config

import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.helm.api.HelmClient
import com.tencent.bkrepo.job.batch.FileReferenceCleanupJob
import com.tencent.bkrepo.job.batch.FileSynchronizeJob
import com.tencent.bkrepo.job.batch.RemoteRepoInitJob
import com.tencent.bkrepo.job.batch.RemoteRepoRefreshJob
import com.tencent.bkrepo.job.batch.SignFileCleanupJob
import com.tencent.bkrepo.job.executor.BlockThreadPoolTaskExecutorDecorator
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.StorageCredentialsClient
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * Job配置
 * */
@Configuration
@EnableConfigurationProperties(JobProperties::class)
class JobConfig {
    @Bean
    fun blockThreadPoolTaskExecutorDecorator(
        threadPoolTaskExecutor: ThreadPoolTaskExecutor,
        properties: TaskExecutionProperties
    ): BlockThreadPoolTaskExecutorDecorator {
        return BlockThreadPoolTaskExecutorDecorator(
            threadPoolTaskExecutor,
            properties.pool.queueCapacity,
            Runtime.getRuntime().availableProcessors()
        )
    }

    @Bean
    fun fileReferenceCleanupJob(
        storageService: StorageService,
        mongoTemplate: MongoTemplate,
        storageCredentialsClient: StorageCredentialsClient,
        jobProperties: JobProperties
    ): FileReferenceCleanupJob {
        return FileReferenceCleanupJob(
            storageService,
            mongoTemplate,
            storageCredentialsClient,
            jobProperties.fileReferenceCleanupJobProperties
        )
    }

    @Bean
    fun remoteRepoRefreshJob(
        mongoTemplate: MongoTemplate,
        jobProperties: JobProperties,
        helmClient: HelmClient
    ): RemoteRepoRefreshJob {
        return RemoteRepoRefreshJob(
            properties = jobProperties.repoRefreshJobProperties,
            helmClient = helmClient
        )
    }

    @Bean
    fun remoteRepoInitJob(
        mongoTemplate: MongoTemplate,
        jobProperties: JobProperties,
        helmClient: HelmClient
    ): RemoteRepoInitJob {
        return RemoteRepoInitJob(
            properties = jobProperties.repoInitJobProperties,
            helmClient = helmClient
        )
    }

    @Bean
    fun signFileCleanupJob(
        mongoTemplate: MongoTemplate,
        nodeClient: NodeClient,
        jobProperties: JobProperties
    ): SignFileCleanupJob {
        return SignFileCleanupJob(
            nodeClient = nodeClient,
            mongoTemplate = mongoTemplate,
            properties = jobProperties.signFileCleanupJobProperties
        )
    }

    @Bean
    fun fileSynchronizeJob(
        jobProperties: JobProperties,
        storageCredentialsClient: StorageCredentialsClient,
        storageService: StorageService
    ): FileSynchronizeJob {
        return FileSynchronizeJob(
            properties = jobProperties.fileSynchronizeJobProperties,
            storageService = storageService,
            storageCredentialsClient = storageCredentialsClient
        )
    }
}
