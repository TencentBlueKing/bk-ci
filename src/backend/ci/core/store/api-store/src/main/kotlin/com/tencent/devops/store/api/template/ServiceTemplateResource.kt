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

package com.tencent.devops.store.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
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

@Tag(name = "SERVICE_MARKET_TEMPLATE", description = "服务端-模板")
@Path("/service/market/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTemplateResource {

    @Operation(summary = "模版市场搜索模版")
    @GET
    @Path("/list/")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<MarketTemplateResp>

    @Operation(summary = "安装模板到项目")
    @POST
    @Path("/template/install")
    fun installTemplate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "安装模板到项目请求报文体", required = true)
        installTemplateReq: InstallTemplateReq
    ): Result<Boolean>

    @Operation(summary = "校验模板内组件可见范围")
    @GET
    @Path("/{templateCode}/validate")
    fun validateUserTemplateComponentVisibleDept(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "标识", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @Parameter(description = "项目", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @Operation(summary = "校验流水线模型内组件可见范围")
    @POST
    @Path("/{projectCode}/verification")
    fun validateModelComponentVisibleDept(
        @Parameter(description = "用户", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线模型-阶段", required = true)
        @Valid
        model: Model,
        @Parameter(description = "项目", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @Operation(summary = "根据模板代码查看模板详情")
    @GET
    @Path("/templateCodes/{templateCode}")
    fun getTemplateDetailByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplateDetail?>

    @Operation(summary = "获取研发商店模板状态")
    @GET
    @Path("/getMarketTemplateStatus/{templateCode}")
    fun getMarketTemplateStatus(
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplateStatusEnum>

    @Operation(summary = "创建模板上架研发商店记录")
    @POST
    @Path("/published/create")
    fun createMarketTemplatePublishedVersion(
        @Parameter(description = "模板版本发布关联实体", required = true)
        templatePublishedVersionInfo: TemplatePublishedVersionInfo
    ): Result<Boolean>

    @Operation(summary = "获取模板最新上架版本")
    @GET
    @Path("{templateCode}/published/latest")
    fun getLatestMarketPublishedVersion(
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplatePublishedVersionInfo?>

    @Operation(summary = "删除模板发布历史版本")
    @DELETE
    @Path("/{templateCode}/published/versions/delete")
    fun deleteMarketPublishedVersions(
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @Parameter(description = "版本号列表", required = true)
        versions: List<Long>
    ): Result<Boolean>

    @Operation(summary = "删除模板发布历史")
    @DELETE
    @Path("/{templateCode}/published/delete")
    fun deleteMarketPublishedHistory(
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<Boolean>

    @Operation(summary = "批量获取模板最新发布版本")
    @POST
    @Path("/published/latest/list")
    fun listLatestPublishedVersions(
        @Parameter(description = "模板Code列表", required = true)
        templateCodes: List<String>
    ): Result<List<TemplatePublishedVersionInfo>>

    @Operation(summary = "记录模板版本安装历史")
    @POST
    @Path("/install/create")
    fun createTemplateInstallHistory(
        @Parameter(description = "模板版本安装历史实体", required = true)
        installHistoryInfo: TemplateVersionInstallHistoryInfo
    ): Result<Boolean>

    @Operation(summary = "删除模板版本安装历史")
    @DELETE
    @Path("/{templateCode}/install/delete")
    fun deleteTemplateInstallHistory(
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<Boolean>

    @Operation(summary = "删除模板版本安装历史版本")
    @DELETE
    @Path("/{srcTemplateCode}/{templateCode}/versions/delete")
    fun deleteTemplateInstallHistoryVersions(
        @Parameter(description = "父模板代码", required = true)
        @PathParam("srcTemplateCode")
        srcTemplateCode: String,
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @Parameter(description = "版本号列表", required = true)
        versions: List<Long>
    ): Result<Boolean>

    @Operation(summary = "获取模板最近安装历史")
    @GET
    @Path("/{templateCode}/install/recently")
    fun getRecentlyInstalledVersion(
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplateVersionInstallHistoryInfo?>

    @Operation(summary = "获取最新安装版本")
    @GET
    @Path("/{templateCode}/install/latest")
    fun getLatestInstalledVersion(
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplateVersionInstallHistoryInfo?>

    @Operation(summary = "批量获取模板最新安装版本")
    @POST
    @Path("/install/latest/list")
    fun listLatestInstalledVersions(
        @Parameter(description = "模板Code列表", required = true)
        templateCodes: List<String>
    ): Result<List<TemplateVersionInstallHistoryInfo>>
}
