package com.tencent.devops.gitci.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.gitci.pojo.GitProjectConfWithPage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.DELETE
import javax.ws.rs.QueryParam
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.core.MediaType

@Api(tags = arrayOf("OP_GIT_PROJECT"), description = "git项目管理")
@Path("/op/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpGitProjectResource {

    @ApiOperation("添加git项目")
    @PUT
    @Path("/create")
    fun create(
        @ApiParam(value = "工蜂项目ID", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "工蜂项目名称", required = true)
        @QueryParam("name")
        name: String,
        @ApiParam(value = "工蜂项目URL", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam(value = "是否可以启用工蜂CI", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @ApiOperation("修改git项目")
    @POST
    @Path("/update")
    fun update(
        @ApiParam(value = "工蜂项目ID", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "工蜂项目名称", required = false)
        @QueryParam("name")
        name: String?,
        @ApiParam(value = "工蜂项目URL", required = false)
        @QueryParam("url")
        url: String?,
        @ApiParam(value = "是否可以启用工蜂CI", required = false)
        @QueryParam("enable")
        enable: Boolean?
    ): Result<Boolean>

    @ApiOperation("删除git项目")
    @DELETE
    @Path("/delete")
    fun delete(
        @ApiParam(value = "工蜂项目ID", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long
    ): Result<Boolean>

    @ApiOperation("列出git项目")
    @GET
    @Path("/project/list")
    fun list(
        @ApiParam(value = "工蜂项目ID", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long?,
        @ApiParam(value = "工蜂项目名称", required = false)
        @QueryParam("name")
        name: String?,
        @ApiParam(value = "工蜂项目URL", required = false)
        @QueryParam("url")
        url: String?,
        @ApiParam(value = "第几页，从1开始", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<GitProjectConfWithPage>
}