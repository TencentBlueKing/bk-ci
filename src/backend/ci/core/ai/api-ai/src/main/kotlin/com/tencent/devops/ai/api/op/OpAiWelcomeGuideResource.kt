/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲫持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲫持续集成平台 is licensed under the MIT license.
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

package com.tencent.devops.ai.api.op

import com.tencent.devops.ai.pojo.WelcomeGuideCreateRequest
import com.tencent.devops.ai.pojo.WelcomeGuideOpItemVO
import com.tencent.devops.ai.pojo.WelcomeGuidePatchRequest
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_AI_WELCOME_GUIDE", description = "运营-欢迎引导管理")
@Path("/op/ai/welcome-guide")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAiWelcomeGuideResource {

    @Operation(summary = "获取欢迎引导信息")
    @GET
    @Path("/")
    fun list(): Result<List<WelcomeGuideOpItemVO>>

    @Operation(summary = "创建欢迎引导")
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "欢迎引导数据", required = true)
        request: WelcomeGuideCreateRequest
    ): Result<Boolean>

    @Operation(summary = "更新欢迎引导")
    @PUT
    @Path("/{guideId}")
    fun update(
        @Parameter(description = "引导ID", required = true)
        @PathParam("guideId")
        guideId: String,
        @Parameter(description = "欢迎引导数据", required = true)
        request: WelcomeGuidePatchRequest
    ): Result<Boolean>

    @Operation(summary = "删除欢迎引导")
    @DELETE
    @Path("/{guideId}")
    fun delete(
        @Parameter(description = "引导ID", required = true)
        @PathParam("guideId")
        guideId: String
    ): Result<Boolean>
}
