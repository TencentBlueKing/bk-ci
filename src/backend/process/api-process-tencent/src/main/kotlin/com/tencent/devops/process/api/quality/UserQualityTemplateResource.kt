package com.tencent.devops.process.api.quality

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * deng
 * 2019-01-08
 */
@Api(tags = ["USER_QUALITY_TEMPLATE"], description = "用户-质量红线-流水模板资源")
@Path("/user/quality/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityTemplateResource {

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
        pageSize: Int?,
        @ApiParam("模板名称关键字", required = false)
        @QueryParam("keywords")
        keywords: String?
    ): Result<TemplateListModel>

    @ApiOperation("获取单个流水线信息接口")
    @GET
    @Path("/project/{projectId}/template/{templateId}/getTemplateInfo")
    fun getTemplateInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("模板ID", required = true)
        @PathParam("templateId")
        templateId: String
    ): Result<TemplateModelDetail>
}