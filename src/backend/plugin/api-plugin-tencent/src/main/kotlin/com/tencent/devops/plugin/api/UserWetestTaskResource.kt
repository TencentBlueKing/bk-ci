/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.wetest.WetestCloud
import com.tencent.devops.plugin.pojo.wetest.WetestTask
import com.tencent.devops.plugin.pojo.wetest.WetestTaskParam
import com.tencent.devops.plugin.pojo.wetest.WetestTaskResponse
import com.tencent.devops.plugin.pojo.wetest.WetestTestType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_WETEST_TASK"], description = "用户-WETEST测试任务")
@Path("/user/wetest/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserWetestTaskResource {

    @ApiOperation("新增WETEST测试任务")
    @POST
    @Path("/{projectId}/create")
    fun create(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("weTest任务", required = true)
        weTestTask: WetestTaskParam
    ): Result<Map<String, Int>>

    @ApiOperation("更新WETEST测试任务")
    @POST
    @Path("/{projectId}/update")
    fun update(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("ID", required = true)
        @QueryParam("id")
        id: Int,
        @ApiParam("名称", required = false)
        weTestTask: WetestTaskParam
    ): Result<Boolean>

    @ApiOperation("获取单个WETEST测试任务")
    @POST
    @Path("/{projectId}/get")
    fun get(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("ID", required = true)
        @QueryParam("id")
        id: Int
    ): Result<WetestTask?>

    @ApiOperation("删除WETEST测试任务")
    @POST
    @Path("/{projectId}/delete")
    fun delete(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("ID", required = true)
        @QueryParam("id")
        id: Int
    ): Result<Boolean>

    @ApiOperation("查询WETEST测试任务列表")
    @GET
    @Path("/{projectId}/list")
    fun getList(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "开始页数，从1开始", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页数据条数", required = false, defaultValue = "12")
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<WetestTaskResponse?>

    @ApiOperation("获取测试类型及脚本类型")
    @GET
    @Path("/{projectId}/getTestTypeScriptType")
    fun getTestTypeScriptType(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线创建人的ID", required = true)
        @QueryParam("createUser")
        createUser: String
    ): Result<List<WetestTestType>>

    @ApiOperation("查询WETEST拉取个人私有云配置")
    @GET
    @Path("/{projectId}/getMyCloud")
    fun getMyCloud(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Map<String, String>?>

    @ApiOperation("获取私有云设备列表")
    @GET
    @Path("/{projectId}/getPrivateCloudDevice")
    fun getPrivateCloudDevice(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("私有云ID，逗号分隔", required = true)
        @QueryParam("cloudIds")
        cloudIds: String,
        @ApiParam("是否需要设备在线，默认为1", required = false)
        @QueryParam("online")
        online: String?,
        @ApiParam("是否需要设备空闲，默认为0", required = false)
        @QueryParam("free")
        free: String?
    ): Result<List<WetestCloud>>

    @ApiOperation("根据测试ID拉取测试相关的信息")
    @GET
    @Path("/{projectId}/getPrivateTestInfo")
    fun getPrivateTestInfo(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("测试ID", required = true)
        @QueryParam("testId")
        testId: String,
        @ApiParam("流水线创建人的ID", required = true)
        @QueryParam("createUser")
        createUser: String
    ): Result<Map<String, Any>>

    @ApiOperation("通过测试ID和设备ID批量获取性能和错误信息，防止测试设备过多导致批量拉取时间长")
    @GET
    @Path("/{projectId}/getTestDevicePerfError")
    fun getTestDevicePerfError(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("测试ID", required = true)
        @QueryParam("testId")
        testId: String,
        @ApiParam("设备ID", required = true)
        @QueryParam("deviceId")
        deviceId: String,
        @ApiParam("是否需要性能数据，默认为1", required = false)
        @QueryParam("needperf")
        needPerf: String?,
        @ApiParam("是否需要错误拉取，默认为1", required = false)
        @QueryParam("neederror")
        needError: String?,
        @ApiParam("流水线创建人的ID", required = true)
        @QueryParam("createUser")
        createUser: String
    ): Result<Map<String, Any>>

    @ApiOperation("通过测试ID和设备ID批量获取性能和错误信息，防止测试设备过多导致批量拉取时间长")
    @GET
    @Path("/{projectId}/getTestDeviceImageLog")
    fun getTestDeviceImageLog(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("测试ID", required = true)
        @QueryParam("testId")
        testId: String,
        @ApiParam("设备ID", required = true)
        @QueryParam("deviceId")
        deviceId: String,
        @ApiParam("是否需要截图，默认为1", required = false)
        @QueryParam("needimage")
        needImage: String?,
        @ApiParam("是否需要日志，默认为1", required = false)
        @QueryParam("needlog")
        needLog: String?,
        @ApiParam("流水线创建人的ID", required = true)
        @QueryParam("createUser")
        createUser: String
    ): Result<Map<String, Any>>

    @ApiOperation("根据测试ID拉取测试相关的日志信息")
    @GET
    @Path("/{projectId}/getTestLogContent")
    fun getTestLogContent(
        @ApiParam("用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("测试ID", required = true)
        @QueryParam("testId")
        testId: String,
        @ApiParam("设备ID", required = true)
        @QueryParam("deviceId")
        deviceId: String,
        @ApiParam("日式级别，为all/error/warn/info/debug，默认为all", required = false)
        @QueryParam("level")
        level: String?,
        @ApiParam("开始行数，从1开始，默认为1", required = false)
        @QueryParam("startLine")
        startLine: Int?,
        @ApiParam("拉取行数，默认200", required = false)
        @QueryParam("lineCnt")
        lineCnt: Int?,
        @ApiParam("流水线创建人的ID", required = true)
        @QueryParam("createUser")
        createUser: String
    ): Result<Map<String, Any>>
}