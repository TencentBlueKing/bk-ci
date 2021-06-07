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

package com.tencent.bkrepo.common.artifact.repository.core

import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.artifact.event.ArtifactDownloadedEvent
import com.tencent.bkrepo.common.artifact.event.ArtifactResponseEvent
import com.tencent.bkrepo.common.artifact.event.ArtifactUploadedEvent
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.exception.ArtifactResponseException
import com.tencent.bkrepo.common.artifact.metrics.ArtifactMetrics
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.migration.MigrateDetail
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.util.http.ArtifactResourceWriter
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.PackageDownloadsClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * 构件仓库抽象类
 */
// TooGenericExceptionCaught: 需要捕捉文件传输阶段网络、IO等不可控的异常
// LateinitUsage: AbstractArtifactRepository有大量子类，使用构造器注入将造成不便
@Suppress("TooGenericExceptionCaught", "LateinitUsage")
abstract class AbstractArtifactRepository : ArtifactRepository {

    @Autowired
    lateinit var nodeClient: NodeClient

    @Autowired
    lateinit var repositoryClient: RepositoryClient

    @Autowired
    lateinit var packageClient: PackageClient

    @Autowired
    lateinit var storageService: StorageService

    @Autowired
    lateinit var storageManager: StorageManager

    @Autowired
    lateinit var artifactMetrics: ArtifactMetrics

    @Autowired
    lateinit var publisher: ApplicationEventPublisher

    @Autowired
    lateinit var packageDownloadsClient: PackageDownloadsClient

    @Autowired
    private lateinit var taskAsyncExecutor: ThreadPoolTaskExecutor

    override fun upload(context: ArtifactUploadContext) {
        try {
            this.onUploadBefore(context)
            this.onUpload(context)
            this.onUploadSuccess(context)
        } catch (exception: RuntimeException) {
            this.onUploadFailed(context, exception)
        } finally {
            this.onUploadFinished(context)
        }
    }

    override fun download(context: ArtifactDownloadContext) {
        try {
            this.onDownloadBefore(context)
            val artifactResponse = this.onDownload(context)
                ?: throw ArtifactNotFoundException(context.artifactInfo.toString())
            val throughput = ArtifactResourceWriter.write(artifactResponse)
            this.onDownloadSuccess(context, artifactResponse, throughput)
        } catch (responseException: ArtifactResponseException) {
            val principal = SecurityUtils.getPrincipal()
            val artifactInfo = context.artifactInfo
            logger.warn("User[$principal] download artifact[$artifactInfo] failed, ${responseException.message}")
        } catch (exception: RuntimeException) {
            this.onDownloadFailed(context, exception)
        } finally {
            this.onDownloadFinished(context)
        }
    }

    override fun remove(context: ArtifactRemoveContext) {
        throw MethodNotAllowedException()
    }

    override fun query(context: ArtifactQueryContext): Any? {
        throw MethodNotAllowedException()
    }

    override fun search(context: ArtifactSearchContext): List<Any> {
        throw MethodNotAllowedException()
    }

    override fun migrate(context: ArtifactMigrateContext): MigrateDetail {
        throw MethodNotAllowedException()
    }

    /**
     * 上传前回调
     */
    open fun onUploadBefore(context: ArtifactUploadContext) {
        artifactMetrics.uploadingCount.incrementAndGet()
    }

    /**
     * 上传构件
     */
    open fun onUpload(context: ArtifactUploadContext) {
        throw MethodNotAllowedException()
    }

    /**
     * 上传成功回调
     */
    open fun onUploadSuccess(context: ArtifactUploadContext) {
        publisher.publishEvent(ArtifactUploadedEvent(context))
        val artifactInfo = context.artifactInfo
        logger.info("User[${SecurityUtils.getPrincipal()}] upload artifact[$artifactInfo] success")
    }

    /**
     * 上传失败回调
     */
    open fun onUploadFailed(context: ArtifactUploadContext, exception: Exception) {
        // 默认向上抛异常，由全局异常处理器处理
        throw exception
    }

    /**
     * 下载前回调
     */
    open fun onDownloadBefore(context: ArtifactDownloadContext) {
        artifactMetrics.downloadingCount.incrementAndGet()
    }

    /**
     * 下载构件
     */
    open fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        throw MethodNotAllowedException()
    }

    /**
     * 下载成功回调
     */
    open fun onDownloadSuccess(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
        throughput: Throughput
    ) {
        if (artifactResource.channel == ArtifactChannel.LOCAL) {
            buildDownloadRecord(context, artifactResource)?.let {
                taskAsyncExecutor.execute { packageDownloadsClient.record(it) }
            }
        }
        if (throughput != Throughput.EMPTY) {
            publisher.publishEvent(ArtifactResponseEvent(artifactResource, throughput, context.storageCredentials))
        }
        publisher.publishEvent(ArtifactDownloadedEvent(context))
        logger.info("User[${SecurityUtils.getPrincipal()}] download artifact[${context.artifactInfo}] success")
    }

    /**
     * 构造下载记录
     *
     * 各依赖源自行判断是否需要增加下载记录，如果返回空则不记录
     */
    open fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        return null
    }

    /**
     * 下载失败回调
     *
     * 默认向上抛异常，由全局异常处理器处理
     */
    open fun onDownloadFailed(context: ArtifactDownloadContext, exception: Exception) {
        throw exception
    }

    /**
     * 上传结束回调
     */
    open fun onUploadFinished(context: ArtifactUploadContext) {
        artifactMetrics.uploadingCount.decrementAndGet()
    }

    /**
     * 下载结束回调
     */
    open fun onDownloadFinished(context: ArtifactDownloadContext) {
        artifactMetrics.downloadingCount.decrementAndGet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractArtifactRepository::class.java)
    }
}
