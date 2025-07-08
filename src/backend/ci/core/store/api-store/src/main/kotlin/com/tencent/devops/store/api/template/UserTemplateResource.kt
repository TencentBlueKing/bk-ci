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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.InstallTemplateResp
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
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

@Tag(name = "USER_MARKET_TEMPLATE", description = "模板")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserTemplateResource {

    @Operation(summary = "模版市场首页")
    @Path("/template/list/main")
    @GET
    fun mainPageList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketTemplateMain>>

    @Operation(summary = "模版市场搜索模版")
    @GET
    @Path("/template/list/")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "模版分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "应用范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @Parameter(description = "功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @Parameter(description = "评分", required = false)
        @QueryParam("score")
        score: Int?,
        @Parameter(description = "研发来源", required = false)
        @QueryParam("rdType")
        rdType: TemplateRdTypeEnum?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: MarketTemplateSortTypeEnum? = MarketTemplateSortTypeEnum.CREATE_TIME,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectCode")
        projectCode: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketTemplateResp>

    @Operation(summary = "删除工作台模版")
    @DELETE
    @Path("/templates/{templateCode}")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板Code", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<Boolean>

    @Operation(summary = "根据ID查看模板详情")
    @GET
    @Path("/template/templateIds/{templateId}")
    fun getTemplateDetailById(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<TemplateDetail?>

    @Operation(summary = "根据模板代码查看模板详情")
    @GET
    @Path("/template/templateCodes/{templateCode}")
    fun getTemplateDetailByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplateDetail?>

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

    @Operation(summary = "安装模板到项目--返回流水线模板信息")
    @POST
    @Path("/template/install/new")
    fun installTemplateNew(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "安装模板到项目请求报文体", required = true)
        installTemplateReq: InstallTemplateReq
    ): Result<InstallTemplateResp>

    @Operation(summary = "根据模板标识获取已安装的项目列表")
    @GET
    @Path("/template/installedProjects/{templateCode}")
    fun getInstalledProjects(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模版代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<List<InstalledProjRespItem?>>

    @Operation(summary = "根据用户获取原子工作台模板列表")
    @GET
    @Path("/desk/template/list")
    fun getMyTemplates(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "模版名称", required = false)
        @QueryParam("templateName")
        templateName: String?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<MyTemplateItem>?>
}
