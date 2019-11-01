package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.dispatch.pojo.DockerHostZone
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_DOCKER_HOST"], description = "服务-获取构建容器信息")
@Path("/service/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceDockerHostResource {

    @ApiOperation("获取dockerhost列表")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Page<DockerHostZone>
}