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
import com.tencent.bkrepo.common.artifact.path.PathUtils.combineFullPath
import com.tencent.bkrepo.common.artifact.path.PathUtils.resolveName
import com.tencent.bkrepo.common.artifact.path.PathUtils.resolveParent
import com.tencent.bkrepo.common.artifact.path.PathUtils.toPath
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.constant.DEFAULT_STORAGE_CREDENTIALS_KEY
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.dao.RepositoryDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.service.node.NodeMoveCopyOperation
import com.tencent.bkrepo.repository.service.repo.QuotaService
import com.tencent.bkrepo.repository.service.repo.StorageCredentialService
import com.tencent.bkrepo.repository.util.NodeEventFactory
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
    private val quotaService: QuotaService = nodeBaseService.quotaService

    override fun moveNode(moveRequest: NodeMoveCopyRequest) {
        moveCopy(moveRequest, true)
        publishEvent(NodeEventFactory.buildMovedEvent(moveRequest))
        logger.info("Move node success: [$moveRequest]")
    }

    override fun copyNode(copyRequest: NodeMoveCopyRequest) {
        moveCopy(copyRequest, false)
        publishEvent(NodeEventFactory.buildCopiedEvent(copyRequest))
        logger.info("Copy node success: [$copyRequest]")
    }

    /**
     * 处理节点操作请求
     */
    private fun moveCopy(request: NodeMoveCopyRequest, move: Boolean) {
        with(resolveContext(request, move)) {
            preCheck(this)
            if (canIgnore(this)) {
                return
            }
            if (srcNode.folder) {
                moveCopyFolder(this)
            } else {
                moveCopyFile(this)
            }
        }
    }

    /**
     * 移动/复制节点
     */
    private fun doMoveCopy(
        context: MoveCopyContext,
        node: TNode,
        dstPath: String,
        dstName: String
    ) {
        with(context) {
            val dstFullPath = combineFullPath(dstPath, dstName)
            // 冲突检查
            val existNode = nodeDao.findNode(dstProjectId, dstRepoName, dstFullPath)
            // 目录 -> 目录: 跳过
            if (node.folder && existNode?.folder == true) return
            checkConflict(context, node, existNode)
            // copy目标节点
            val dstNode = buildDstNode(this, node, dstPath, dstName, dstFullPath)
            // 仓库配额检查
            checkQuota(context, node, existNode)

            // 文件 & 跨存储node
            if (!node.folder && srcCredentials != dstCredentials) {
                if (storageService.exist(node.sha256!!, srcCredentials)) {
                    storageService.copy(node.sha256!!, srcCredentials, dstCredentials)
                } else {
                    // 默认存储为null,所以需要使用一个默认key，以区分该节点是拷贝节点
                    dstNode.copyFromCredentialsKey = srcCredentials?.key ?: DEFAULT_STORAGE_CREDENTIALS_KEY
                    dstNode.copyIntoCredentialsKey = dstCredentials?.key ?: DEFAULT_STORAGE_CREDENTIALS_KEY
                }
            }
            // 创建dst节点
            nodeBaseService.doCreate(dstNode, dstRepo)
            // move操作，创建dst节点后，还需要删除src节点
            // 因为分表所以不能直接更新src节点，必须创建新的并删除旧的
            if (move) {
                val query = NodeQueryHelper.nodeQuery(node.projectId, node.repoName, node.fullPath)
                val update = NodeQueryHelper.nodeDeleteUpdate(operator)
                if (!node.folder) {
                    quotaService.decreaseUsedVolume(node.projectId, node.repoName, node.size)
                }
                nodeDao.updateFirst(query, update)
            }
        }
    }

    private fun checkQuota(context: MoveCopyContext, node: TNode, existNode: TNode?) {
        // 目录不占仓库容量，不需要检查
        if (node.folder) return

        with(context) {
            // 文件 -> 文件，目标文件不存在
            if (existNode == null) {
                // 同仓库的移动操作不需要检查仓库已使用容量
                if (!(isSameRepo() && move)) {
                    quotaService.checkRepoQuota(dstProjectId, dstRepoName, node.size)
                }
            }

            // 文件 -> 文件 & 允许覆盖: 删除old
            if (existNode?.folder == false && overwrite) {
                quotaService.checkRepoQuota(existNode.projectId, existNode.repoName, node.size - existNode.size)
                nodeBaseService.deleteByPath(existNode.projectId, existNode.repoName, existNode.fullPath, operator)
            }
        }
    }

    private fun buildDstNode(
        context: MoveCopyContext,
        node: TNode,
        dstPath: String,
        dstName: String,
        dstFullPath: String
    ): TNode {
        with(context) {
            val dstNode = node.copy(
                id = null,
                projectId = dstProjectId,
                repoName = dstRepoName,
                path = dstPath,
                name = dstName,
                fullPath = dstFullPath,
                lastModifiedBy = operator,
                lastModifiedDate = LocalDateTime.now()
            )
            // move操作，create信息保留
            if (move) {
                dstNode.createdBy = operator
                dstNode.createdDate = LocalDateTime.now()
            }

            return dstNode
        }
    }

    private fun resolveContext(request: NodeMoveCopyRequest, move: Boolean): MoveCopyContext {
        with(request) {
            val srcFullPath = PathUtils.normalizeFullPath(srcFullPath)
            val dstProjectId = request.destProjectId ?: srcProjectId
            val dstRepoName = request.destRepoName ?: srcRepoName
            val dstFullPath = PathUtils.normalizeFullPath(request.destFullPath)
            val isSameRepo = srcProjectId == destProjectId && srcRepoName == destRepoName
            // 查询repository
            val srcRepo = findRepository(srcProjectId, srcRepoName)
            val dstRepo = if (!isSameRepo) findRepository(dstProjectId, dstRepoName) else srcRepo
            // 查询storageCredentials
            val srcCredentials = findCredential(srcRepo.credentialsKey)
            val dstCredentials = if (!isSameRepo) findCredential(dstRepo.credentialsKey) else srcCredentials
            val srcNode = nodeDao.findNode(srcProjectId, srcRepoName, srcFullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, srcFullPath)
            val dstNode = nodeDao.findNode(dstProjectId, dstRepoName, dstFullPath)

            return MoveCopyContext(
                srcRepo = srcRepo,
                srcCredentials = srcCredentials,
                srcNode = srcNode,
                dstProjectId = dstProjectId,
                dstRepoName = dstRepoName,
                dstFullPath = dstFullPath,
                dstRepo = dstRepo,
                dstCredentials = dstCredentials,
                dstNode = dstNode,
                overwrite = overwrite,
                operator = request.operator,
                move = move
            )
        }
    }

    /**
     * 预检查
     */
    private fun preCheck(context: MoveCopyContext) {
        // 只允许local或者composite类型仓库操作
        val canSrcRepoMove = context.srcRepo.category.let {
            it == RepositoryCategory.LOCAL || it == RepositoryCategory.COMPOSITE
        }
        val canDstRepoMove = context.dstRepo.category.let {
            it == RepositoryCategory.LOCAL || it == RepositoryCategory.COMPOSITE
        }
        if (!canSrcRepoMove || !canDstRepoMove) {
            throw ErrorCodeException(CommonMessageCode.METHOD_NOT_ALLOWED, "Only local repository is supported")
        }
    }

    /**
     * 判断能否忽略执行
     */
    private fun canIgnore(context: MoveCopyContext): Boolean {
        with(context) {
            var canIgnore = false
            if (isSameRepo()) {
                if (srcNode.fullPath == dstNode?.fullPath) {
                    // 同路径，跳过
                    canIgnore = true
                } else if (dstNode?.folder == true && srcNode.path == toPath(dstNode.fullPath)) {
                    // src为dst目录下的子节点，跳过
                    canIgnore = true
                }
            }
            return canIgnore
        }
    }

    private fun checkConflict(context: MoveCopyContext, node: TNode, existNode: TNode?) {
        // 目录 -> 文件: 出错
        if (node.folder && existNode?.folder == false) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }
        // 文件 -> 文件 & 不允许覆盖: 出错
        if (!node.folder && existNode?.folder == false && !context.overwrite) {
            throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, existNode.fullPath)
        }
    }

    /**
     * 移动/复制目录
     */
    private fun moveCopyFolder(context: MoveCopyContext) {
        with(context) {
            // 目录 -> 文件: error
            if (dstNode?.folder == false) {
                throw ErrorCodeException(ArtifactMessageCode.NODE_CONFLICT, dstFullPath)
            }
            val dstRootNodePath = if (dstNode == null) {
                // 目录 -> 不存在的目录
                val path = resolveParent(dstFullPath)
                val name = resolveName(dstFullPath)
                // 创建dst父目录
                nodeBaseService.mkdirs(dstProjectId, dstRepoName, path, operator)
                // 操作节点
                doMoveCopy(this, srcNode, path, name)
                PathUtils.combinePath(path, name)
            } else {
                // 目录 -> 存在的目录
                val path = toPath(dstNode.fullPath)
                val name = srcNode.name
                // 操作节点
                doMoveCopy(this, srcNode, path, name)
                PathUtils.combinePath(path, name)
            }
            val srcRootNodePath = toPath(srcNode.fullPath)
            val listOption = NodeListOption(includeFolder = true, includeMetadata = true, deep = true, sort = false)
            val query = NodeQueryHelper.nodeListQuery(srcNode.projectId, srcNode.repoName, srcRootNodePath, listOption)
            // 目录下的节点 -> 创建好的目录
            nodeDao.find(query).forEach {
                doMoveCopy(this, it, it.path.replaceFirst(srcRootNodePath, dstRootNodePath), it.name)
            }
        }
    }

    /**
     * 移动/复制文件
     */
    private fun moveCopyFile(context: MoveCopyContext) {
        with(context) {
            val dstPath = if (dstNode?.folder == true) toPath(dstNode.fullPath) else resolveParent(dstFullPath)
            val dstName = if (dstNode?.folder == true) srcNode.name else resolveName(dstFullPath)
            // 创建dst父目录
            nodeBaseService.mkdirs(dstProjectId, dstRepoName, dstPath, operator)
            doMoveCopy(context, srcNode, dstPath, dstName)
        }
    }

    private fun findRepository(projectId: String, repoName: String): TRepository {
        return repositoryDao.findByNameAndType(projectId, repoName)
            ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
    }

    private fun findCredential(key: String?): StorageCredentials? {
        return key?.let { storageCredentialService.findByKey(it) }
    }

    data class MoveCopyContext(
        val srcRepo: TRepository,
        val srcCredentials: StorageCredentials?,
        val srcNode: TNode,
        val dstProjectId: String,
        val dstRepoName: String,
        val dstFullPath: String,
        val dstRepo: TRepository,
        val dstCredentials: StorageCredentials?,
        val dstNode: TNode?,
        val overwrite: Boolean,
        val operator: String,
        val move: Boolean
    ) {
        fun isSameRepo() = srcNode.projectId == dstProjectId && srcNode.repoName == dstRepoName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeMoveCopySupport::class.java)
    }
}
