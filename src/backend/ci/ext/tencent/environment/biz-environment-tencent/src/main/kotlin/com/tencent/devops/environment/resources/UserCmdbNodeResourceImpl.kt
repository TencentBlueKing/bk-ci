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

import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.ActionId
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserCmdbNodeResource
import com.tencent.devops.environment.pojo.CmdbNode
import com.tencent.devops.environment.pojo.ScrollIdPage
import com.tencent.devops.environment.pojo.job.AddCmdbNodesRes
import com.tencent.devops.environment.pojo.job.ImportCmdbNodeInfo
import com.tencent.devops.environment.pojo.job.ReImportCmdbNodeInfo
import com.tencent.devops.environment.service.CmdbNodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserCmdbNodeResourceImpl @Autowired constructor(
    private val cmdbNodeService: CmdbNodeService
) : UserCmdbNodeResource {

    override fun listUserCmdbNodesNew(
        userId: String,
        projectId: String,
        bakOperator: Boolean,
        page: Int,
        pageSize: Int,
        ips: List<String>?
    ): Result<Page<CmdbNode>> {
        return Result(
            cmdbNodeService.getUserCmdbNodesNew(
                userId = userId,
                bakOperator = bakOperator,
                page = page,
                pageSize = pageSize,
                projectId = projectId,
                ips = ips ?: listOf()
            )
        )
    }

    override fun listUserCmdbNodesWithScrollId(
        userId: String,
        projectId: String,
        bakOperator: Boolean,
        scrollId: String,
        pageSize: Int,
        ips: List<String>?
    ): Result<ScrollIdPage<CmdbNode>> {
        // TODO:逻辑待实现
        return Result(
            ScrollIdPage(
                scrollId = "0",
                hasNext = true,
                records = emptyList()
            )
        )
    }

    @AuditEntry(actionId = ActionId.ENV_NODE_CREATE)
    override fun addCmdbNodes(userId: String, projectId: String, nodeIps: List<String>): Result<AddCmdbNodesRes> {
        val addCmdbNodesRes = cmdbNodeService.addCmdbNodesByIp(
            userId = userId, projectId = projectId, nodeIpList = nodeIps
        )
        return Result(addCmdbNodesRes)
    }

    @AuditEntry(actionId = ActionId.ENV_NODE_CREATE)
    override fun addCmdbNode(
        userId: String,
        projectId: String,
        importCmdbNodeInfoList: List<ImportCmdbNodeInfo>
    ): Result<AddCmdbNodesRes> {
        val addCmdbNodeRes = cmdbNodeService.addCmdbNodesByServerId(
            userId = userId, projectId = projectId, nodeServerIdList = importCmdbNodeInfoList.map { it.serverId }
        )
        return Result(addCmdbNodeRes)
    }

    override fun reImportCmdbNodes(
        userId: String,
        projectId: String,
        reImportCmdbNodeInfoList: List<ReImportCmdbNodeInfo>
    ): Result<AddCmdbNodesRes> {
        var importTag = true // true - reImportCmdbNodesByServerId, false - reImportCmdbNodesByIp
        run {
            reImportCmdbNodeInfoList.forEach {
                if (null == it.serverId) {
                    importTag = false
                    return@run
                }
            }
        }
        val reImportCmdbNodesRes = if (importTag) {
            cmdbNodeService.reImportCmdbNodesByServerId(
                userId = userId,
                projectId = projectId,
                reImportCmdbNodeInfoList = reImportCmdbNodeInfoList.map {
                    ReImportCmdbNodeInfo(nodeId = it.nodeId, serverId = it.serverId)
                }
            )
        } else {
            cmdbNodeService.reImportCmdbNodesByIp(
                userId = userId,
                projectId = projectId,
                reImportCmdbNodeInfoList = reImportCmdbNodeInfoList.map {
                    ReImportCmdbNodeInfo(nodeIp = it.nodeIp, nodeId = it.nodeId)
                }
            )
        }
        return Result(reImportCmdbNodesRes)
    }
}
