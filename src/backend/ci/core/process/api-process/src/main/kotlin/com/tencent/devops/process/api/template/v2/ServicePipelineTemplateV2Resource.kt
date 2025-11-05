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

package com.tencent.devops.process.api.template.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.template.UpgradeStrategyEnum
import com.tencent.devops.process.pojo.PipelineTemplateVersionSimple
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.pojo.template.v2.MarketTemplateV2Request
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateDetailsResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoResponse
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateMarketCreateReq
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_TEMPLATE_V2", description = "服务-模板资源-v2")
@Path("/service/templates/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineTemplateV2Resource {
    @Operation(summary = "研发商店上架事件处理")
    @POST
    @Path("/store/update")
    fun handleMarketTemplatePublished(
        @Parameter(description = "安装模板请求报文体", required = true)
        request: MarketTemplateV2Request
    ): Result<Boolean>

    @Operation(summary = "研发商店模板版本上架处理")
    @PUT
    @Path("/{templateId}/{version}/market/published/template/version/")
    fun handleMarketTemplateVersionPublished(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "上架版本", required = true)
        @PathParam("version")
        version: Long
    ): Result<Boolean>

    @Operation(summary = "查看模板详情")
    @GET
    @Path("/{projectId}/{templateId}/details/")
    fun getTemplateDetails(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "版本", required = false)
        @QueryParam("version")
        version: Long?
    ): Result<PipelineTemplateDetailsResponse>

    @Operation(summary = "判断模板镜像是否发布")
    @GET
    @Path("/store/templates/{projectId}/{templateId}/images/releaseStatus/check")
    fun checkImageReleaseStatus(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模版版本", required = false)
        @QueryParam("version")
        version: Long
    ): Result<String?>

    @Operation(summary = "查询项目下所有源模板的ID")
    @GET
    @Path("/store/projects/{projectId}/srcTemplates")
    fun getSrcTemplateCodes(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<String>>

    @Operation(summary = "更新研发商店摸吧状态")
    @PUT
    @Path("/{templateId}/store/updateStoreStatus")
    fun updateStoreStatus(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "研发商店模板状态", required = true)
        @QueryParam("storeStatus")
        storeStatus: TemplateStatusEnum,
        @Parameter(description = "下架版本", required = true)
        @QueryParam("version")
        version: Long?
    ): Result<Boolean>

    @Operation(summary = "更新发布策略")
    @PUT
    @Path("/{templateId}/store/publishStrategy")
    fun updatePublishStrategy(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "发布策略", required = true)
        strategy: UpgradeStrategyEnum
    ): Result<Boolean>

    @Operation(summary = "上架研发商店-校验")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}/published/check")
    fun checkWhenPublishedTemplate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @Parameter(description = "模版版本", required = false)
        @QueryParam("version")
        version: Long
    ): Result<Boolean>

    @Operation(summary = "查看模板基本信息")
    @GET
    @Path("/{projectId}/{templateId}/info/")
    fun getTemplateInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<PipelineTemplateInfoResponse>

    @Operation(summary = "获取模板最新正式版本")
    @POST
    @Path("/listLatestReleasedVersions")
    fun listLatestReleasedVersions(
        @Parameter(description = "模板ID列表", required = true)
        templateIds: List<String>
    ): Result<List<PipelineTemplateVersionSimple>>

    @Operation(summary = "获取模板最新版本")
    @POST
    @Path("/listPacSettings")
    fun listPacSettings(
        @Parameter(description = "模板ID列表", required = true)
        templateIds: List<String>
    ): Result<Map<String, Boolean>>

    @Operation(summary = "研发商店导入模板")
    @POST
    @Path("/{projectId}/create/market")
    fun createByMarket(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String?,
        @Parameter(description = "请求体", required = true)
        request: PipelineTemplateMarketCreateReq
    ): Result<DeployTemplateResult>
}
