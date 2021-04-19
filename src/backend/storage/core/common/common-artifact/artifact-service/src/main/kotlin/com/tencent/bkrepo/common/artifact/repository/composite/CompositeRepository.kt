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

package com.tencent.bkrepo.common.artifact.repository.composite

import com.tencent.bkrepo.common.artifact.constant.PRIVATE_PROXY_REPO_NAME
import com.tencent.bkrepo.common.artifact.constant.PUBLIC_PROXY_PROJECT
import com.tencent.bkrepo.common.artifact.constant.PUBLIC_PROXY_REPO_NAME
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.ProxyChannelSetting
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.migration.MigrateDetail
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.storage.monitor.Throughput
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 组合仓库抽象逻辑
 */
@Service
class CompositeRepository(
    private val localRepository: LocalRepository,
    private val remoteRepository: RemoteRepository,
    private val proxyChannelClient: ProxyChannelClient
) : AbstractArtifactRepository() {

    /**
     * upload复用local仓库逻辑
     */
    override fun upload(context: ArtifactUploadContext) {
        localRepository.upload(context)
    }

    /**
     * migrate复用local仓库逻辑
     */
    override fun migrate(context: ArtifactMigrateContext): MigrateDetail {
        return localRepository.migrate(context)
    }

    /**
     * remove复用local仓库逻辑
     */
    override fun remove(context: ArtifactRemoveContext) {
        return localRepository.remove(context)
    }

    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        localRepository.onDownloadBefore(context)
    }

    override fun onDownloadFailed(context: ArtifactDownloadContext, exception: Exception) {
        localRepository.onDownloadFailed(context, exception)
    }

    override fun onDownloadFinished(context: ArtifactDownloadContext) {
        localRepository.onDownloadFinished(context)
    }

    /**
     * 下载成功后，如果是从本地下载，应该记录下载统计
     */
    override fun onDownloadSuccess(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
        throughput: Throughput
    ) {
        localRepository.onDownloadSuccess(context, artifactResource, throughput)
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        return localRepository.onDownload(context) ?: run {
            mapFirstProxyRepo(context) {
                require(it is ArtifactDownloadContext)
                remoteRepository.onDownload(it)
            }
        }
    }

    override fun query(context: ArtifactQueryContext): Any? {
        return localRepository.query(context) ?: run {
            mapFirstProxyRepo(context) {
                require(it is ArtifactQueryContext)
                remoteRepository.query(it)
            }
        }
    }

    override fun search(context: ArtifactSearchContext): List<Any> {
        val localResult = localRepository.search(context)
        return mapEachProxyRepo(context) {
            require(it is ArtifactSearchContext)
            remoteRepository.search(it)
        }.apply { add(localResult) }.flatten()
    }

    /**
     * 遍历代理仓库列表，执行[action]操作，当遇到代理仓库[action]操作返回非`null`时，立即返回结果[R]
     */
    private fun <R> mapFirstProxyRepo(context: ArtifactContext, action: (ArtifactContext) -> R?): R? {
        val proxyChannelList = getProxyChannelList(context)
        for (setting in proxyChannelList) {
            try {
                action(getContextFromProxyChannel(context, setting))?.let { return it }
            } catch (ignored: Exception) {
                logger.warn("Failed to execute map with channel ${setting.name}", ignored)
            }
        }
        return null
    }

    /**
     * 遍历代理仓库列表，执行[action]操作，并将结果聚合成[List]返回
     */
    private fun <R> mapEachProxyRepo(context: ArtifactContext, action: (ArtifactContext) -> R?): MutableList<R> {
        val proxyChannelList = getProxyChannelList(context)
        val mapResult = mutableListOf<R>()
        for (proxyChannel in proxyChannelList) {
            try {
                action(getContextFromProxyChannel(context, proxyChannel))?.let { mapResult.add(it) }
            } catch (ignored: Exception) {
                logger.warn("Failed to execute map with channel ${proxyChannel.name}", ignored)
            }
        }
        return mapResult
    }

    /**
     * 遍历代理仓库列表，执行[action]操作
     */
    private fun forEachProxyRepo(context: ArtifactContext, action: (ArtifactContext) -> Unit) {
        val proxyChannelList = getProxyChannelList(context)
        for (proxyChannel in proxyChannelList) {
            try {
                action(getContextFromProxyChannel(context, proxyChannel))
            } catch (ignored: Exception) {
                logger.warn("Failed to execute action with channel ${proxyChannel.name}", ignored)
            }
        }
    }

    /**
     * 获取代理源设置列表
     */
    private fun getProxyChannelList(context: ArtifactContext): List<ProxyChannelSetting> {
        return context.getCompositeConfiguration().proxy.channelList
    }

    /**
     * 根据原始上下文[context]以及代理源设置[setting]生成新的[ArtifactContext]
     */
    private fun getContextFromProxyChannel(context: ArtifactContext, setting: ProxyChannelSetting): ArtifactContext {
        return if (setting.public) {
            getContextFromPublicProxyChannel(context, setting)
        } else {
            getContextFromPrivateProxyChannel(context, setting)
        }
    }

    /**
     * 根据原始上下文[context]以及公共代理源设置[setting]生成新的[ArtifactContext]
     */
    private fun getContextFromPublicProxyChannel(
        context: ArtifactContext,
        setting: ProxyChannelSetting
    ): ArtifactContext {
        // 查询公共源详情
        val proxyChannel = proxyChannelClient.getById(setting.channelId!!).data!!
        // 查询远程仓库
        val repoType = proxyChannel.repoType.name
        val projectId = PUBLIC_PROXY_PROJECT
        val repoName = PUBLIC_PROXY_REPO_NAME.format(repoType, proxyChannel.name)
        val remoteRepoDetail = repositoryClient.getRepoDetail(projectId, repoName, repoType).data!!
        // 构造proxyConfiguration
        val remoteConfiguration = remoteRepoDetail.configuration
        require(remoteConfiguration is RemoteConfiguration)
        remoteConfiguration.url = proxyChannel.url
        remoteConfiguration.credentials.username = proxyChannel.username
        remoteConfiguration.credentials.password = proxyChannel.password

        return context.copy(remoteRepoDetail)
    }

    /**
     * 根据原始上下文[context]以及私有代理源设置[setting]生成新的[ArtifactContext]
     */
    private fun getContextFromPrivateProxyChannel(
        context: ArtifactContext,
        setting: ProxyChannelSetting
    ): ArtifactContext {
        // 查询远程仓库
        val projectId = context.repositoryDetail.projectId
        val repoType = context.repositoryDetail.type.name
        val repoName = PRIVATE_PROXY_REPO_NAME.format(context.repositoryDetail.name, setting.name)
        val remoteRepoDetail = repositoryClient.getRepoDetail(projectId, repoName, repoType).data!!
        // 构造proxyConfiguration
        val remoteConfiguration = remoteRepoDetail.configuration
        require(remoteConfiguration is RemoteConfiguration)
        remoteConfiguration.url = setting.url!!
        remoteConfiguration.credentials.username = setting.username
        remoteConfiguration.credentials.password = setting.password

        return context.copy(remoteRepoDetail)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CompositeRepository::class.java)
    }
}
