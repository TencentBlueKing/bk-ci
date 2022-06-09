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

package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.service.node.NodeSearchService
import com.tencent.bkrepo.repository.service.node.NodeService
import org.springframework.web.bind.annotation.RestController

/**
 * 节点服务接口实现类
 */
@RestController
class NodeController(
    private val nodeService: NodeService,
    private val nodeSearchService: NodeSearchService
) : NodeClient {

    override fun getNodeDetail(projectId: String, repoName: String, fullPath: String): Response<NodeDetail?> {
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, fullPath)
        return ResponseBuilder.success(nodeService.getNodeDetail(artifactInfo))
    }

    override fun checkExist(projectId: String, repoName: String, fullPath: String): Response<Boolean> {
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, fullPath)
        return ResponseBuilder.success(nodeService.checkExist(artifactInfo))
    }

    override fun listExistFullPath(
        projectId: String,
        repoName: String,
        fullPathList: List<String>
    ): Response<List<String>> {
        return ResponseBuilder.success(nodeService.listExistFullPath(projectId, repoName, fullPathList))
    }

    override fun listNodePage(
        projectId: String,
        repoName: String,
        path: String,
        option: NodeListOption
    ): Response<Page<NodeInfo>> {
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, path)
        return ResponseBuilder.success(nodeService.listNodePage(artifactInfo, option))
    }

    override fun createNode(nodeCreateRequest: NodeCreateRequest): Response<NodeDetail> {
        return ResponseBuilder.success(nodeService.createNode(nodeCreateRequest))
    }

    override fun updateNode(nodeUpdateRequest: NodeUpdateRequest): Response<Void> {
        nodeService.updateNode(nodeUpdateRequest)
        return ResponseBuilder.success()
    }

    override fun renameNode(nodeRenameRequest: NodeRenameRequest): Response<Void> {
        nodeService.renameNode(nodeRenameRequest)
        return ResponseBuilder.success()
    }

    override fun moveNode(nodeMoveRequest: NodeMoveCopyRequest): Response<Void> {
        nodeService.moveNode(nodeMoveRequest)
        return ResponseBuilder.success()
    }

    override fun copyNode(nodeCopyRequest: NodeMoveCopyRequest): Response<Void> {
        nodeService.copyNode(nodeCopyRequest)
        return ResponseBuilder.success()
    }

    override fun deleteNode(nodeDeleteRequest: NodeDeleteRequest): Response<Void> {
        nodeService.deleteNode(nodeDeleteRequest)
        return ResponseBuilder.success()
    }

    override fun computeSize(projectId: String, repoName: String, fullPath: String): Response<NodeSizeInfo> {
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, fullPath)
        return ResponseBuilder.success(nodeService.computeSize(artifactInfo))
    }

    override fun countFileNode(projectId: String, repoName: String, path: String): Response<Long> {
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, path)
        return ResponseBuilder.success(nodeService.countFileNode(artifactInfo))
    }

    override fun search(queryModel: QueryModel): Response<Page<Map<String, Any?>>> {
        return ResponseBuilder.success(nodeSearchService.search(queryModel))
    }

    override fun listNode(
        projectId: String,
        repoName: String,
        path: String,
        includeFolder: Boolean,
        deep: Boolean
    ): Response<List<NodeInfo>> {
        val artifactInfo = DefaultArtifactInfo(projectId, repoName, path)
        val nodeListOption = NodeListOption(
            includeFolder = includeFolder,
            includeMetadata = false,
            deep = deep
        )
        return ResponseBuilder.success(nodeService.listNode(artifactInfo, nodeListOption))
    }
}
