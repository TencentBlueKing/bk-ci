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

package com.tencent.bkrepo.repository.service.node.impl

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.listener.event.node.NodeCopiedEvent
import com.tencent.bkrepo.repository.listener.event.node.NodeMovedEvent
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.node.CrossRepoNodeRequest
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveRequest
import com.tencent.bkrepo.repository.service.StorageCredentialService
import com.tencent.bkrepo.repository.service.node.NodeMoveCopyOperation
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 节点移动/拷贝接口实现
 */
open class NodeMoveCopySupport(
    private val nodeBaseService: NodeBaseService
) : NodeMoveCopyOperation {

    private val storageService: StorageService = nodeBaseService.storageService
    private val nodeDao: NodeDao = nodeBaseService.nodeDao
    private val repositoryDao: RepositoryDao = nodeBaseService.repositoryDao
    private val storageCredentialService: StorageCredentialService = nodeBaseService.storageCredentialService

    override fun moveNode(moveRequest: NodeMoveRequest) {
        moveOrCopy(moveRequest, moveRequest.operator)
    }

    override fun copyNode(copyRequest: NodeCopyRequest) {
        moveOrCopy(copyRequest, copyRequest.operator)
    }

    /**
     * 处理节点操作请求
     */
    private fun moveOrCopy(request: CrossRepoNodeRequest, operator: String) {
        with(request) {
            // 准备数据
            val srcFullPath = PathUtils.normalizeFullPath(srcFullPath)
            val destProjectId = request.destProjectId ?: srcProjectId
            val destRepoName = request.destRepoName ?: srcRepoName
            val destFullPath = PathUtils.normalizeFullPath(request.destFullPath)

            val isSameRepository = srcProjectId == destProjectId && srcRepoName == destRepoName
            // 查询repository
            val srcRepo = repositoryDao.findByNameAndType(srcProjectId, srcRepoName)
                ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, srcRepoName)
            val destRepo = if (!isSameRepository) {
                repositoryDao.findByNameAndType(destProjectId, destRepoName)
                    ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, destRepoName)
            } else srcRepo

            // 查询storageCredentials
            val srcCredentials = srcRepo.credentialsKey?.let {
                storageCredentialService.findByKey(it)
            }
            val destCredentials = if (!isSameRepository) {
                destRepo.credentialsKey?.let { storageCredentialService.findByKey(it) }
            } else srcCredentials

            // 只允许local或者composite类型仓库操作
            val canSrcRepoMove = srcRepo.category.let {
                it == RepositoryCategory.LOCAL || it == RepositoryCategory.COMPOSITE
            }
            val canDestRepoMove = destRepo.category.let {
                it == RepositoryCategory.LOCAL || it == RepositoryCategory.COMPOSITE
            }
            if (!canSrcRepoMove || !canDestRepoMove) {
                throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "Only local repository is supported")
            }
            val srcNode = nodeDao.findNode(srcProjectId, srcRepoName, srcFullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, srcFullPath)
            val destNode = nodeDao.findNode(destProjectId, destRepoName, destFullPath)
            // 同路径，跳过
            if (isSameRepository && srcNode.fullPath == destNode?.fullPath) return
            // src为dest目录下的子节点，跳过
            if (isSameRepository && destNode?.folder == true && srcNode.path == PathUtils.toPath(destNode.fullPath)) {
                return
            }
            // 目录 ->
            if (srcNode.folder) {
                // 目录 -> 文件: error
                if (destNode?.folder == false) {
                    throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, destFullPath)
                }
                val destRootNodePath = if (destNode == null) {
                    // 目录 -> 不存在的目录
                    val path = PathUtils.resolveParent(destFullPath)
                    val name = PathUtils.resolveName(destFullPath)
                    // 创建dest父目录
                    nodeBaseService.mkdirs(destProjectId, destRepoName, path, operator)
                    // 操作节点
                    doMoveOrCopy(srcNode, destRepo, srcCredentials, destCredentials, path, name, request, operator)
                    PathUtils.combinePath(path, name)
                } else {
                    // 目录 -> 存在的目录
                    val path = PathUtils.toPath(destNode.fullPath)
                    val nodeName = srcNode.name
                    // 操作节点
                    doMoveOrCopy(srcNode, destRepo, srcCredentials, destCredentials, path, nodeName, request, operator)
                    PathUtils.combinePath(path, srcNode.name)
                }
                val srcRootNodePath = PathUtils.toPath(srcNode.fullPath)
                val listOption = NodeListOption(
                    includeFolder = true,
                    includeMetadata = false,
                    deep = true,
                    sort = false
                )
                val query =
                    NodeQueryHelper.nodeListQuery(srcNode.projectId, srcNode.repoName, srcRootNodePath, listOption)
                // 目录下的节点 -> 创建好的目录
                nodeDao.find(query).forEach {
                    val destPath = it.path.replaceFirst(srcRootNodePath, destRootNodePath)
                    doMoveOrCopy(it, destRepo, srcCredentials, destCredentials, destPath, null, request, operator)
                }
            } else {
                // 文件 ->
                val destPath =
                    if (destNode?.folder == true) PathUtils.toPath(destNode.fullPath) else PathUtils.resolveParent(
                        destFullPath
                    )
                val destName = if (destNode?.folder == true) srcNode.name else PathUtils.resolveName(destFullPath)
                // 创建dest父目录
                nodeBaseService.mkdirs(destProjectId, destRepoName, destPath, operator)
                doMoveOrCopy(srcNode, destRepo, srcCredentials, destCredentials, destPath, destName, request, operator)
            }
            // event
            if (request is NodeMoveRequest) {
                nodeBaseService.publishEvent(NodeMovedEvent(request))
            } else if (request is NodeCopyRequest) {
                nodeBaseService.publishEvent(NodeCopiedEvent(request))
            }
            logger.info("[${request.getOperateName()}] node success: [$this]")
        }
    }

    /**
     * 移动/拷贝节点
     */
    private fun doMoveOrCopy(
        srcNode: TNode,
        destRepository: TRepository,
        srcStorageCredentials: StorageCredentials?,
        destStorageCredentials: StorageCredentials?,
        destPath: String,
        nodeName: String?,
        request: CrossRepoNodeRequest,
        operator: String
    ) {
        // 计算destName
        val destName = nodeName ?: srcNode.name
        val destFullPath = PathUtils.combineFullPath(destPath, destName)
        // 冲突检查
        val existNode = nodeDao.findNode(destRepository.projectId, destRepository.name, destFullPath)
        // 目录 -> 目录: 跳过
        if (srcNode.folder && existNode?.folder == true) return
        // 目录 -> 文件: 出错
        if (srcNode.folder && existNode?.folder == false) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }
        // 文件 -> 文件 & 不允许覆盖: 出错
        if (!srcNode.folder && existNode?.folder == false && !request.overwrite) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }

        // copy目标节点
        val destNode = srcNode.copy(
            id = null,
            projectId = destRepository.projectId,
            repoName = destRepository.name,
            path = destPath,
            name = destName,
            fullPath = destFullPath,
            lastModifiedBy = operator,
            lastModifiedDate = LocalDateTime.now()
        )
        // move操作，create信息保留
        if (request is NodeMoveRequest) {
            destNode.createdBy = operator
            destNode.createdDate = LocalDateTime.now()
        }
        // 文件 -> 文件 & 允许覆盖: 删除old
        if (!srcNode.folder && existNode?.folder == false && request.overwrite) {
            val query = NodeQueryHelper.nodeQuery(existNode.projectId, existNode.repoName, existNode.fullPath)
            val update = NodeQueryHelper.nodeDeleteUpdate(operator)
            nodeDao.updateFirst(query, update)
        }
        // 文件 & 跨存储
        if (!srcNode.folder && srcStorageCredentials != destStorageCredentials) {
            storageService.copy(srcNode.sha256!!, srcStorageCredentials, destStorageCredentials)
        }
        // 创建dest节点
        nodeBaseService.doCreate(destNode, destRepository)
        // move操作，创建dest节点后，还需要删除src节点
        // 因为分表所以不能直接更新src节点，必须创建新的并删除旧的
        if (request is NodeMoveRequest) {
            val query = NodeQueryHelper.nodeQuery(srcNode.projectId, srcNode.repoName, srcNode.fullPath)
            val update = NodeQueryHelper.nodeDeleteUpdate(operator)
            nodeDao.updateFirst(query, update)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeMoveCopySupport::class.java)
    }
}
