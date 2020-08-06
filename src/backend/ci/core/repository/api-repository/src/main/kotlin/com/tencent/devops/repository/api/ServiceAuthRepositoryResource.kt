package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
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
    @POST
    @Path("/listByProjects")
    fun listByProjects(
        projectIds: Set<String>,
        @ApiParam("分页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("分页大小", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<RepositoryInfo>>
}