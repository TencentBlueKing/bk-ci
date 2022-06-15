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

package com.tencent.bkrepo.generic.artifact

import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.exception.BadRequestException
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.constant.PARAM_PREVIEW
import com.tencent.bkrepo.common.artifact.constant.X_CHECKSUM_MD5
import com.tencent.bkrepo.common.artifact.constant.X_CHECKSUM_SHA256
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.generic.constant.BKREPO_META
import com.tencent.bkrepo.generic.constant.BKREPO_META_PREFIX
import com.tencent.bkrepo.generic.constant.GenericMessageCode
import com.tencent.bkrepo.generic.constant.HEADER_EXPIRES
import com.tencent.bkrepo.generic.constant.HEADER_MD5
import com.tencent.bkrepo.generic.constant.HEADER_OVERWRITE
import com.tencent.bkrepo.generic.constant.HEADER_SEQUENCE
import com.tencent.bkrepo.generic.constant.HEADER_SHA256
import com.tencent.bkrepo.generic.constant.HEADER_UPLOAD_ID
import com.tencent.bkrepo.repository.constant.NODE_DETAIL_LIST_KEY
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.unit.DataSize
import java.net.URLDecoder
import java.util.Base64
import javax.servlet.http.HttpServletRequest

@Component
class GenericLocalRepository : LocalRepository() {

    override fun onUploadBefore(context: ArtifactUploadContext) {
        super.onUploadBefore(context)
        // 若不允许覆盖, 提前检查节点是否存在
        val overwrite = HeaderUtils.getBooleanHeader(HEADER_OVERWRITE)
        val uploadId = HeaderUtils.getHeader(HEADER_UPLOAD_ID)
        val sequence = HeaderUtils.getHeader(HEADER_SEQUENCE)?.toInt()
        if (!overwrite && !isBlockUpload(uploadId, sequence)) {
            with(context.artifactInfo) {
                nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data?.let {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, getArtifactName())
                }
            }
        }
        // 校验sha256
        val calculatedSha256 = context.getArtifactSha256()
        val uploadSha256 = HeaderUtils.getHeader(HEADER_SHA256)
        if (uploadSha256 != null && !calculatedSha256.equals(uploadSha256, true)) {
            throw ErrorCodeException(ArtifactMessageCode.DIGEST_CHECK_FAILED, "sha256")
        }
        // 校验md5
        val calculatedMd5 = context.getArtifactMd5()
        val uploadMd5 = HeaderUtils.getHeader(HEADER_MD5)
        if (uploadMd5 != null && !calculatedMd5.equals(uploadMd5, true)) {
            throw ErrorCodeException(ArtifactMessageCode.DIGEST_CHECK_FAILED, "md5")
        }
    }

    override fun onUpload(context: ArtifactUploadContext) {
        val uploadId = context.request.getHeader(HEADER_UPLOAD_ID)
        val sequence = context.request.getHeader(HEADER_SEQUENCE)?.toInt()
        if (isBlockUpload(uploadId, sequence)) {
            this.blockUpload(uploadId, sequence!!, context)
            context.response.contentType = MediaTypes.APPLICATION_JSON
            context.response.writer.println(ResponseBuilder.success().toJsonString())
        } else {
            val nodeDetail = storageManager.storeArtifactFile(
                buildNodeCreateRequest(context),
                context.getArtifactFile(),
                context.storageCredentials
            )
            context.response.contentType = MediaTypes.APPLICATION_JSON
            context.response.addHeader(X_CHECKSUM_MD5, context.getArtifactMd5())
            context.response.addHeader(X_CHECKSUM_SHA256, context.getArtifactSha256())
            context.response.writer.println(ResponseBuilder.success(nodeDetail).toJsonString())
        }
    }

    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        // 文件默认下载，设置Content-Dispostition响应头
        // preview == true时不设置Content-Dispostition响应头
        val preview = context.request.getParameter(PARAM_PREVIEW)?.toBoolean()
        context.useDisposition = preview == null || preview == false
        if (context.repo.name == REPORT) {
            context.useDisposition = false
        }
    }

    /**
     * 支持单文件、目录、批量文件下载
     * 目录下载会以zip包形式将目录下的文件打包下载
     * 批量文件下载会以zip包形式将文件打包下载
     */
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        return if (context.artifacts.isNullOrEmpty()) {
            downloadSingleNode(context)
        } else {
            downloadMultiNode(context)
        }
    }

    /**
     * 单节点下载，支持目录下载
     */
    private fun downloadSingleNode(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            val node = getNodeDetailsFromReq(true)?.firstOrNull()
                ?: nodeClient.getNodeDetail(projectId, repoName, artifactInfo.getArtifactFullPath()).data
                ?: return null
            if (node.folder) {
                return downloadFolder(this, node)
            }
            downloadIntercept(this, node)
            val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials) ?: return null
            val responseName = artifactInfo.getResponseName()

            return ArtifactResource(inputStream, responseName, node, ArtifactChannel.LOCAL, useDisposition)
        }
    }

    /**
     * 多节点下载， 节点不允许为目录
     */
    private fun downloadMultiNode(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            var prefix = artifacts!!.first().getArtifactFullPath()
            val fullPathList = artifacts!!.map { it.getArtifactFullPath() }
            fullPathList.forEach {
                prefix = PathUtils.getCommonPath(prefix, it)
            }
            val nodes = getNodeDetailsFromReq(false)
                ?: queryNodeDetailList(
                    projectId = artifacts!!.first().projectId,
                    repoName = artifacts!!.first().repoName,
                    paths = fullPathList,
                    prefix = prefix
                )
            val notExistNodes = fullPathList.subtract(nodes.map { it.fullPath })
            if (notExistNodes.isNotEmpty()) {
                throw NodeNotFoundException(notExistNodes.joinToString(StringPool.COMMA))
            }
            nodes.forEach { downloadIntercept(this, it) }
            val nodeMap = nodes.associate {
                val name = it.fullPath.removePrefix(prefix)
                val inputStream = storageManager.loadArtifactInputStream(it, context.storageCredentials)
                    ?: throw ArtifactNotFoundException(it.fullPath)
                name to inputStream
            }
            return ArtifactResource(nodeMap, useDisposition = true)
        }
    }

    private fun queryNodeDetailList(
        projectId: String,
        repoName: String,
        paths: List<String>,
        prefix: String
    ): List<NodeDetail> {
        var pageNumber = 1
        val nodeDetailList = mutableListOf<NodeDetail>()
        do {
            val option = NodeListOption(
                pageNumber = pageNumber,
                pageSize = 1000,
                includeFolder = true,
                includeMetadata = true,
                deep = true
            )
            val records = nodeClient.listNodePage(projectId, repoName, prefix, option).data?.records
            if (records.isNullOrEmpty()) {
                break
            }
            nodeDetailList.addAll(
                records.filter { paths.contains(it.fullPath) }.map {
                    if (it.folder) {
                        throw BadRequestException(GenericMessageCode.DOWNLOAD_DIR_NOT_ALLOWED)
                    }
                    NodeDetail(it)
                }
            )
            pageNumber ++
        } while (nodeDetailList.size < paths.size)
        return nodeDetailList
    }

    /**
     * 下载目录
     * @param context 构件下载context
     * @param node 目录节点详情
     */
    private fun downloadFolder(context: ArtifactDownloadContext, node: NodeDetail): ArtifactResource? {
        // 检查文件数量
        checkFileCount(node)
        // 查询子节点
        val nodes = nodeClient.listNode(
            projectId = node.projectId,
            repoName = node.repoName,
            path = node.fullPath,
            includeFolder = false,
            deep = true
        ).data.orEmpty()
        // 检查目录大小
        checkFolderSize(nodes)
        nodes.forEach {
            val nodeDetail = NodeDetail(it)
            downloadIntercept(context, nodeDetail)
        }
        // 构造name-node map
        val prefix = "${node.fullPath}/"
        val nodeMap = nodes.associate {
            val name = it.fullPath.removePrefix(prefix)
            val inputStream = storageManager.loadArtifactInputStream(it, context.storageCredentials) ?: return null
            name to inputStream
        }
        return ArtifactResource(nodeMap, node, useDisposition = true)
    }

    private fun downloadIntercept(context: ArtifactDownloadContext, nodeDetail: NodeDetail) {
        val interceptors = context.getInterceptors()
        interceptors.forEach { it.intercept(nodeDetail) }
    }

    private fun getNodeDetailsFromReq(allowFolder: Boolean): List<NodeDetail>? {
        val nodeDetailList = HttpContextHolder.getRequest().getAttribute(NODE_DETAIL_LIST_KEY) as? List<NodeDetail>
        nodeDetailList?.forEach {
            if (!allowFolder && it.folder) {
                throw BadRequestException(GenericMessageCode.DOWNLOAD_DIR_NOT_ALLOWED)
            }
        }
        return nodeDetailList
    }
    /**
     * 检查文件数量是否超过阈值
     * @throws ErrorCodeException 超过阈值抛出NODE_LIST_TOO_LARGE类型ErrorCodeException
     */
    @Throws(ErrorCodeException::class)
    private fun checkFileCount(node: NodeDetail) {
        // 判断节点数量
        val fileCount = nodeClient.countFileNode(node.projectId, node.repoName, node.fullPath).data ?: 0
        if (fileCount > BATCH_DOWNLOAD_COUNT_THRESHOLD) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_LIST_TOO_LARGE)
        }
    }

    /**
     * 检查目录数据大小是否超过阈值
     * @throws ErrorCodeException 超过阈值抛出NODE_LIST_TOO_LARGE类型ErrorCodeException
     */
    @Throws(ErrorCodeException::class)
    private fun checkFolderSize(nodes: List<NodeInfo>) {
        val totalSize = nodes.map { it.size }.sum()
        if (totalSize > BATCH_DOWNLOAD_SIZE_THRESHOLD) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_LIST_TOO_LARGE)
        }
    }

    override fun remove(context: ArtifactRemoveContext) {
        with(context.artifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data
                ?: throw NodeNotFoundException(this.getArtifactFullPath())
            if (node.folder) {
                if (nodeClient.countFileNode(projectId, repoName, getArtifactFullPath()).data!! > 0) {
                    throw ErrorCodeException(ArtifactMessageCode.FOLDER_CONTAINS_FILE)
                }
            }
            val nodeDeleteRequest = NodeDeleteRequest(projectId, repoName, getArtifactFullPath(), context.userId)
            nodeClient.deleteNode(nodeDeleteRequest)
        }
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        return super.buildNodeCreateRequest(context).copy(
            expires = HeaderUtils.getLongHeader(HEADER_EXPIRES),
            overwrite = HeaderUtils.getBooleanHeader(HEADER_OVERWRITE),
            metadata = resolveMetadata(context.request)
        )
    }

    /**
     * 判断是否为分块上传
     */
    private fun isBlockUpload(uploadId: String?, sequence: Int?): Boolean {
        return !uploadId.isNullOrBlank() && sequence != null
    }

    /**
     * 上传分块
     */
    private fun blockUpload(uploadId: String, sequence: Int, context: ArtifactUploadContext) {
        with(context) {
            if (!storageService.checkBlockId(uploadId, storageCredentials)) {
                throw ErrorCodeException(GenericMessageCode.UPLOAD_ID_NOT_FOUND, uploadId)
            }
            storageService.storeBlock(
                uploadId,
                sequence,
                getArtifactSha256(),
                getArtifactFile(),
                HeaderUtils.getBooleanHeader(HEADER_OVERWRITE),
                storageCredentials
            )
        }
    }

    /**
     * 从header中提取metadata
     */
    fun resolveMetadata(request: HttpServletRequest): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        // case insensitive
        val headerNames = request.headerNames
        for (headerName in headerNames) {
            if (headerName.startsWith(BKREPO_META_PREFIX, true)) {
                val key = headerName.substring(BKREPO_META_PREFIX.length).trim().toLowerCase()
                if (key.isNotBlank()) {
                    metadata[key] = HeaderUtils.getUrlDecodedHeader(headerName)!!
                }
            }
        }
        // case sensitive, base64 metadata
        // format X-BKREPO-META: base64(a=1&b=2)
        request.getHeader(BKREPO_META)?.let { metadata.putAll(decodeMetadata(it)) }
        return metadata
    }

    private fun decodeMetadata(header: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            val metadataUrl = String(Base64.getDecoder().decode(header))
            metadataUrl.split(CharPool.AND).forEach { part ->
                val pair = part.trim().split(CharPool.EQUAL, limit = 2)
                if (pair.size > 1 && pair[0].isNotBlank() && pair[1].isNotBlank()) {
                    val key = URLDecoder.decode(pair[0], StringPool.UTF_8)
                    val value = URLDecoder.decode(pair[1], StringPool.UTF_8)
                    metadata[key] = value
                }
            }
        } catch (exception: IllegalArgumentException) {
            logger.warn("$header is not in valid Base64 scheme.")
        }
        return metadata
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GenericLocalRepository::class.java)

        /**
         * 目录下载，子文件数量阈值
         */
        private const val BATCH_DOWNLOAD_COUNT_THRESHOLD = 1024

        /**
         * 目录下载，目录大小阈值
         */
        private val BATCH_DOWNLOAD_SIZE_THRESHOLD = DataSize.ofGigabytes(10).toBytes()

        private const val REPORT = "report"
    }
}
