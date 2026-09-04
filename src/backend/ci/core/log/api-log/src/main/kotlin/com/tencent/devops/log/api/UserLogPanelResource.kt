/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */

package com.tencent.devops.log.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.log.pojo.QueryLogPanel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * 执行详情日志 Tab 专用读接口。路径与 /user/logs 隔离，不改存量 init/after/download。
 */
@Tag(name = "USER_LOG_PANEL", description = "用户-日志面板")
@Path("/user/log/panels")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserLogPanelResource {

    @Operation(summary = "打开日志 Tab：默认落在最新一页")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/latest")
    fun getLatestPage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        @Parameter(description = "插件 elementId") @QueryParam("tag") tag: String?,
        @Parameter(description = "插件内 subTag") @QueryParam("subTag") subTag: String?,
        @Parameter(description = "容器 hash，现网 User API 的 jobId") @QueryParam("jobId") jobId: String?,
        @Parameter(description = "执行次数") @QueryParam("executeCount") executeCount: Int?,
        @Parameter(description = "级别，逗号分隔，默认 INFO,WARN,ERROR") @QueryParam("levels") levels: String?,
        @Parameter(description = "每页行数，默认 200，最大 500") @QueryParam("pageSize") pageSize: Int?,
        @Parameter(description = "是否查询归档数据") @QueryParam("archiveFlag") archiveFlag: Boolean? = false
    ): Result<QueryLogPanel>

    @Operation(summary = "向上滚动：加载比 endLineNo 更早的一页")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/before")
    fun getBeforePage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        @Parameter(description = "当前页首行号（不含）", required = true) @QueryParam("endLineNo") endLineNo: Long,
        @QueryParam("tag") tag: String?,
        @QueryParam("subTag") subTag: String?,
        @QueryParam("jobId") jobId: String?,
        @QueryParam("executeCount") executeCount: Int?,
        @QueryParam("levels") levels: String?,
        @QueryParam("pageSize") pageSize: Int?,
        @QueryParam("archiveFlag") archiveFlag: Boolean? = false
    ): Result<QueryLogPanel>

    @Operation(summary = "停留底部时跟随：加载比 startLineNo 更新的日志")
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/after")
    fun getAfterPage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        @Parameter(description = "当前页末行号（不含）", required = true) @QueryParam("startLineNo") startLineNo: Long,
        @QueryParam("tag") tag: String?,
        @QueryParam("subTag") subTag: String?,
        @QueryParam("jobId") jobId: String?,
        @QueryParam("executeCount") executeCount: Int?,
        @QueryParam("levels") levels: String?,
        @QueryParam("pageSize") pageSize: Int?,
        @QueryParam("archiveFlag") archiveFlag: Boolean? = false
    ): Result<QueryLogPanel>
}
