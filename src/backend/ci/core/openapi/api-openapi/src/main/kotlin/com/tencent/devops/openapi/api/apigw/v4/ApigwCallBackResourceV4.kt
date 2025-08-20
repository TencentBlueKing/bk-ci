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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.process.pojo.CreateCallBackResult
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_CALLBACK_V4", description = "OPENAPI-callback资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/callbacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v4")
interface ApigwCallBackResourceV4 {

    @Operation(summary = "创建callback回调，调用需要项目管理员身份", tags = ["v4_app_callback_create", "v4_user_callback_create"])
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "url", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "region", required = true)
        @QueryParam("region")
        region: CallBackNetWorkRegionType,
        @Parameter(description = "event", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @Parameter(description = "该参数将会在回调中X-DEVOPS-WEBHOOK-TOKEN返回", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<Boolean>

    @Operation(summary = "批量创建callback回调", tags = ["v4_user_callback_batch_create", "v4_app_callback_batch_create"])
    @POST
    @Path("/batch")
    fun batchCreate(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "url", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "region", required = true)
        @QueryParam("region")
        region: CallBackNetWorkRegionType,
        @Parameter(description = "event", required = true)
        @QueryParam("event")
        event: String,
        @Parameter(description = "secretToken", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<CreateCallBackResult>

    @Operation(summary = "callback回调列表", tags = ["v4_app_callback_list", "v4_user_callback_list"])
    @GET
    @Path("/callback_list")
    fun list(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBack>>

    @Operation(summary = "callback回调移除", tags = ["v4_app_callback_delete", "v4_user_callback_delete"])
    @DELETE
    @Path("/callback")
    fun remove(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "传入的是回调列表接口返回的id：v3_user/app_callback_list", required = true)
        @QueryParam("id")
        id: Long
    ): Result<Boolean>

    @Operation(summary = "callback回调执行历史记录", tags = ["v4_user_callback_history_list", "v4_app_callback_history_list"])
    @GET
    @Path("/callback_history")
    fun listHistory(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "回调url", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "事件类型", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @Parameter(description = "开始时间(yyyy-MM-dd HH:mm:ss格式)", required = false)
        @QueryParam("startTime")
        startTime: String?,
        @Parameter(description = "结束时间(yyyy-MM-dd HH:mm:ss格式)", required = false)
        @QueryParam("endTime")
        endTime: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBackHistory>>

    @Operation(summary = "callback回调重试", tags = ["v4_user_callback_history_retry", "v4_app_callback_history_retry"])
    @POST
    @Path("/callback_retry")
    fun retry(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "id", required = true)
        @QueryParam("id")
        id: Long
    ): Result<Boolean>
}
