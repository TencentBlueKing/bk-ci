/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.ActivityInfo
import com.tencent.devops.project.pojo.OPActivityUpdate
import com.tencent.devops.project.pojo.OPActivityVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ActivityType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "OP_ACTIVITI", description = "项目最新动态")
@Path("/op/activities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPActivityResource {

    @POST
    @Path("/types/{type}")
    @Operation(summary = "添加")
    fun addActivity(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "最新动态类型")
        @PathParam("type")
        type: ActivityType,
        @Parameter(description = "最新动态")
        info: ActivityInfo
    ): Result<Boolean>

    @GET
    @Path("/{fieldName}/enum")
    @Operation(summary = "获取字段常量信息")
    fun getField(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "最新动态ID")
        @PathParam("fieldName")
        fieldName: String
    ): Result<List<String>>

//    @GET
// //    @Path("/{fieldName}/enum")
//    @Path("/fields/{fieldName}/enum")
//    @Operation(summary = "获取字段常量信息")
//    fun getFieldV2(
//            @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @Parameter(description = "最新动态ID")
//            @PathParam("fieldName")
//            fieldName: String
//    ): Result<List<String>>

    @PUT
    @Path("/{activityId}")
    @Operation(summary = "修改")
    fun upDateActivity(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "最新动态ID")
        @PathParam("activityId")
        activityId: Long,
        @Parameter(description = "最新动态")
        opActivityUpdate: OPActivityUpdate
    ): Result<Boolean>

    @DELETE
    @Path("/{activityId}")
    @Operation(summary = "删除")
    fun deleteActivity(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "最新动态ID")
        @PathParam("activityId")
        activityId: Long
    ): Result<Boolean>

    @GET
    @Path("/{activityId}")
    @Operation(summary = "根据ID查询")
    fun getActivity(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "最新动态ID")
        @PathParam("activityId")
        activityId: Long
    ): Result<OPActivityVO>

    @GET
    @Path("/list")
    @Operation(summary = "查询最新动态列表")
    fun listActivity(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<OPActivityVO>>
}
