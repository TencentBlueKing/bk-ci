package com.tencent.devops.environment.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.EnvWithPermission
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_ENVIRONMENT_AUTH"], description = "服务-环境服务-权限中心")
@Path("/service/environment/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface RemoteEnvResource {

    @ApiOperation("分页获取节点列表")
    @GET
    @Path("/projects/{projectId}/list/page")
    fun listEnvByPage(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int? = null,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null
    ): Result<Page<EnvWithPermission>>

    @ApiOperation("获取环境信息")
    @GET
    @Path("/infos")
    fun getEnvInfos(
        @ApiParam("节点Id串", required = true)
        @QueryParam("envIds")
        envIds: List<String>
    ): Result<List<EnvWithPermission>>
}