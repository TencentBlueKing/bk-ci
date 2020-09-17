package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_AUTH"], description = "服务-代码库-权限中心")
@Path("/service/auth/repository/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthRepositoryResource {

    @ApiOperation("获取项目代码库列表")
    @GET
    @Path("/projects/{projectIds}/listByProjects")
    fun listByProjects(
        @ApiParam("项目Id", required = false)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int?,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<Page<RepositoryInfo>>

    @ApiOperation("获取项目代码库列表")
    @GET
    @Path("/infos")
    fun getInfos(
        @ApiParam("代码库Id串", required = true)
        @QueryParam("repositoryIds")
        repositoryIds: List<String>
    ): Result<List<RepositoryInfo>?>
}