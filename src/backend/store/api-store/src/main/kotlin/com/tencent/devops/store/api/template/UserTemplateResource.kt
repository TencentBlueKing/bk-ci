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

package com.tencent.devops.store.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.pojo.template.MyTemplateItem
import com.tencent.devops.store.pojo.template.TemplateDetail
import com.tencent.devops.store.pojo.template.TemplateProcessInfo
import com.tencent.devops.store.pojo.template.enums.MarketTemplateSortTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateRdTypeEnum
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
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_MARKET_TEMPLATE"], description = "模板")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTemplateResource {

    @ApiOperation("模版市场首页")
    @Path("/template/list/main")
    @GET
    fun mainPageList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<MarketTemplateMain>>

    @ApiOperation("模版市场搜索模版")
    @GET
    @Path("/template/list/")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模版名称", required = false)
        @QueryParam("templateName")
        templateName: String?,
        @ApiParam("模版分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("应用范畴", required = false)
        @QueryParam("categoryCode")
        categoryCode: String?,
        @ApiParam("功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @ApiParam("评分", required = false)
        @QueryParam("score")
        score: Int?,
        @ApiParam("研发来源", required = false)
        @QueryParam("rdType")
        rdType: TemplateRdTypeEnum?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: MarketTemplateSortTypeEnum? = MarketTemplateSortTypeEnum.CREATE_TIME,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MarketTemplateResp>

    @ApiOperation("关联模板")
    @POST
    @Path("/templates/{templateCode}/store/rel")
    fun addMarketTemplate(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @ApiParam("关联模板请求报文体", required = true)
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean>

    @ApiOperation("上架模板")
    @PUT
    @Path("/desk/template/release")
    fun updateMarketTemplate(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("上架模板请求报文体", required = true)
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?>

    @ApiOperation("删除工作台模版")
    @DELETE
    @Path("/templates/{templateCode}")
    fun delete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板Code", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<Boolean>

    @ApiOperation("根据ID查看模板详情")
    @GET
    @Path("/template/templateIds/{templateId}")
    fun getTemplateDetailById(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<TemplateDetail?>

    @ApiOperation("根据模板代码查看模板详情")
    @GET
    @Path("/template/templateCodes/{templateCode}")
    fun getTemplateDetailByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplateDetail?>

    @ApiOperation("根据插件版本ID获取插件版本进度")
    @GET
    @Path("/desk/template/release/process/{templateId}")
    fun getProcessInfo(
        @ApiParam("templateId", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<TemplateProcessInfo>

    @ApiOperation("安装模板到项目")
    @POST
    @Path("/template/install")
    fun installTemplate(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("安装模板到项目请求报文体", required = true)
        installTemplateReq: InstallTemplateReq
    ): Result<Boolean>

    @ApiOperation("根据模板标识获取已安装的项目列表")
    @GET
    @Path("/template/installedProjects/{templateCode}")
    fun getInstalledProjects(
        @ApiParam("token", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模版代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<List<InstalledProjRespItem?>>

    @ApiOperation("根据用户获取插件工作台模板列表")
    @GET
    @Path("/desk/template/list")
    fun getMyTemplates(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模版名称", required = false)
        @QueryParam("templateName")
        templateName: String?,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<MyTemplateItem>?>

    @ApiOperation("取消发布")
    @PUT
    @Path("/desk/template/release/cancel/templateIds/{templateId}")
    fun cancelRelease(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("templateId", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>

    @ApiOperation("下架模板")
    @PUT
    @Path("/desk/template/offline/templateCodes/{templateCode}/versions")
    fun offlineTemplate(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模版代码", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @ApiParam("版本号", required = false)
        @QueryParam("version")
        version: String?,
        @ApiParam("原因", required = false)
        @QueryParam("reason")
        reason: String?
    ): Result<Boolean>
}