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
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.service.util.SpringContextUtils.Companion.publishEvent
import com.tencent.bkrepo.repository.dao.NodeDao
import com.tencent.bkrepo.repository.model.TNode
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.service.node.NodeRenameOperation
import com.tencent.bkrepo.repository.util.NodeEventFactory.buildRenamedEvent
import com.tencent.bkrepo.repository.util.NodeQueryHelper
import org.slf4j.LoggerFactory

/**
 * 节点重命名接口实现
 */
open class NodeRenameSupport(
    private val nodeBaseService: NodeBaseService
) : NodeRenameOperation {

    private val nodeDao: NodeDao = nodeBaseService.nodeDao

    override fun renameNode(renameRequest: NodeRenameRequest) {
        with(renameRequest) {
            val fullPath = PathUtils.normalizeFullPath(fullPath)
            val newFullPath = PathUtils.normalizeFullPath(newFullPath)
            val node = nodeDao.findNode(projectId, repoName, fullPath)
                ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, fullPath)
            doRename(node, newFullPath, operator)
            publishEvent(buildRenamedEvent(renameRequest))
            logger.info("Rename node [$this] success.")
        }
    }

    /**
     * 将节点重命名为指定名称
     */
    private fun doRename(node: TNode, newFullPath: String, operator: String) {
        val projectId = node.projectId
        val repoName = node.repoName
        val newPath = PathUtils.resolveParent(newFullPath)
        val newName = PathUtils.resolveName(newFullPath)

        // 检查新路径是否被占用
        if (nodeDao.exists(projectId, repoName, newFullPath)) {
            logger.warn("Rename node [${node.fullPath}] failed: $newFullPath is exist.")
            throw ErrorCodeException(ArtifactMessageCode.NODE_EXISTED, newFullPath)
        }

        // 如果为文件夹，查询子节点并修改
        if (node.folder) {
            nodeBaseService.mkdirs(projectId, repoName, newFullPath, operator)
            val newParentPath = PathUtils.toPath(newFullPath)
            val listOption = NodeListOption(
                includeFolder = true,
                includeMetadata = false,
                deep = false,
                sort = false
            )
            val query = NodeQueryHelper.nodeListQuery(projectId, repoName, node.fullPath, listOption)
            nodeDao.find(query).forEach { doRename(it, newParentPath + it.name, operator) }
            // 删除自己
            nodeDao.remove(NodeQueryHelper.nodeQuery(projectId, repoName, node.fullPath))
        } else {
            // 修改自己
            val selfQuery = NodeQueryHelper.nodeQuery(projectId, repoName, node.fullPath)
            val selfUpdate = NodeQueryHelper.nodePathUpdate(newPath, newName, operator)
            nodeDao.updateFirst(selfQuery, selfUpdate)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NodeRenameSupport::class.java)
    }
}
