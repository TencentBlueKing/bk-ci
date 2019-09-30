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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.project.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.ActivityInfo
import com.tencent.devops.project.pojo.OPActivityUpdate
import com.tencent.devops.project.pojo.OPActivityVO
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.ActivityType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PROJECT"], description = "项目最新动态")
@Path("/op/activities")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPActivityResource {

    @POST
    @Path("/types/{type}")
    @ApiOperation("添加")
    fun addActivity(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("最新动态类型")
        @PathParam("type")
        type: ActivityType,
        @ApiParam("最新动态")
        info: ActivityInfo
    ): Result<Boolean>

    @GET
    @Path("/{fieldName}/enum")
    @ApiOperation("获取字段常量信息")
    fun getField(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("最新动态ID")
        @PathParam("fieldName")
        fieldName: String
    ): Result<List<String>>

    @PUT
    @Path("/{activityId}")
    @ApiOperation("修改")
    fun upDateActivity(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("最新动态ID")
        @PathParam("activityId")
        activityId: Long,
        @ApiParam("最新动态")
        opActivityUpdate: OPActivityUpdate
    ): Result<Boolean>

    @DELETE
    @Path("/{activityId}")
    @ApiOperation("删除")
    fun deleteActivity(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("最新动态ID")
        @PathParam("activityId")
        activityId: Long
    ): Result<Boolean>

    @GET
    @Path("/{activityId}")
    @ApiOperation("根据ID查询")
    fun getActivity(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("最新动态ID")
        @PathParam("activityId")
        activityId: Long
    ): Result<OPActivityVO>

    @GET
    @Path("/list")
    @ApiOperation("查询最新动态列表")
    fun listActivity(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<OPActivityVO>>

    @GET
//    @Path("/{fieldName}/enum")
    @Path("/fieldName/{fieldName}/enum")
    @ApiOperation("获取字段常量信息")
    fun getFieldV2(
            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            userId: String,
            @ApiParam("最新动态ID")
            @PathParam("fieldName")
            fieldName: String
    ): Result<List<String>>

    @PUT
//    @Path("/{activityId}")
    @Path("/activityId/{activityId}")
    @ApiOperation("修改")
    fun upDateActivityV2(
            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            userId: String,
            @ApiParam("最新动态ID")
            @PathParam("activityId")
            activityId: Long,
            @ApiParam("最新动态")
            opActivityUpdate: OPActivityUpdate
    ): Result<Boolean>

    @DELETE
//    @Path("/{activityId}")
    @Path("/activityId/{activityId}")
    @ApiOperation("删除")
    fun deleteActivityV2(
            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            userId: String,
            @ApiParam("最新动态ID")
            @PathParam("activityId")
            activityId: Long
    ): Result<Boolean>

    @GET
//    @Path("/{activityId}")
    @Path("/activityId/{activityId}")
    @ApiOperation("根据ID查询")
    fun getActivityV2(
            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
            userId: String,
            @ApiParam("最新动态ID")
            @PathParam("activityId")
            activityId: Long
    ): Result<OPActivityVO>

}