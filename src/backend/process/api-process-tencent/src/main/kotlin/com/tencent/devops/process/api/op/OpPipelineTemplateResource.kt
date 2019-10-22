package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.pojo.template.TemplateType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.QueryParam

@Api(tags = ["OP_PIPELINE_TEMPLATE"], description = "OP-流水线-模板资源")
@Path("/op/templates")
interface OpPipelineTemplateResource {

    @ApiOperation("添加流水线模板")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("author", required = true)
        @QueryParam("author")
        author: String,
        @ApiParam("name", required = true)
        @QueryParam("name")
        name: String,
        @ApiParam("type", required = true)
        @QueryParam("type")
        type: TemplateType,
        @ApiParam("category", required = true)
        @QueryParam("category")
        category: String,
        @ApiParam("icon", required = false)
        @QueryParam("icon")
        icon: String?,
        @ApiParam("logoUrl", required = false)
        @QueryParam("logoUrl")
        logoUrl: String?,
        @ApiParam("projectCode", required = true, defaultValue = "-1")
        @QueryParam("projectCode")
        projectCode: String,
        @ApiParam("流水线", required = true)
        model: Model
    ): Result<Boolean>

    @ApiOperation("迁移模版")
    @POST
    @Path("/migrateTemplate")
    fun migrateTemplate(): Result<Boolean>
}