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

package com.tencent.bkrepo.oci.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.oci.constant.FORCE
import com.tencent.bkrepo.oci.constant.LAST_TAG
import com.tencent.bkrepo.oci.constant.MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.N
import com.tencent.bkrepo.oci.constant.OCI_IMAGE_MANIFEST_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.OLD_DOCKER_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.PATCH
import com.tencent.bkrepo.oci.constant.POST
import com.tencent.bkrepo.oci.exception.OciFileNotFoundException
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciTagArtifactInfo
import com.tencent.bkrepo.oci.pojo.digest.OciDigest
import com.tencent.bkrepo.oci.pojo.tags.TagsInfo
import com.tencent.bkrepo.oci.service.OciOperationService
import com.tencent.bkrepo.oci.util.OciLocationUtils
import com.tencent.bkrepo.oci.util.OciResponseUtils
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OciRegistryLocalRepository(
    private val ociOperationService: OciOperationService
) : LocalRepository() {

    /**
     * 上传前回调
     */
    override fun onUploadBefore(context: ArtifactUploadContext) {
        with(context) {
            super.onUploadBefore(context)
            val requestMethod = request.method
            if (PATCH == requestMethod) {
                logger.info("Will using patch ways to upload file in repo ${artifactInfo.getRepoIdentify()}")
                return
            }
            val isForce = request.getParameter(FORCE)?.let { true } ?: false
            val projectId = repositoryDetail.projectId
            val repoName = repositoryDetail.name
            val fullPath = context.artifactInfo.getArtifactFullPath()
            val isExist = nodeClient.checkExist(projectId, repoName, fullPath).data!!
            logger.info(
                "The file $fullPath that will be uploaded to server is exist: $isExist " +
                    "in repo ${artifactInfo.getRepoIdentify()}, and the flag of force overwrite is $isForce"
            )
            if (isExist && !isForce) {
                logger.warn(
                    "${fullPath.trimStart('/')} already exists in repo ${artifactInfo.getRepoIdentify()}"
                )
                return
            }
        }
    }

    /**
     * 从Content-Range头中解析出起始位置
     */
    private fun getRangeInfo(range: String): Pair<Long, Long> {
        val values = range.split("-")
        return Pair(values[0].toLong(), values[1].toLong())
    }

    /**
     * 上传
     */
    override fun onUpload(context: ArtifactUploadContext) {
        logger.info("Preparing to upload the oci file in repo ${context.artifactInfo.getRepoIdentify()}")
        val requestMethod = context.request.method
        if (PATCH == requestMethod) {
            patchUpload(context)
        } else {
            val (digest, location) = if (POST == requestMethod) {
                postUpload(context)
            } else {
                putUpload(context)
            }
            logger.info(
                "Artifact ${context.artifactInfo.getArtifactFullPath()} has been uploaded " +
                    "and will can be accessed in $location" +
                    " in repo ${context.artifactInfo.getRepoIdentify()}"
            )
            if (digest == null || location.isNullOrEmpty()) return
            val domain = ociOperationService.getReturnDomain(HttpContextHolder.getRequest())
            OciResponseUtils.buildUploadResponse(
                domain = domain,
                digest = digest,
                locationStr = location,
                response = context.response
            )
        }
    }

    /**
     * blob chunks上传中的patch上传部分逻辑处理
     * Pushing a blob in chunks
     * A chunked blob upload is accomplished in three phases:
     * 1:Obtain a session ID (upload URL) (POST)
     * 2:Upload the chunks (PATCH)
     * 3:Close the session (PUT)
     */
    private fun patchUpload(context: ArtifactUploadContext) {
        logger.info("Will using patch ways to upload file in repo ${context.artifactInfo.getRepoIdentify()}")
        if (context.artifactInfo !is OciBlobArtifactInfo) return
        with(context.artifactInfo as OciBlobArtifactInfo) {
            val range = context.request.getHeader("Content-Range")
            val length = context.request.contentLength
            val domain = ociOperationService.getReturnDomain(HttpContextHolder.getRequest())
            if (!range.isNullOrEmpty() && length > -1) {
                logger.info("range $range, length $length, uuid $uuid")
                val (start, end) = getRangeInfo(range)
                // 判断要上传的长度是否超长
                if (end - start > length - 1) {
                    OciResponseUtils.buildBlobUploadPatchResponse(
                        domain = domain,
                        uuid = uuid!!,
                        locationStr = OciLocationUtils.blobUUIDLocation(uuid, this),
                        response = HttpContextHolder.getResponse(),
                        range = length.toLong(),
                        status = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE
                    )
                    return
                }
            }
            val patchLen = storageService.append(
                appendId = uuid!!,
                artifactFile = context.getArtifactFile(),
                storageCredentials = context.repositoryDetail.storageCredentials
            )
            OciResponseUtils.buildBlobUploadPatchResponse(
                domain = domain,
                uuid = uuid,
                locationStr = OciLocationUtils.blobUUIDLocation(uuid, this),
                response = HttpContextHolder.getResponse(),
                range = patchLen
            )
        }
    }

    /**
     * blob 上传，直接使用post
     * Pushing a blob monolithically ：A single POST request
     */
    private fun postUpload(context: ArtifactUploadContext): Pair<OciDigest, String> {
        val artifactFile = context.getArtifactFile()
        val digest = OciDigest.fromSha256(artifactFile.getFileSha256())
        ociOperationService.storeArtifact(
            ociArtifactInfo = context.artifactInfo as OciArtifactInfo,
            artifactFile = artifactFile,
            storageCredentials = context.storageCredentials
        )
        logger.info(
            "Artifact ${context.artifactInfo.getArtifactFullPath()} has " +
                "been uploaded to ${context.artifactInfo.getArtifactFullPath()}" +
                " in repo  ${context.artifactInfo.getRepoIdentify()}"
        )
        val blobLocation = OciLocationUtils.blobLocation(digest, context.artifactInfo as OciArtifactInfo)
        return Pair(digest, blobLocation)
    }

    /**
     * put 上传包含三种逻辑：
     * 1 blob POST with PUT 上传的put模块处理
     * 2 blob POST PATCH with PUT 上传的put模块处理
     * 3 manifest PUT上传的逻辑处理
     */
    private fun putUpload(context: ArtifactUploadContext): Pair<OciDigest?, String?> {
        if (context.artifactInfo is OciBlobArtifactInfo) {
            return putUploadBlob(context)
        }
        if (context.artifactInfo is OciManifestArtifactInfo) {
            return putUploadManifest(context)
        }
        return Pair(null, null)
    }

    /**
     * blob PUT上传的逻辑处理
     * 1 blob POST with PUT 上传的put模块处理
     * 2 blob POST PATCH with PUT 上传的put模块处理
     */
    private fun putUploadBlob(context: ArtifactUploadContext): Pair<OciDigest, String> {
        val artifactInfo = context.artifactInfo as OciBlobArtifactInfo
        storageService.append(
            appendId = artifactInfo.uuid!!,
            artifactFile = context.getArtifactFile(),
            storageCredentials = context.repositoryDetail.storageCredentials
        )
        val fileInfo = storageService.finishAppend(artifactInfo.uuid, context.repositoryDetail.storageCredentials)
        val digest = OciDigest.fromSha256(fileInfo.sha256)
        ociOperationService.storeArtifact(
            ociArtifactInfo = context.artifactInfo as OciArtifactInfo,
            artifactFile = context.getArtifactFile(),
            storageCredentials = context.storageCredentials,
            fileInfo = fileInfo
        )
        logger.info(
            "Artifact ${context.artifactInfo.getArtifactFullPath()} " +
                "has been uploaded to ${context.artifactInfo.getArtifactFullPath()}" +
                " in repo  ${context.artifactInfo.getRepoIdentify()}"
        )
        val blobLocation = OciLocationUtils.blobLocation(digest, artifactInfo)
        return Pair(digest, blobLocation)
    }

    /**
     * manifest文件 PUT上传的逻辑处理
     */
    private fun putUploadManifest(context: ArtifactUploadContext): Pair<OciDigest, String> {
        val artifactInfo = context.artifactInfo as OciManifestArtifactInfo
        val artifactFile = context.getArtifactFile()
        val digest = OciDigest.fromSha256(artifactFile.getFileSha256())
        val node = ociOperationService.storeArtifact(
            ociArtifactInfo = artifactInfo,
            artifactFile = artifactFile,
            storageCredentials = context.storageCredentials
        )
        logger.info(
            "Artifact ${context.artifactInfo.getArtifactFullPath()} has been uploaded to ${node!!.fullPath}" +
                " in repo  ${context.artifactInfo.getRepoIdentify()}"
        )
        // 上传manifest文件，同时需要将manifest中对应blob的属性进行补充到blob节点中，同时创建package相关信息
        ociOperationService.updateOciInfo(
            ociArtifactInfo = artifactInfo,
            digest = digest,
            artifactFile = artifactFile,
            storageCredentials = context.storageCredentials,
            fullPath = node.fullPath
        )
        val manifestLocation = OciLocationUtils.manifestLocation(digest, artifactInfo)
        return Pair(digest, manifestLocation)
    }

    /**
     * 在原有逻辑上增加响应头
     */
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        logger.info(
            "Will start to download oci artifact ${context.artifactInfo.getArtifactFullPath()}" +
                " in repo ${context.artifactInfo.getRepoIdentify()}..."
        )
        val artifactInfo = context.artifactInfo as OciArtifactInfo
        val fullPath = ociOperationService.getNodeFullPath(artifactInfo)
        return downloadArtifact(context, fullPath)
    }

    /**
     * 针对oci协议 需要将对应的media type返回
     */
    private fun downloadArtifact(context: ArtifactDownloadContext, fullPath: String?): ArtifactResource? {
        if (fullPath == null) return null
        val node = getNodeDetail(context.artifactInfo as OciArtifactInfo, fullPath)
        logger.info(
            "Starting to download $fullPath " +
                "in repo: ${context.artifactInfo.getRepoIdentify()}"
        )
        val inputStream = storageManager.loadArtifactInputStream(node, context.storageCredentials)
            ?: return null
        val digest = OciDigest.fromSha256(node!!.sha256.orEmpty())
        val mediaType = node.metadata[MEDIA_TYPE] ?: run {
            node.metadata[OLD_DOCKER_MEDIA_TYPE] ?: MediaTypes.APPLICATION_OCTET_STREAM
        }
        val contentType = if (context.artifactInfo is OciManifestArtifactInfo) {
            node.metadata[MEDIA_TYPE] ?: run {
                node.metadata[OLD_DOCKER_MEDIA_TYPE] ?: OCI_IMAGE_MANIFEST_MEDIA_TYPE
            }
        } else {
            MediaTypes.APPLICATION_OCTET_STREAM
        }

        logger.info(
            "The mediaType of Artifact $fullPath is $mediaType and it's contentType is $contentType" +
                "in repo: ${context.artifactInfo.getRepoIdentify()}"
        )
        OciResponseUtils.buildDownloadResponse(
            digest = digest,
            response = context.response,
            size = node.size,
            contentType = contentType as String
        )
        val resource = ArtifactResource(
            inputStream = inputStream,
            artifactName = context.artifactInfo.getResponseName(),
            node = node,
            channel = ArtifactChannel.LOCAL,
            useDisposition = context.useDisposition
        )
        resource.contentType = mediaType.toString()
        return resource
    }

    private fun getNodeDetail(artifactInfo: OciArtifactInfo, fullPath: String): NodeDetail? {
        return nodeClient.getNodeDetail(artifactInfo.projectId, artifactInfo.repoName, fullPath).data ?: run {
            val oldDockerPath = ociOperationService.getDockerNode(artifactInfo)
                ?: return null
            nodeClient.getNodeDetail(artifactInfo.projectId, artifactInfo.repoName, oldDockerPath).data
        }
    }

/**
     * 版本不存在时 status code 404
     */
    override fun remove(context: ArtifactRemoveContext) {
        with(context.artifactInfo) {
            val fullPath = ociOperationService.getNodeFullPath(this as OciArtifactInfo)
                ?: throw OciFileNotFoundException(
                    "node [${getArtifactFullPath()}] in repo ${this.getRepoIdentify()} does not found."
                )
            nodeClient.getNodeDetail(projectId, repoName, fullPath).data
                ?: throw OciFileNotFoundException(
                    "node [$fullPath] in repo ${this.getRepoIdentify()} does not found."
                )
            logger.info("Ready to delete $fullPath in repo ${getRepoIdentify()}")
            val request = NodeDeleteRequest(projectId, repoName, fullPath, context.userId)
            nodeClient.deleteNode(request)
            OciResponseUtils.buildDeleteResponse(context.response)
        }
    }

    /**
     * 查询tag列表
     */
    override fun query(context: ArtifactQueryContext): Any? {
        if (context.artifactInfo is OciTagArtifactInfo) {
            return queryTagList(context)
        }
        if (context.artifactInfo is OciManifestArtifactInfo) {
            return queryManifest(context)
        }
        return null
    }

    private fun queryManifest(context: ArtifactQueryContext): ArtifactInputStream? {
        val node = getNodeDetail(context.artifactInfo as OciArtifactInfo, context.artifactInfo.getArtifactFullPath())
        return storageManager.loadArtifactInputStream(node, context.storageCredentials)
    }

    private fun queryTagList(context: ArtifactQueryContext): TagsInfo {
        with(context.artifactInfo as OciTagArtifactInfo) {
            val n = context.getAttribute<Int>(N)
            val last = context.getAttribute<String>(LAST_TAG)
            val packageKey = PackageKeys.ofName(context.repositoryDetail.type.name.toLowerCase(), packageName)
            val versionList = packageClient.listAllVersion(
                projectId,
                repoName,
                packageKey
            ).data.orEmpty()
            var tagList = mutableListOf<String>().apply {
                versionList.forEach {
                    this.add(it.name)
                }
                this.sort()
            }
            tagList = filterHandler(
                tags = tagList,
                n = n,
                last = last
            )
            return TagsInfo(packageName, tagList as List<String>)
        }
    }

    /**
     * 根据n或者last进行过滤（注意n是否会超过tags总长）
     * 1 n和last 都不存在，则返回所有
     * 2 n存在， last不存在，则返回前n个
     * 3 last存在 n不存在， 则返回查到的last，如不存在，则返回空列表
     * 4 last存在，n存在，则返回last之后的n个
     */
    private fun filterHandler(tags: MutableList<String>, n: Int?, last: String?): MutableList<String> {
        if (n != null) return handleNFilter(tags, n, last)
        if (last.isNullOrEmpty()) return tags
        val index = tags.indexOf(last)
        return if (index == -1) {
            mutableListOf()
        } else {
            mutableListOf(last)
        }
    }

    /**
     * 处理n存在时的逻辑
     */
    private fun handleNFilter(tags: MutableList<String>, n: Int, last: String?): MutableList<String> {
        var tagList = tags
        var size = n
        val length = tags.size
        if (last.isNullOrEmpty()) {
            // 需要判断n个是否超过tags总长
            if (size > length) {
                size = length
            }
            tagList = tagList.subList(0, size)
            return tagList
        }
        // 当last存在，n也存在 则获取last所在后n个tag
        val index = tagList.indexOf(last)
        return if (index == -1) {
            mutableListOf()
        } else {
            // 需要判断last后n个是否超过tags总长
            if (index + size + 1 > length) {
                size = length - 1 - index
            }
            tagList = tagList.subList(index + 1, index + size + 1)
            tagList
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OciRegistryLocalRepository::class.java)
    }
}
