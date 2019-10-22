package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.op.TemplateData
import com.tencent.devops.quality.api.v2.pojo.op.TemplateUpdateData
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

@Api(tags = ["OP_TEMPLATE"], description = "质量红线-(模板/指标集)配置")
@Path("/op/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPTemplateResource {

    @ApiOperation("获取质量红线(模板/指标集)配置列表")
    @Path("/list")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页号", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("页码", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<TemplateData>>

    @ApiOperation("新增质量红线(模板/指标集)配置")
    @Path("/add")
    @POST
    fun add(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("(模板/指标集)配置信息", required = true)
        templateUpdateData: TemplateUpdateData
    ): Result<Boolean>

    @ApiOperation("删除质量红线(模板/指标集)配置置")
    @Path("/{id}/delete")
    @DELETE
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("(模板/指标集)配置ID", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>

    @ApiOperation("修改(模板/指标集)配置")
    @Path("/{id}/update")
    @PUT
    fun update(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("质量红线指标配置ID", required = true)
        @PathParam("id")
        id: Long,
        @ApiParam("(模板/指标集)配置信息", required = true)
        templateUpdateData: TemplateUpdateData
    ): Result<Boolean>
}