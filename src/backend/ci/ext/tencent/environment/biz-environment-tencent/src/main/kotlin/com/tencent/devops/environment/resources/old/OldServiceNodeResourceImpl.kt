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

package com.tencent.devops.environment.resources.old

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.old.OldServiceNodeResource
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.service.EnvService
import com.tencent.devops.environment.service.NodeService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OldServiceNodeResourceImpl @Autowired constructor(
    private val nodeService: NodeService,
    private val envService: EnvService
) : OldServiceNodeResource {
    override fun listNodeByNodeType(projectId: String, nodeType: NodeType): Result<List<NodeBaseInfo>> {
        return Result(nodeService.listByNodeType("", projectId, nodeType))
    }

    override fun listRawByHashIds(
        userId: String,
        projectId: String,
        nodeHashIds: List<String>
    ): Result<List<NodeBaseInfo>> {
        if (nodeHashIds.isEmpty()) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                params = arrayOf("nodeHashIds")
            )
        }

        return Result(nodeService.listRawServerNodeByIds(userId, projectId, nodeHashIds))
    }

    override fun listRawByEnvHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Result<Map<String, List<NodeBaseInfo>>> {
        if (envHashIds.isEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("envHashIds"))
        }

        return Result(envService.listRawServerNodeByEnvHashIds(userId, projectId, envHashIds))
    }

    override fun listUsableServerNodes(userId: String, projectId: String): Result<List<NodeWithPermission>> {
        return Result(nodeService.listUsableServerNodes(userId, projectId))
    }

    override fun listByHashIds(
        userId: String,
        projectId: String,
        nodeHashIds: List<String>
    ): Result<List<NodeWithPermission>> {
        return Result(nodeService.listByHashIds(userId, projectId, nodeHashIds))
    }

    override fun listNodeByType(userId: String, projectId: String, type: String): Result<List<NodeBaseInfo>> {
        return Result(nodeService.listByType(userId, projectId, type))
    }
}
