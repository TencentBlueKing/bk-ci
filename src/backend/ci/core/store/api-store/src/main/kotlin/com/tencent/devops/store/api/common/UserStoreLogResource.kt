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

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_STORE_LOG", description = "研发商店-日志")
@Path("/user/store/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserStoreLogResource {

    @Operation(summary = "根据构建ID获取初始化所有日志")
    @GET
    @Path("/types/{storeType}/projects/{projectCode}/pipelines/{pipelineId}/builds/{buildId}/")
    fun getInitLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "研发商店组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs?>

    @Operation(summary = "获取更多日志")
    @GET
    @Path("/types/{storeType}/projects/{projectCode}/pipelines/{pipelineId}/builds/{buildId}/more")
    fun getMoreLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "研发商店组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "日志行数", required = false)
        @QueryParam("num")
        num: Int? = 100,
        @Parameter(description = "是否正序输出", required = false)
        @QueryParam("fromStart")
        fromStart: Boolean? = true,
        @Parameter(description = "起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @Parameter(description = "结尾行号", required = true)
        @QueryParam("end")
        end: Long,
        @Parameter(description = "对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs?>

    @Operation(summary = "获取某行后的日志")
    @GET
    @Path("/types/{storeType}/projects/{projectCode}/pipelines/{pipelineId}/builds/{buildId}/after")
    fun getAfterLogs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "研发商店组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @Parameter(description = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "起始行号", required = true)
        @QueryParam("start")
        start: Long,
        @Parameter(description = "是否包含调试日志", required = false)
        @QueryParam("debug")
        debug: Boolean? = false,
        @Parameter(description = "对应elementId", required = false)
        @QueryParam("tag")
        tag: String?,
        @Parameter(description = "执行次数", required = false)
        @QueryParam("executeCount")
        executeCount: Int?
    ): Result<QueryLogs?>
}
