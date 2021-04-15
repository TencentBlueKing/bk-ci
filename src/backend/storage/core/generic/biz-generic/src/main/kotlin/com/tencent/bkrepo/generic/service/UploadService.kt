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

package com.tencent.bkrepo.generic.service

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.REPO_KEY
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HeaderUtils.getBooleanHeader
import com.tencent.bkrepo.common.service.util.HeaderUtils.getLongHeader
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.generic.artifact.GenericArtifactInfo
import com.tencent.bkrepo.generic.constant.GenericMessageCode
import com.tencent.bkrepo.generic.constant.HEADER_EXPIRES
import com.tencent.bkrepo.generic.constant.HEADER_OVERWRITE
import com.tencent.bkrepo.generic.pojo.BlockInfo
import com.tencent.bkrepo.generic.pojo.UploadTransactionInfo
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 通用文件上传服务类
 */
@Service
class UploadService(
    private val nodeClient: NodeClient,
    private val storageService: StorageService
) : ArtifactService() {

    fun upload(artifactInfo: GenericArtifactInfo, file: ArtifactFile) {
        val context = ArtifactUploadContext(file)
        repository.upload(context)
    }

    fun delete(userId: String, artifactInfo: GenericArtifactInfo) {
        val context = ArtifactRemoveContext()
        repository.remove(context)
        logger.info("User[${SecurityUtils.getPrincipal()}] delete artifact[$artifactInfo] success.")
    }

    fun startBlockUpload(userId: String, artifactInfo: GenericArtifactInfo): UploadTransactionInfo {
        with(artifactInfo) {
            val expires = getLongHeader(HEADER_EXPIRES)
            val overwrite = getBooleanHeader(HEADER_OVERWRITE)
            Preconditions.checkArgument(expires >= 0, "expires")
            // 判断文件是否存在
            if (!overwrite && nodeClient.checkExist(projectId, repoName, getArtifactFullPath()).data == true) {
                logger.warn(
                    "User[${SecurityUtils.getPrincipal()}] start block upload [$artifactInfo] failed: " +
                        "artifact already exists."
                )
                throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, getArtifactName())
            }

            val uploadId = storageService.createBlockId(getStorageCredentials())
            val uploadTransaction = UploadTransactionInfo(
                uploadId = uploadId,
                expireSeconds = TRANSACTION_EXPIRES
            )

            logger.info("User[${SecurityUtils.getPrincipal()}] start block upload [$artifactInfo] success: $uploadId.")
            return uploadTransaction
        }
    }

    fun abortBlockUpload(userId: String, uploadId: String, artifactInfo: GenericArtifactInfo) {
        val storageCredentials = getStorageCredentials()
        checkUploadId(uploadId, storageCredentials)

        storageService.deleteBlockId(uploadId, storageCredentials)
        logger.info("User[${SecurityUtils.getPrincipal()}] abort upload block [$artifactInfo] success.")
    }

    fun completeBlockUpload(userId: String, uploadId: String, artifactInfo: GenericArtifactInfo) {
        val storageCredentials = getStorageCredentials()
        checkUploadId(uploadId, storageCredentials)

        val mergedFileInfo = storageService.mergeBlock(uploadId, storageCredentials)
        // 保存节点
        nodeClient.createNode(
            NodeCreateRequest(
                projectId = artifactInfo.projectId,
                repoName = artifactInfo.repoName,
                folder = false,
                fullPath = artifactInfo.getArtifactFullPath(),
                sha256 = mergedFileInfo.sha256,
                md5 = mergedFileInfo.md5,
                size = mergedFileInfo.size,
                overwrite = true,
                operator = userId
            )
        )
        logger.info("User[${SecurityUtils.getPrincipal()}] complete upload [$artifactInfo] success.")
    }

    fun listBlock(userId: String, uploadId: String, artifactInfo: GenericArtifactInfo): List<BlockInfo> {
        val storageCredentials = getStorageCredentials()
        checkUploadId(uploadId, storageCredentials)

        val blockInfoList = storageService.listBlock(uploadId, storageCredentials)
        return blockInfoList.mapIndexed { index, it ->
            BlockInfo(size = it.first, sequence = index + 1, sha256 = it.second)
        }
    }

    private fun checkUploadId(uploadId: String, storageCredentials: StorageCredentials?) {
        if (!storageService.checkBlockId(uploadId, storageCredentials)) {
            throw ErrorCodeException(GenericMessageCode.UPLOAD_ID_NOT_FOUND, uploadId)
        }
    }

    private fun getStorageCredentials(): StorageCredentials? {
        val repoDetail = HttpContextHolder.getRequest().getAttribute(REPO_KEY)
        require(repoDetail is RepositoryDetail)
        return repoDetail.storageCredentials
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UploadService::class.java)
        private const val TRANSACTION_EXPIRES: Long = 3600 * 12L
    }
}
