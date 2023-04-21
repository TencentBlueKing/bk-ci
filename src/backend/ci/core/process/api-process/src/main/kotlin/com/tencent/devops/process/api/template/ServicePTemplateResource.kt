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

package com.tencent.devops.process.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.process.pojo.PipelineTemplateInfo
import com.tencent.devops.process.pojo.template.AddMarketTemplateRequest
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateDetailInfo
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_TEMPLATE"], description = "服务-模板资源")
@Path("/service/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePTemplateResource {

    @ApiOperation("添加模板市场模板")
    @POST
    @Path("/store/add")
    fun addMarketTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("安装模板请求报文体", required = true)
        addMarketTemplateRequest: AddMarketTemplateRequest
    ): Result<Map<String, String>>

    @ApiOperation("更新已安装的模版")
    @POST
    @Path("/store/update")
    fun updateMarketTemplateReference(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("安装模板请求报文体", required = true)
        updateMarketTemplateRequest: AddMarketTemplateRequest
    ): Result<Boolean>

    @ApiOperation("查询模板详情")
    @GET
    @Path("/store/templateCodes/{templateCode}")
    fun getTemplateDetailInfo(
        @ApiParam("模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<TemplateDetailInfo?>

    @ApiOperation("判断模板镜像是否发布")
    @GET
    @Path("/store/templates/{templateCode}/images/releaseStatus/check")
    fun checkImageReleaseStatus(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("模板代码", required = true)
        @PathParam("templateCode")
        templateCode: String
    ): Result<String?>

    @ApiOperation("查询项目下所有源模板的ID")
    @GET
    @Path("/store/projects/{projectId}/srcTemplates")
    fun getSrcTemplateCodes(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<String>>

    @ApiOperation("查询源模板ID绑定的模板")
    @GET
    @Path("/store/srcTemplates/{srcTemplateId}")
    fun getTemplateIdBySrcCode(
        @ApiParam("源模板Id", required = true)
        @PathParam("srcTemplateId")
        srcTemplateId: String,
        @ApiParam("项目列表", required = true)
        @QueryParam("projectIds")
        projectIds: List<String>
    ): Result<List<PipelineTemplateInfo>>

    @ApiOperation("更新模版是否已关联市场标识")
    @PUT
    @Path("/{templateId}/store/storeFlag")
    fun updateStoreFlag(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("是否已关联市场标识", required = true)
        storeFlag: Boolean
    ): Result<Boolean>

    @ApiOperation("创建流水线-获取模版列表")
    @GET
    @Path("/projects/{projectId}/allTemplates")
    fun listAllTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateType?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = false)
        pageSize: Int?
    ): Result<OptionalTemplateList>

    @ApiOperation("获取列表流水线模板")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}")
    fun getTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String,
        @ApiParam("模板版本", required = false)
        @QueryParam("version")
        version: Long?,
        @ApiParam("模板版本名称", required = false)
        @QueryParam("versionName")
        versionName: String?
    ): Result<TemplateModelDetail>

    @ApiOperation("模版管理-获取模版列表")
    @GET
    @Path("/projects/{projectId}/templates")
    fun listTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateType?,
        @ApiParam("是否已关联到store", required = false)
        @QueryParam("storeFlag")
        storeFlag: Boolean?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = false)
        pageSize: Int?
    ): Result<TemplateListModel>

    @ApiOperation("根据id获取模版列表")
    @POST
    @Path("/listTemplateById")
    fun listTemplateById(
        @ApiParam("模板ID", required = true)
        templateIds: Collection<String>,
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam("模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateType?
    ): Result<OptionalTemplateList>

    @ApiOperation("检查模板是否合法")
    @GET
    @Path("/projects/{projectId}/templates/{templateId}/check")
    fun checkTemplate(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "模版ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<Boolean>
}
