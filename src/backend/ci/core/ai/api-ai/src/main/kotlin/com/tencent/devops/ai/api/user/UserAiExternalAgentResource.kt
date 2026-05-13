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

package com.tencent.devops.ai.api.user

import com.tencent.devops.ai.pojo.ExternalAgentCreate
import com.tencent.devops.ai.pojo.ExternalAgentInfo
import com.tencent.devops.ai.pojo.ExternalAgentUpdate
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_AI_EXTERNAL_AGENT", description = "用户-AI外部智能体配置")
@Path("/user/ai/external/agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserAiExternalAgentResource {

    @Operation(summary = "注册外部智能体")
    @POST
    @Path("/")
    fun create(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建外部智能体请求体", required = true)
        request: ExternalAgentCreate
    ): Result<ExternalAgentInfo>

    @Operation(summary = "获取我的外部智能体列表")
    @GET
    @Path("/")
    fun list(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ExternalAgentInfo>>

    @Operation(summary = "更新外部智能体配置")
    @PUT
    @Path("/{configId}")
    fun update(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "配置ID", required = true)
        @PathParam("configId")
        configId: String,
        @Parameter(description = "更新外部智能体请求体", required = true)
        request: ExternalAgentUpdate
    ): Result<Boolean>

    @Operation(summary = "删除外部智能体配置")
    @DELETE
    @Path("/{configId}")
    fun delete(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "配置ID", required = true)
        @PathParam("configId")
        configId: String
    ): Result<Boolean>
}
