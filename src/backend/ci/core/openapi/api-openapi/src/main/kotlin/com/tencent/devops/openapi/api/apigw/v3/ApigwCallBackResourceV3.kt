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
package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.process.pojo.CreateCallBackResult
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPENAPI_CALLBACK_V3"], description = "OPENAPI-callback资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/projects/{projectId}/callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwCallBackResourceV3 {

    @ApiOperation("创建callback回调，调用需要项目管理员身份", tags = ["v3_app_callback_create", "v3_user_callback_create"])
    @POST
    @Path("/")
    fun create(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("url", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("region", required = true)
        @QueryParam("region")
        region: CallBackNetWorkRegionType,
        @ApiParam("event", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @ApiParam("该参数将会在回调中X-DEVOPS-WEBHOOK-TOKEN返回", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<Boolean>

    @ApiOperation("批量创建callback回调", tags = ["v3_user_callback_batch_create", "v3_app_callback_batch_create"])
    @POST
    @Path("/batch")
    fun batchCreate(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("url", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("region", required = true)
        @QueryParam("region")
        region: CallBackNetWorkRegionType,
        @ApiParam("event", required = true)
        @QueryParam("event")
        event: String,
        @ApiParam("secretToken", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<CreateCallBackResult>

    @ApiOperation("callback回调列表", tags = ["v3_app_callback_list", "v3_user_callback_list"])
    @GET
    @Path("/")
    fun list(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBack>>

    @ApiOperation("callback回调移除", tags = ["v3_app_callback_delete", "v3_user_callback_delete"])
    @DELETE
    @Path("/{id}")
    fun remove(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("传入的是回调列表接口返回的id：v3_user/app_callback_list", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>

    @ApiOperation("callback回调执行历史记录", tags = ["v3_user_callback_history_list", "v3_app_callback_history_list"])
    @GET
    @Path("/history")
    fun listHistory(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("回调url", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam("事件类型", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @ApiParam("开始时间(yyyy-MM-dd HH:mm:ss格式)", required = false)
        @QueryParam("startTime")
        startTime: String?,
        @ApiParam("结束时间(yyyy-MM-dd HH:mm:ss格式)", required = false)
        @QueryParam("endTime")
        endTime: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBackHistory>>

    @ApiOperation("callback回调重试", tags = [])
    @POST
    @Path("/history/{id}/retry")
    fun retry(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>
}
