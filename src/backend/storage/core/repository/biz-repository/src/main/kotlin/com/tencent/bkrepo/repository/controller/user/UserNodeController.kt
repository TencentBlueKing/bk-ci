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

package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo.Companion.DEFAULT_MAPPING_URI
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.query.model.QueryModel
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.node.NodeDeletedPoint
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeInfo
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeRestoreOption
import com.tencent.bkrepo.repository.pojo.node.NodeRestoreResult
import com.tencent.bkrepo.repository.pojo.node.NodeSizeInfo
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeUpdateRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeMoveCopyRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeRenameRequest
import com.tencent.bkrepo.repository.pojo.node.user.UserNodeUpdateRequest
import com.tencent.bkrepo.repository.service.node.NodeSearchService
import com.tencent.bkrepo.repository.service.node.NodeService
import com.tencent.bkrepo.repository.util.PipelineRepoUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Api("节点用户接口")
@RestController
@RequestMapping("/api/node")
class UserNodeController(
    private val nodeService: NodeService,
    private val nodeSearchService: NodeSearchService,
    private val permissionManager: PermissionManager
) {

    @ApiOperation("根据路径查看节点详情")
    @Permission(type = ResourceType.NODE, action = PermissionAction.READ)
    @GetMapping(DEFAULT_MAPPING_URI/* Deprecated */, "/detail/$DEFAULT_MAPPING_URI")
    fun getNodeDetail(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo
    ): Response<NodeDetail> {
        val node = nodeService.getNodeDetail(artifactInfo)
            ?: throw ErrorCodeException(ArtifactMessageCode.NODE_NOT_FOUND, artifactInfo.getArtifactFullPath())
        return ResponseBuilder.success(node)
    }

    @ApiOperation("创建文件夹")
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    @PostMapping(DEFAULT_MAPPING_URI/* Deprecated */, "/mkdir/$DEFAULT_MAPPING_URI")
    fun mkdir(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo
    ): Response<Void> {
        with(artifactInfo) {
            val createRequest = NodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                folder = true,
                fullPath = getArtifactFullPath(),
                overwrite = false,
                operator = userId
            )
            nodeService.createNode(createRequest)
            return ResponseBuilder.success()
        }
    }

    @ApiOperation("删除节点")
    @Permission(type = ResourceType.NODE, action = PermissionAction.DELETE)
    @DeleteMapping(DEFAULT_MAPPING_URI/* Deprecated */, "/delete/$DEFAULT_MAPPING_URI")
    fun deleteNode(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo
    ): Response<Void> {
        with(artifactInfo) {
            val deleteRequest = NodeDeleteRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = getArtifactFullPath(),
                operator = userId
            )
            nodeService.deleteNode(deleteRequest)
            return ResponseBuilder.success()
        }
    }

    @ApiOperation("更新节点")
    @Permission(type = ResourceType.NODE, action = PermissionAction.UPDATE)
    @PostMapping("/update/$DEFAULT_MAPPING_URI")
    fun updateNode(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo,
        @RequestBody request: UserNodeUpdateRequest
    ): Response<Void> {
        with(artifactInfo) {
            val updateRequest = NodeUpdateRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = getArtifactFullPath(),
                expires = request.expires,
                operator = userId
            )
            nodeService.updateNode(updateRequest)
            return ResponseBuilder.success()
        }
    }

    @ApiOperation("重命名节点")
    @Permission(type = ResourceType.NODE, action = PermissionAction.UPDATE)
    @PostMapping("/rename/$DEFAULT_MAPPING_URI")
    fun renameNode(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: ArtifactInfo,
        @RequestParam newFullPath: String
    ): Response<Void> {
        with(artifactInfo) {
            permissionManager.checkNodePermission(PermissionAction.UPDATE, projectId, repoName, newFullPath)
            val renameRequest = NodeRenameRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = getArtifactFullPath(),
                newFullPath = newFullPath,
                operator = userId
            )
            nodeService.renameNode(renameRequest)
            return ResponseBuilder.success()
        }
    }

    @Deprecated("/rename/{projectId}/{repoName}/**")
    @ApiOperation("重命名节点")
    @PostMapping("/rename")
    fun renameNode(
        @RequestAttribute userId: String,
        @RequestBody request: UserNodeRenameRequest
    ): Response<Void> {
        with(request) {
            permissionManager.checkNodePermission(PermissionAction.UPDATE, projectId, repoName, fullPath)
            permissionManager.checkNodePermission(PermissionAction.UPDATE, projectId, repoName, newFullPath)
            val renameRequest = NodeRenameRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = fullPath,
                newFullPath = newFullPath,
                operator = userId
            )
            nodeService.renameNode(renameRequest)
            return ResponseBuilder.success()
        }
    }

    @ApiOperation("移动节点")
    @PostMapping("/move")
    fun moveNode(
        @RequestAttribute userId: String,
        @RequestBody request: UserNodeMoveCopyRequest
    ): Response<Void> {
        with(request) {
            checkCrossRepoPermission(request)
            val moveRequest = NodeMoveCopyRequest(
                srcProjectId = srcProjectId,
                srcRepoName = srcRepoName,
                srcFullPath = srcFullPath,
                destProjectId = destProjectId,
                destRepoName = destRepoName,
                destFullPath = destFullPath,
                overwrite = overwrite,
                operator = userId
            )
            nodeService.moveNode(moveRequest)
            return ResponseBuilder.success()
        }
    }

    @ApiOperation("复制节点")
    @PostMapping("/copy")
    fun copyNode(
        @RequestAttribute userId: String,
        @RequestBody request: UserNodeMoveCopyRequest
    ): Response<Void> {
        with(request) {
            checkCrossRepoPermission(request)
            val copyRequest = NodeMoveCopyRequest(
                srcProjectId = srcProjectId,
                srcRepoName = srcRepoName,
                srcFullPath = srcFullPath,
                destProjectId = destProjectId,
                destRepoName = destRepoName,
                destFullPath = destFullPath,
                overwrite = overwrite,
                operator = userId
            )
            nodeService.copyNode(copyRequest)
            return ResponseBuilder.success()
        }
    }

    @ApiOperation("查询节点大小信息")
    @Permission(type = ResourceType.NODE, action = PermissionAction.READ)
    @GetMapping("/size/$DEFAULT_MAPPING_URI")
    fun computeSize(artifactInfo: ArtifactInfo): Response<NodeSizeInfo> {
        val nodeSizeInfo = nodeService.computeSize(artifactInfo)
        return ResponseBuilder.success(nodeSizeInfo)
    }

    @ApiOperation("分页查询节点")
    @Permission(type = ResourceType.NODE, action = PermissionAction.READ)
    @GetMapping("/page/$DEFAULT_MAPPING_URI")
    fun listPageNode(
        artifactInfo: ArtifactInfo,
        nodeListOption: NodeListOption
    ): Response<Page<NodeInfo>> {
        // 禁止查询pipeline仓库
        PipelineRepoUtils.checkPipeline(artifactInfo.repoName)
        val nodePage = nodeService.listNodePage(artifactInfo, nodeListOption)
        return ResponseBuilder.success(nodePage)
    }

    @ApiOperation("查询节点删除点")
    @Permission(type = ResourceType.NODE, action = PermissionAction.READ)
    @GetMapping("/list-deleted/$DEFAULT_MAPPING_URI")
    fun listDeletedPoint(artifactInfo: ArtifactInfo): Response<List<NodeDeletedPoint>> {
        return ResponseBuilder.success(nodeService.listDeletedPoint(artifactInfo))
    }

    @ApiOperation("恢复被删除节点")
    @Permission(type = ResourceType.NODE, action = PermissionAction.WRITE)
    @PostMapping("/restore/$DEFAULT_MAPPING_URI")
    fun restoreNode(
        artifactInfo: ArtifactInfo,
        nodeRestoreOption: NodeRestoreOption
    ): Response<NodeRestoreResult> {
        return ResponseBuilder.success(nodeService.restoreNode(artifactInfo, nodeRestoreOption))
    }

    @ApiOperation("自定义查询节点")
    @PostMapping("/search")
    fun search(@RequestBody queryModel: QueryModel): Response<Page<Map<String, Any?>>> {
        return ResponseBuilder.success(nodeSearchService.search(queryModel))
    }

    @Deprecated("replace with search")
    @ApiOperation("自定义查询节点")
    @PostMapping("/query")
    fun query(@RequestBody queryModel: QueryModel): Response<Page<Map<String, Any?>>> {
        return ResponseBuilder.success(nodeSearchService.search(queryModel))
    }

    /**
     * 校验跨仓库操作权限
     */
    private fun checkCrossRepoPermission(request: UserNodeMoveCopyRequest) {
        with(request) {
            permissionManager.checkNodePermission(PermissionAction.WRITE, srcProjectId, srcRepoName, srcFullPath)
            val toProjectId = request.destProjectId ?: srcProjectId
            val toRepoName = request.destRepoName ?: srcRepoName
            permissionManager.checkNodePermission(PermissionAction.WRITE, toProjectId, toRepoName, destFullPath)
        }
    }
}
