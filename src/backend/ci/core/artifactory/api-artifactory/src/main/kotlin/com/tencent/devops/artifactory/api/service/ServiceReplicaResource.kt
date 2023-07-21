package com.tencent.devops.artifactory.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_ARTIFACTORY_REPLICA"], description = "仓库-制品同步管理")
@Path("/service/artifactories/replica")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceReplicaResource {

    @ApiOperation("创建边缘节点主动拉取的同步任务")
    @POST
    @Path("/create/{projectId}/{repoName}")
    fun createReplicaTask(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目 ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("仓库名", required = true)
        @PathParam("repoName")
        repoName: String,
        @ApiParam("文件路径", required = true)
        @QueryParam("fullPath")
        fullPath: String
    ): Result<Boolean>
}
