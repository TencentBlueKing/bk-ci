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

package com.tencent.bkrepo.helm.artifact.repository

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.helm.constants.CHART
import com.tencent.bkrepo.helm.constants.FILE_TYPE
import com.tencent.bkrepo.helm.constants.FORCE
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.META_DETAIL
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.OVERWRITE
import com.tencent.bkrepo.helm.constants.PROV
import com.tencent.bkrepo.helm.constants.SIZE
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.helm.exception.HelmBadRequestException
import com.tencent.bkrepo.helm.exception.HelmFileAlreadyExistsException
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.listener.event.ChartDeleteEvent
import com.tencent.bkrepo.helm.listener.event.ChartUploadEvent
import com.tencent.bkrepo.helm.listener.event.ChartVersionDeleteEvent
import com.tencent.bkrepo.helm.pojo.artifact.HelmDeleteArtifactInfo
import com.tencent.bkrepo.helm.pojo.chart.ChartPackageDeleteRequest
import com.tencent.bkrepo.helm.pojo.chart.ChartVersionDeleteRequest
import com.tencent.bkrepo.helm.service.impl.HelmOperationService
import com.tencent.bkrepo.helm.utils.ChartParserUtil
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HelmLocalRepository(
    private val helmOperationService: HelmOperationService
) : LocalRepository() {

    override fun onUploadBefore(context: ArtifactUploadContext) {
        with(context) {
            super.onUploadBefore(context)
            val size = getArtifactFile().getSize()
            putAttribute(SIZE, size)
            when (getStringAttribute(FILE_TYPE)) {
                CHART -> {
                    try {
                        val chartMetadata = ChartParserUtil.parseChartFileInfo(context.getArtifactFile())
                        putAttribute(
                            FULL_PATH,
                            HelmUtils.getChartFileFullPath(chartMetadata.name, chartMetadata.version)
                        )
                        putAttribute(META_DETAIL, HelmMetadataUtils.convertToMap(chartMetadata))
                        putAttribute(NAME, chartMetadata.name)
                        putAttribute(VERSION, chartMetadata.version)
                    } catch (e: Exception) {
                        throw HelmBadRequestException("The chart is broken.....")
                    }
                }
                PROV -> {
                    val provFileInfo = ChartParserUtil.parseProvFileInfo(context.getArtifactFile())
                    putAttribute(FULL_PATH, HelmUtils.getProvFileFullPath(provFileInfo.first, provFileInfo.second))
                }
            }
            // 判断是否是强制上传
            val isForce = request.getParameter(FORCE)?.let { true } ?: false
            putAttribute(FORCE, isForce)
            val repositoryDetail = repositoryDetail
            val projectId = repositoryDetail.projectId
            val repoName = repositoryDetail.name
            val fullPath = getStringAttribute(FULL_PATH).orEmpty()
            val isExist = nodeClient.checkExist(projectId, repoName, fullPath).data!!
            val isOverwrite = isOverwrite(fullPath, isForce)
            putAttribute(OVERWRITE, isOverwrite)
            if (isExist && !isOverwrite) {
                throw HelmFileAlreadyExistsException("${fullPath.trimStart('/')} already exists")
            }
        }
    }

    /**
     * 上传成功回调
     */
    override fun onUploadSuccess(context: ArtifactUploadContext) {
        super.onUploadSuccess(context)
        helmOperationService.initPackageInfo(context)
        if (CHART == context.getStringAttribute(FILE_TYPE)) {
            publishEvent(
                ChartUploadEvent(
                    ObjectBuilderUtil.buildChartUploadRequest(context)
                )
            )
        }
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        with(context) {
            val fullPath = getStringAttribute(FULL_PATH).orEmpty()
            val isForce = getBooleanAttribute(FORCE)!!

            return NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = false,
                fullPath = fullPath,
                size = getLongAttribute(SIZE),
                sha256 = getArtifactSha256(),
                md5 = getArtifactMd5(),
                operator = userId,
                metadata = parseMetaData(context),
                overwrite = isOverwrite(fullPath, isForce)
            )
        }
    }

    /**
     * 下载前回调
     */
    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        context.putAttribute(FULL_PATH, context.artifactInfo.getArtifactFullPath())
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        val fullPath = context.getStringAttribute(FULL_PATH)!!
        val node = nodeClient.getNodeDetail(context.projectId, context.repoName, fullPath).data
        node?.let {
            node.metadata[NAME]?.let { context.putAttribute(NAME, it) }
            node.metadata[VERSION]?.let { context.putAttribute(VERSION, it) }
        }
        val inputStream = storageManager.loadArtifactInputStream(node, context.storageCredentials)
        inputStream?.let {
            return ArtifactResource(
                inputStream,
                context.artifactInfo.getResponseName(),
                node,
                ArtifactChannel.LOCAL,
                context.useDisposition
            )
        }
        return null
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        return ObjectBuilderUtil.buildDownloadRecordRequest(context)
    }

    override fun query(context: ArtifactQueryContext): ArtifactInputStream? {
        val fullPath = context.getStringAttribute(FULL_PATH)!!
        return this.onQuery(context) ?: throw HelmFileNotFoundException("Artifact[$fullPath] does not exist")
    }

    private fun onQuery(context: ArtifactQueryContext): ArtifactInputStream? {
        val repositoryDetail = context.repositoryDetail
        val projectId = repositoryDetail.projectId
        val repoName = repositoryDetail.name
        val fullPath = context.getStringAttribute(FULL_PATH)!!
        val node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
        if (node == null || node.folder) return null
        return storageService.load(
            node.sha256!!, Range.full(node.size), context.storageCredentials
        )
    }

    /**
     * 版本不存在时 status code 404
     */
    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun remove(context: ArtifactRemoveContext) {
        helmOperationService.removeChartOrProv(context)
        with(context.artifactInfo as HelmDeleteArtifactInfo) {
            val event = if (version.isBlank()) {
                ChartDeleteEvent(
                    ChartPackageDeleteRequest(
                        projectId = projectId,
                        repoName = repoName,
                        name = PackageKeys.resolveHelm(packageName),
                        operator = context.userId
                    )
                )
            } else {
                ChartVersionDeleteEvent(
                    ChartVersionDeleteRequest(
                        projectId = projectId,
                        repoName = repoName,
                        name = PackageKeys.resolveHelm(packageName),
                        version = version,
                        operator = context.userId
                    )
                )
            }
            publishEvent(event)
        }
    }

    private fun parseMetaData(context: ArtifactUploadContext): Map<String, Any>? {
        with(context) {
            val fullPath = getStringAttribute(FULL_PATH)
            val forceUpdate = getBooleanAttribute(FORCE)
            val fileType = getStringAttribute(FILE_TYPE)
            var result: Map<String, Any>? = emptyMap()
            if (!isOverwrite(fullPath!!, forceUpdate!!)) {
                when (fileType) {
                    CHART -> result = getAttribute(META_DETAIL)
                    PROV -> result = ChartParserUtil.parseNameAndVersion(fullPath)
                }
            }
            return result
        }
    }

    private fun isOverwrite(fullPath: String, isForce: Boolean): Boolean {
        return isForce || !(fullPath.trim().endsWith(".tgz", true) || fullPath.trim().endsWith(".prov", true))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(HelmLocalRepository::class.java)
    }
}
