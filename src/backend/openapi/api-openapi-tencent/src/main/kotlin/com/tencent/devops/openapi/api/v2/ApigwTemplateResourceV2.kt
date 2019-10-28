package com.tencent.devops.openapi.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_V2_TEMPLATE"], description = "OPEN-API-V2-项目模板资源")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwTemplateResourceV2 {

    @ApiOperation("获取所有种类流水线模板列表")
    @GET
    @Path("/organization")
    fun listTemplateByOrganization(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "组织类型", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_TYPE)
        organizationType: String,
        @ApiParam(value = "组织名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ORGANIZATION_NAME)
        organizationName: String,
        @ApiParam("部门名称", required = false, defaultValue = "")
        @QueryParam("deptName")
        deptName: String? = "",
        @ApiParam("中心名称", required = false, defaultValue = "")
        @QueryParam("centerName")
        centerName: String? = "",
        @ApiParam("模版类型", required = false)
        @QueryParam("templateType")
        templateType: TemplateType?,
        @ApiParam("是否已关联到store", required = false)
        @QueryParam("storeFlag")
        storeFlag: Boolean?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int? = null,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<TemplateModel>>
}