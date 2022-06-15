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

package com.tencent.bkrepo.rds.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.util.readYamlString
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.rds.constants.CHART
import com.tencent.bkrepo.rds.constants.EXTENSION
import com.tencent.bkrepo.rds.constants.FILE_TYPE
import com.tencent.bkrepo.rds.constants.FULL_PATH
import com.tencent.bkrepo.rds.constants.INDEX_CACHE_YAML
import com.tencent.bkrepo.rds.constants.NODE_CREATE_DATE
import com.tencent.bkrepo.rds.constants.NODE_FULL_PATH
import com.tencent.bkrepo.rds.constants.NODE_METADATA
import com.tencent.bkrepo.rds.constants.NODE_SHA256
import com.tencent.bkrepo.rds.constants.SLEEP_MILLIS
import com.tencent.bkrepo.rds.exception.RdsFileNotFoundException
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo
import com.tencent.bkrepo.rds.pojo.metadata.RdsChartMetadata
import com.tencent.bkrepo.rds.pojo.metadata.RdsIndexYamlMetadata
import com.tencent.bkrepo.rds.service.ChartRepositoryService
import com.tencent.bkrepo.rds.utils.ChartParserUtil
import com.tencent.bkrepo.rds.utils.DecompressUtil.getArchivesContent
import com.tencent.bkrepo.rds.utils.ObjectBuilderUtil
import com.tencent.bkrepo.rds.utils.RdsUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChartRepositoryServiceImpl : AbstractChartService(), ChartRepositoryService {

    @Permission(ResourceType.REPO, PermissionAction.READ)
    override fun queryIndexYaml(artifactInfo: RdsArtifactInfo) {
        lockAction(artifactInfo.projectId, artifactInfo.repoName) { downloadIndex(artifactInfo) }
    }

    private fun downloadIndex(artifactInfo: RdsArtifactInfo) {
        // 创建仓库后，index.yaml文件时没有生成的，需要生成默认的
        if (!exist(artifactInfo.projectId, artifactInfo.repoName, RdsUtils.getIndexCacheYamlFullPath())) {
            val (artifactFile, nodeCreateRequest) = ObjectBuilderUtil.buildFileAndNodeCreateRequest(
                indexYamlMetadata = RdsUtils.initIndexYamlMetadata(),
                projectId = artifactInfo.projectId,
                repoName = artifactInfo.repoName,
                operator = SecurityUtils.getUserId()
            )
            uploadIndexYamlMetadata(artifactFile, nodeCreateRequest)
        }
        downloadIndexYaml()
    }

    @Suppress("UNCHECKED_CAST")
    override fun buildIndexYamlMetadata(
        result: List<Map<String, Any?>>,
        artifactInfo: RdsArtifactInfo,
        isInit: Boolean
    ): RdsIndexYamlMetadata {
        val indexYamlMetadata =
            if (!exist(artifactInfo.projectId, artifactInfo.repoName, RdsUtils.getIndexYamlFullPath()) || isInit) {
                RdsUtils.initIndexYamlMetadata()
            } else {
                queryOriginalIndexYaml()
            }
        if (result.isEmpty()) return indexYamlMetadata
        val context = ArtifactQueryContext()
        result.forEach {
            val fullPath = it[NODE_FULL_PATH] as String
            if (!fullPath.endsWith(INDEX_CACHE_YAML)) {
                Thread.sleep(SLEEP_MILLIS)
                var chartName: String? = null
                var chartVersion: String? = null
                try {
                    val chartMetadata = queryRdsChartMetadata(context, it)
                    chartName = chartMetadata.code
                    chartVersion = chartMetadata.version
                    chartMetadata.created = convertDateTime(it[NODE_CREATE_DATE] as String)
                    chartMetadata.digest = it[NODE_SHA256] as String
                    ChartParserUtil.addIndexEntries(indexYamlMetadata, chartMetadata)
                } catch (ex: Exception) {
                    logger.warn(
                        "generate indexFile for chart [$chartName-$chartVersion] in " +
                            "[${artifactInfo.getRepoIdentify()}] failed, ${ex.message}"
                    )
                }
            }
        }
        return indexYamlMetadata
    }

    private fun queryRdsChartMetadata(context: ArtifactQueryContext, nodeInfo: Map<String, Any?>): RdsChartMetadata {
        val nodePath = nodeInfo[NODE_FULL_PATH] as String
        val metaData = nodeInfo[NODE_METADATA] as Map<*, *>
        context.putAttribute(FULL_PATH, nodePath)
        val artifactInputStream =
            ArtifactContextHolder.getRepository().query(context) as ArtifactInputStream
        val content = artifactInputStream.use {
            it.getArchivesContent(metaData[EXTENSION] as String)
        }
        return content.byteInputStream().readYamlString()
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    override fun installTgz(artifactInfo: RdsArtifactInfo) {
        try {
            val context = ArtifactDownloadContext()
            context.putAttribute(FULL_PATH, artifactInfo.getArtifactFullPath())
            context.putAttribute(FILE_TYPE, CHART)
            ArtifactContextHolder.getRepository().download(context)
        } catch (e: Exception) {
            logger.warn("Error occurred while installing chart, error: ${e.message}")
            throw RdsFileNotFoundException(e.message.toString())
        }
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    override fun regenerateIndexYaml(artifactInfo: RdsArtifactInfo) {
        val nodeList = queryNodeList(artifactInfo, false)
        logger.info(
            "query node list for full refresh index.yaml success in repo [${artifactInfo.getRepoIdentify()}]" +
                ", size [${nodeList.size}], starting full refresh index.yaml ... "
        )
        val indexYamlMetadata = buildIndexYamlMetadata(nodeList, artifactInfo, true)
        uploadIndexYamlMetadata(indexYamlMetadata).also { logger.info("Full refresh index.yaml success！") }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChartRepositoryServiceImpl::class.java)
    }
}
