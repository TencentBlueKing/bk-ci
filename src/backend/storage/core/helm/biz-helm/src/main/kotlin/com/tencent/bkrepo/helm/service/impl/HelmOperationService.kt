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

package com.tencent.bkrepo.helm.service.impl

import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.helm.artifact.repository.HelmLocalRepository
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.pojo.artifact.HelmDeleteArtifactInfo
import com.tencent.bkrepo.helm.pojo.metadata.HelmChartMetadata
import com.tencent.bkrepo.helm.utils.ChartParserUtil
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import java.util.SortedSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HelmOperationService : AbstractChartService() {

    /**
     * 删除chart或者prov
     */
    fun removeChartOrProv(context: ArtifactRemoveContext) {
        with(context.artifactInfo as HelmDeleteArtifactInfo) {
            if (version.isNotBlank()) {
                packageClient.findVersionByName(projectId, repoName, packageName, version).data?.let {
                    removeVersion(
                        projectId = projectId,
                        repoName = repoName,
                        packageKey = packageName,
                        version = it.name,
                        userId = context.userId
                    )
                } ?: throw VersionNotFoundException(version)
            } else {
                packageClient.listAllVersion(projectId, repoName, packageName).data.orEmpty().forEach {
                    removeVersion(
                        projectId = projectId,
                        repoName = repoName,
                        packageKey = packageName,
                        version = it.name,
                        userId = context.userId
                    )
                }
            }
            updatePackageExtension(context)
        }
    }

    /**
     * 删除[version] 对应的node节点也会一起删除
     */
    fun removeVersion(
        projectId: String,
        repoName: String,
        version: String,
        packageKey: String,
        userId: String
    ) {
        packageClient.deleteVersion(projectId, repoName, packageKey, version)
        val packageName = PackageKeys.resolveHelm(packageKey)
        val chartPath = HelmUtils.getChartFileFullPath(packageName, version)
        val provPath = HelmUtils.getProvFileFullPath(packageName, version)
        if (chartPath.isNotBlank()) {
            val request = NodeDeleteRequest(projectId, repoName, chartPath, userId)
            nodeClient.deleteNode(request)
        }
        if (provPath.isNotBlank()) {
            nodeClient.deleteNode(NodeDeleteRequest(projectId, repoName, provPath, userId))
        }
    }

    /**
     * 节点删除后，将package extension信息更新
     */
    private fun updatePackageExtension(context: ArtifactRemoveContext) {
        with(context.artifactInfo as HelmDeleteArtifactInfo) {
            val version = packageClient.findPackageByKey(projectId, repoName, packageName).data?.latest
            try {
                val chartPath = HelmUtils.getChartFileFullPath(getArtifactName(), version!!)
                val map = nodeClient.getNodeDetail(projectId, repoName, chartPath).data?.metadata
                val chartInfo = map?.let { it1 -> HelmMetadataUtils.convertToObject(it1) }
                chartInfo?.appVersion?.let {
                    val packageUpdateRequest = ObjectBuilderUtil.buildPackageUpdateRequest(
                        projectId = projectId,
                        repoName = repoName,
                        name = PackageKeys.resolveHelm(packageName),
                        appVersion = chartInfo.appVersion!!,
                        description = chartInfo.description
                    )
                    packageClient.updatePackage(packageUpdateRequest)
                }
            } catch (e: Exception) {
                HelmLocalRepository.logger.warn("can not convert meta data")
            }
        }
    }

    /**
     * 初次创建仓库时根据index更新package信息
     */
    fun initPackageInfo(projectId: String, repoName: String, userId: String) {
        val helmIndexYamlMetadata = initIndexYaml(projectId, repoName, userId)
        helmIndexYamlMetadata?.entries?.forEach { element ->
            element.value.forEach {
                createVersion(
                    userId = userId,
                    projectId = projectId,
                    repoName = repoName,
                    chartInfo = it
                )
            }
        }
    }

    /**
     * 更新remote仓库index.yaml文件，并刷新对应的package信息
     */
    fun updatePackageForRemote(projectId: String, name: String, userId: String = SecurityUtils.getUserId()) {
        logger.info("Will start to update package info for repo $projectId|$name")
        val repoDetail = checkRepo(projectId, name) ?: return
        if (!checkConfigType(repoDetail.configuration)) return
        // 获取本地index文件
        val oldIndex = try {
            getOriginalIndexYaml(projectId, name)
        } catch (ignore: HelmFileNotFoundException) {
            HelmUtils.initIndexYamlMetadata()
        }
        // 获取最新文件
        val newIndex = getIndex(repoDetail) ?: return

        val (deletedSet, addedSet) = ChartParserUtil.compareIndexYamlMetadata(
            oldEntries = oldIndex.entries,
            newEntries = newIndex.entries
        )
        // 针对composite类型，local中的优先级最高，代理中的顺序在前优先级高，所以不删除本地的、
        addedSet.forEach { (k, v) ->
            if (oldIndex.entries[k].isNullOrEmpty()) {
                oldIndex.entries[k] = v
            } else {
                oldIndex.entries[k]?.addAll(v)
            }
        }
        // 存储新index文件
        storeIndex(
            indexYamlMetadata = oldIndex,
            projectId = projectId,
            repoName = name,
            userId = userId
        )
        // 更新package信息
        updatePackage(
            addedSet = addedSet,
            deletedSet = deletedSet,
            projectId = projectId,
            name = name,
            userId = userId,
            repoDetail = repoDetail
        )
    }

    /**
     * 只针对remote config和composite config中list不为空的情况下进行刷新
     */
    private fun checkConfigType(configuration: RepositoryConfiguration): Boolean {
        if (configuration is CompositeConfiguration) {
            if (configuration.proxy.channelList.isNotEmpty()) return true
        }
        if (configuration is RemoteConfiguration) return true
        return false
    }

    /**
     * 对比新老index文件中的chart信息，刷新package信息
     */
    private fun updatePackage(
        addedSet: MutableMap<String, SortedSet<HelmChartMetadata>>,
        deletedSet: MutableMap<String, SortedSet<HelmChartMetadata>>,
        projectId: String,
        name: String,
        userId: String,
        repoDetail: RepositoryDetail
    ) {
        logger.info("Packages will be updated in repo $projectId|$name")
        // 对新增的chart进行插入
        addedSet.forEach { element ->
            element.value.forEach {
                createVersion(
                    userId = userId,
                    projectId = projectId,
                    repoName = name,
                    chartInfo = it
                )
            }
        }
        if (repoDetail.configuration is RemoteConfiguration) {
            // 对需要删除的chart进行删除
            deletedSet.forEach { element ->
                element.value.forEach {
                    removeVersion(
                        projectId = projectId,
                        repoName = name,
                        version = it.version,
                        packageKey = PackageKeys.ofHelm(it.name),
                        userId = userId
                    )
                }
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(HelmOperationService::class.java)
    }
}
