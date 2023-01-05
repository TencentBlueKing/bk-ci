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

package com.tencent.devops.openapi.api.apigw.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.wetest.pojo.wetest.WeTestAtomHistory
import com.tencent.devops.wetest.pojo.wetest.WeTestAtomRecord
import com.tencent.devops.wetest.pojo.wetest.WeTestFunctionTaskResponse
import com.tencent.devops.wetest.pojo.wetest.WeTestModelCloud
import com.tencent.devops.wetest.pojo.wetest.WeTestTaskInstRecord
import com.tencent.devops.wetest.pojo.wetest.WetestAutoTestRequest
import com.tencent.devops.wetest.pojo.wetest.WetestEmailGroup
import com.tencent.devops.wetest.pojo.wetest.WetestInstStatus
import com.tencent.devops.wetest.pojo.wetest.WetestTask
import com.tencent.devops.wetest.pojo.wetest.WetestTaskInst
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

@Api(tags = ["OPEN_API_V2_WETEST"], description = "OPEN-API-V2-WETEST")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v2/wetest")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwWetestResource {
    @ApiOperation("上传相应的包")
    @POST
    @Path("/uploadRes")
    fun uploadRes(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("凭证id", required = true)
        @QueryParam("accessId")
        accessId: String,
        @ApiParam("凭证的token", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("apk或script或者ipa", required = true)
        @QueryParam("type")
        type: String,
        @ApiParam("文件上传相关参数", required = true)
        fileParams: ArtifactorySearchParam
    ): Result<Map<String, Any>>

    @ApiOperation("上传相应的包（md5方式）")
    @POST
    @Path("/uploadResByMd5")
    fun uploadResByMd5(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("凭证id", required = true)
        @QueryParam("accessId")
        accessId: String,
        @ApiParam("凭证的token", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("apk或script或者ipa", required = true)
        @QueryParam("type")
        type: String,
        @ApiParam("文件上传相关参数", required = true)
        fileParams: ArtifactorySearchParam
    ): Result<Map<String, Any>>

    @ApiOperation("提交测试")
    @POST
    @Path("/autoTest")
    fun autoTest(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("凭证id", required = true)
        @QueryParam("accessId")
        accessId: String,
        @ApiParam("凭证的token", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("提交测试相关参数", required = true)
        request: WetestAutoTestRequest
    ): Result<Map<String, Any>>

    @ApiOperation("测试进度查询")
    @GET
    @Path("/queryTestStatus")
    fun queryTestStatus(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("凭证id", required = true)
        @QueryParam("accessId")
        accessId: String,
        @ApiParam("凭证的token", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("测试ID", required = true)
        @QueryParam("testId")
        testId: String
    ): Result<Map<String, Any>>

    @ApiOperation("获取wetest task信息")
    @GET
    @Path("/getTask")
    fun getTask(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("任务id", required = true)
        @QueryParam("taskId")
        taskId: String,
        @ApiParam("项目id", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<WetestTask?>

    @ApiOperation("根据筛选项返回机型")
    @GET
    @Path("/{projectId}/getModelListCloudWetest")
    fun getModelListCloudWetest(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户rtx", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("私有云ID，逗号分隔", required = true)
        @QueryParam("cloudIds")
        cloudIds: String,
        @ApiParam("是否需要设备在线，默认为1", required = false)
        @QueryParam("online")
        online: String?,
        @ApiParam("拉取机型种类，默认为3 全部 2 ios 1 android", required = false)
        @QueryParam("devicetype")
        devicetype: String?,
        @ApiParam("筛选项品牌 manu", required = false)
        @QueryParam("manu")
        manu: String?,
        @ApiParam("筛选项操作系统版本 version", required = false)
        @QueryParam("version")
        version: String?,
        @ApiParam("筛选项分辨率 resolution", required = false)
        @QueryParam("resolution")
        resolution: String?,
        @ApiParam("筛选项内存 mem_show", required = false)
        @QueryParam("mem_show")
        mem_show: String?
    ): Result<List<WeTestModelCloud>>

    @ApiOperation("保存wetest task实例信息")
    @POST
    @Path("/saveTask")
    fun saveTaskInst(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        wetestTaskInst: WetestTaskInst
    ): Result<String>

    @ApiOperation("更新wetest task实例信息")
    @POST
    @Path("/updateTaskInstStatus")
    fun updateTaskInstStatus(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("测试任务id", required = true)
        @QueryParam("testId")
        testId: String,
        @ApiParam("任务状态", required = true)
        @QueryParam("status")
        status: WetestInstStatus,
        @ApiParam("", required = false)
        @QueryParam("passRate")
        passRate: String?
    ): Result<String>

    @ApiOperation("拉取task的上报信息")
    @GET
    @Path("/getReportInfo")
    fun getReportInfo(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("测试任务id", required = true)
        @QueryParam("testId")
        testId: String
    ): Result<String>

    @ApiOperation("查询WETEST功能测试任务列表")
    @GET
    @Path("/FunctionalTaskList")
    fun getFunctionalTaskList(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<WeTestFunctionTaskResponse?>

    @ApiOperation("验证用户身份")
    @GET
    @Path("/{project}/checkAuthPermission")
    fun checkAuthPermission(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("project")
        project: String
    ): Result<Boolean>

    @ApiOperation("获取流水线中文名称")
    @GET
    @Path("/{project}/getPipelineName")
    fun getPipelineName(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("流水线ID", required = true)
        @QueryParam("pipelineIds")
        pipelineIds: String,
        @ApiParam("项目ID", required = true)
        @PathParam("project")
        project: String
    ): Result<Map<String, String>>

    @ApiOperation("保存插件执行实例信息")
    @POST
    @Path("/saveAtomHistroy")
    fun saveAtomHistroy(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        wetestAtomHistory: WeTestAtomHistory
    ): Result<Map<String, Int>>

    @ApiOperation("更新插件执行实例信息")
    @POST
    @Path("/{projectId}/updateAtomBeginUpload")
    fun updateAtomBeginUpload(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("插件执行id", required = true)
        @QueryParam("Id")
        Id: Int,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("插件执行状态")
        @QueryParam("result")
        result: String
    ): Result<String>

    @ApiOperation("更新插件执行实例信息")
    @POST
    @Path("/{projectId}/updateAtomBeginTest")
    fun updateAtomBeginTest(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("插件执行id", required = true)
        @QueryParam("Id")
        Id: Int,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("提测ID")
        @QueryParam("testId")
        testId: String?,
        @ApiParam("插件执行状态")
        @QueryParam("result")
        result: String
    ): Result<String>

    @ApiOperation("更新插件执行实例信息")
    @POST
    @Path("/{projectId}/updateAtomResult")
    fun updateAtomResult(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("插件执行id", required = true)
        @QueryParam("Id")
        Id: Int,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("插件执行状态")
        @QueryParam("result")
        result: String
    ): Result<String>

    @ApiOperation("同步执行历史taskInst数据")
    @GET
    @Path("/taskInstBydate")
    fun taskInstBydate(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("开始日期", required = true)
        @QueryParam("startDate")
        startDate: String,
        @ApiParam("结束日期", required = true)
        @QueryParam("endDate")
        endDate: String
    ): Result<List<WeTestTaskInstRecord>>

    @ApiOperation("同步设备列表task数据")
    @GET
    @Path("/taskBydate")
    fun taskBydate(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("开始日期", required = true)
        @QueryParam("startDate")
        startDate: String,
        @ApiParam("结束日期", required = true)
        @QueryParam("endDate")
        endDate: String
    ): Result<List<WetestTask>>

    @ApiOperation("同步邮件组emailgroup数据")
    @GET
    @Path("/emailGroupBydate")
    fun emailGroupBydate(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("开始日期", required = true)
        @QueryParam("startDate")
        startDate: String,
        @ApiParam("结束日期", required = true)
        @QueryParam("endDate")
        endDate: String
    ): Result<List<WetestEmailGroup>>

    @ApiOperation("同步插件统计Atom数据")
    @GET
    @Path("/atomBydate")
    fun atomBydate(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("开始日期", required = true)
        @QueryParam("startDate")
        startDate: String,
        @ApiParam("结束日期", required = true)
        @QueryParam("endDate")
        endDate: String
    ): Result<List<WeTestAtomRecord>>
}
