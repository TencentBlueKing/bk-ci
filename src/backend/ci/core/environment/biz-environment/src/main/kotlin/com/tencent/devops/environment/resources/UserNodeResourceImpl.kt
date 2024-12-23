/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.ResourceAuthorizationHandoverDTO
import com.tencent.devops.common.service.prometheus.BkTimed
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserNodeResource
import com.tencent.devops.environment.permission.EnvNodeAuthorizationService
import com.tencent.devops.environment.pojo.DisplayName
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.NodeService
import com.tencent.devops.environment.utils.NodeUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserNodeResourceImpl @Autowired constructor(
    private val nodeService: NodeService,
    private val authorizationService: EnvNodeAuthorizationService
) : UserNodeResource {

    @BkTimed(extraTags = ["operate", "getNode"])
    override fun listUsableServerNodes(userId: String, projectId: String): Result<List<NodeWithPermission>> {
        return Result(NodeUtils.sortByUser(nodeService.listUsableServerNodes(userId, projectId), userId))
    }

    override fun hasCreatePermission(userId: String, projectId: String): Result<Boolean> {
        return Result(nodeService.hasCreatePermission(userId, projectId))
    }

    @AuditEntry(actionId = ActionId.ENV_NODE_DELETE)
    override fun deleteNodes(userId: String, projectId: String, nodeHashIds: List<String>): Result<Boolean> {
        nodeService.deleteNodes(userId, projectId, nodeHashIds.map { HashUtil.decodeIdToLong(it) })
        return Result(true)
    }

    @BkTimed(extraTags = ["operate", "getNode"])
    override fun list(userId: String, projectId: String): Result<List<NodeWithPermission>> {
        return Result(NodeUtils.sortByUser(nodeService.list(userId, projectId), userId))
    }

    @BkTimed(extraTags = ["operate", "getNode"])
    override fun listNew(
        userId: String,
        projectId: String,
        page: Int?,
        pageSize: Int?,
        nodeIp: String?,
        displayName: String?,
        createdUser: String?,
        lastModifiedUser: String?,
        keywords: String?,
        nodeType: NodeType?
    ): Result<Page<NodeWithPermission>> {
        return Result(
            nodeService.listNew(
                userId = userId,
                projectId = projectId,
                page = page,
                pageSize = pageSize,
                nodeIp = nodeIp,
                displayName = displayName,
                createdUser = createdUser,
                lastModifiedUser = lastModifiedUser,
                keywords = keywords,
                nodeType = nodeType
            )
        )
    }

    override fun changeCreatedUser(userId: String, projectId: String, nodeHashId: String): Result<Boolean> {
        val nodeDisplayName = nodeService.changeCreatedUser(userId, projectId, nodeHashId)
        authorizationService.batchModifyHandoverFrom(
            projectId = projectId,
            resourceAuthorizationHandoverList = listOf(
                ResourceAuthorizationHandoverDTO(
                    projectCode = projectId,
                    resourceType = AuthResourceType.ENVIRONMENT_ENV_NODE.value,
                    resourceName = nodeDisplayName,
                    resourceCode = nodeHashId,
                    handoverTo = userId
                )
            )
        )
        return Result(true)
    }

    @AuditEntry(actionId = ActionId.ENV_NODE_EDIT)
    override fun updateDisplayName(
        userId: String,
        projectId: String,
        nodeHashId: String,
        displayName: DisplayName
    ): Result<Boolean> {
        nodeService.updateDisplayName(userId, projectId, nodeHashId, displayName.displayName)
        return Result(true)
    }
}
