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

package com.tencent.devops.ai.resources

import com.tencent.devops.ai.api.user.UserAiMcpServerResource
import com.tencent.devops.ai.pojo.AiMcpServerCreate
import com.tencent.devops.ai.pojo.AiMcpServerInfo
import com.tencent.devops.ai.pojo.AiMcpServerUpdate
import com.tencent.devops.ai.service.AiMcpServerService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

/** MCP 服务配置 API 接口实现。 */
@RestResource
class UserAiMcpServerResourceImpl @Autowired constructor(
    private val aiMcpServerService: AiMcpServerService
) : UserAiMcpServerResource {

    override fun create(
        userId: String,
        request: AiMcpServerCreate
    ): Result<AiMcpServerInfo> {
        return Result(aiMcpServerService.createServer(userId, request))
    }

    override fun list(userId: String): Result<List<AiMcpServerInfo>> {
        return Result(aiMcpServerService.listForUser(userId))
    }

    override fun update(
        userId: String,
        serverId: String,
        request: AiMcpServerUpdate
    ): Result<Boolean> {
        return Result(
            aiMcpServerService.updateServer(userId, serverId, request)
        )
    }

    override fun delete(
        userId: String,
        serverId: String
    ): Result<Boolean> {
        return Result(aiMcpServerService.deleteServer(userId, serverId))
    }
}
