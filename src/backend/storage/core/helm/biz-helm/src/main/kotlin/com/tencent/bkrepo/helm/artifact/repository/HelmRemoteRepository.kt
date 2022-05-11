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

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.helm.constants.CHART
import com.tencent.bkrepo.helm.constants.FILE_TYPE
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.META_DETAIL
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.PROV
import com.tencent.bkrepo.helm.constants.SIZE
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.helm.exception.HelmForbiddenRequestException
import com.tencent.bkrepo.helm.service.impl.HelmOperationService
import com.tencent.bkrepo.helm.utils.ChartParserUtil
import com.tencent.bkrepo.helm.utils.HelmMetadataUtils
import com.tencent.bkrepo.helm.utils.HelmUtils
import com.tencent.bkrepo.helm.utils.ObjectBuilderUtil
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.MalformedURLException
import java.net.URL

@Component
class HelmRemoteRepository(
    private val helmOperationService: HelmOperationService
) : RemoteRepository() {

    override fun upload(context: ArtifactUploadContext) {
        with(context) {
            val message = "Forbidden to upload chart into a remote repository [$projectId/$repoName]"
            logger.warn(message)
            throw HelmForbiddenRequestException(message)
        }
    }

    /**
     * 远程下载响应回调
     */
    override fun onQueryResponse(context: ArtifactQueryContext, response: Response): Any? {
        logger.info("on remote query response...")
        val body = response.body()!!
        val artifactFile = createTempFile(body)
        val size = artifactFile.getSize()
        val result = checkNode(context, artifactFile)
        if (result == null) {
            logger.info("store the new helm file to replace the old version..")
            parseAttribute(context, artifactFile)
            cacheArtifactFile(context, artifactFile)
            return artifactFile.getInputStream().artifactStream(Range.full(size))
        }
        return result
    }

    /**
     * 如缓存存在，判断缓存文件和最新文件是否一样，如不一样以最新文件为准
     */
    private fun checkNode(context: ArtifactQueryContext, artifactFile: ArtifactFile): Any? {
        with(context) {
            val fullPath = getStringAttribute(FULL_PATH)!!
            logger.info(
                "Will go to check the artifact $fullPath in the cache " +
                    "in repo ${context.artifactInfo.getRepoIdentify()}"
            )
            val sha256 = artifactFile.getFileSha256()
            nodeClient.getNodeDetail(projectId, repoName, fullPath).data?.let {
                if (it.sha256.equals(sha256)) {
                    logger.info("artifact [$fullPath] hits the cache.")
                    return artifactFile.getInputStream().artifactStream(Range.full(artifactFile.getSize()))
                }
            }
            return null
        }
    }

    /**
     * 下载前回调
     */
    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        val fullPath = when (context.artifactInfo.getArtifactFullPath()) {
            HelmUtils.getIndexCacheYamlFullPath() -> HelmUtils.getIndexYamlFullPath()
            else -> helmOperationService.findRemoteArtifactFullPath(
                context.artifactInfo.getArtifactFullPath()
            )
        }
        context.putAttribute(FULL_PATH, fullPath)
    }

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        logger.info("create remote download url...")
        val remoteConfiguration = context.getRemoteConfiguration()
        val fullPath = context.getStringAttribute(FULL_PATH)!!.let { HelmUtils.convertIndexYamlPath(it) }
        return if (checkUrl(fullPath)) {
            fullPath
        } else {
            remoteConfiguration.url.trimEnd('/') + "/" + fullPath
        }
    }

    /**
     * 如果fullPath已经是完整的url，则直接使用，否则进行拼接
     */
    private fun checkUrl(fullPath: String): Boolean {
        return try {
            URL(fullPath)
            true
        } catch (e: MalformedURLException) {
            false
        }
    }

    /**
     * 远程下载响应回调
     */
    override fun onDownloadResponse(context: ArtifactDownloadContext, response: Response): ArtifactResource {
        val artifactFile = createTempFile(response.body()!!)
        val artifactStream = parseAttribute(context, artifactFile)
        val node = cacheArtifactFile(context, artifactFile)
        helmOperationService.initPackageInfo(context)
        return ArtifactResource(artifactStream, context.artifactInfo.getResponseName(), node, ArtifactChannel.LOCAL)
    }

    private fun parseAttribute(context: ArtifactContext, artifactFile: ArtifactFile): ArtifactInputStream {
        val size = artifactFile.getSize()
        context.putAttribute(SIZE, size)
        val artifactStream = artifactFile.getInputStream().artifactStream(Range.full(size))
        when (context.getStringAttribute(FILE_TYPE)) {
            CHART -> {
                val helmChartMetadata = ChartParserUtil.parseChartInputStream(artifactStream)
                helmChartMetadata.let {
                    context.putAttribute(NAME, it.name)
                    context.putAttribute(VERSION, it.version)
                    context.putAttribute(META_DETAIL, HelmMetadataUtils.convertToMap(helmChartMetadata))
                }
            }
            PROV -> ChartParserUtil.parseNameAndVersion(context)
        }
        return artifactStream
    }

    /**
     * 获取缓存节点创建请求
     */
    override fun buildCacheNodeCreateRequest(context: ArtifactContext, artifactFile: ArtifactFile): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = context.projectId,
            repoName = context.repoName,
            folder = false,
            fullPath = HelmUtils.convertIndexYamlPathToCache(context.getStringAttribute(FULL_PATH)!!),
            size = artifactFile.getSize(),
            sha256 = artifactFile.getFileSha256(),
            md5 = artifactFile.getFileMd5(),
            operator = context.userId,
            metadata = context.getAttribute(META_DETAIL),
            overwrite = true
        )
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        return ObjectBuilderUtil.buildDownloadRecordRequest(context)
    }

    /**
     * 删除本地缓存chart包
     */
    override fun remove(context: ArtifactRemoveContext) {
        helmOperationService.removeChartOrProv(context)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(HelmRemoteRepository::class.java)
    }
}
