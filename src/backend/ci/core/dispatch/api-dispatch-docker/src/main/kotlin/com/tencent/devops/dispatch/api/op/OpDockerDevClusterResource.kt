package com.tencent.devops.dispatch.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.DockerDevCluster
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

@Api(tags = ["OP_DEV_CLUSTER"], description = "OP-IDC构建机集群管理接口")
@Path("/op/dockerDevCluster")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpDockerDevClusterResource {

    @GET
    @ApiOperation("获取Docker构建机集群列表")
    fun listDockerCluster(
        @ApiParam(
            "用户ID", required = true,
            defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "10")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("是否包括停用", required = false)
        @QueryParam("includeDisable")
        includeDisable: Boolean?
    ): Result<Page<DockerDevCluster>>

    @POST
    @ApiOperation("新增构建机集群")
    fun createDockerCluster(
        @ApiParam(
            "用户ID", required = true,
            defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("集群信息", required = true)
        dockerDevCluster: DockerDevCluster
    ): Result<Boolean>

    @PUT
    @ApiOperation("更新Docker构建机集群状态")
    fun updateDispatchDocker(
        @ApiParam(
            "用户ID", required = true,
            defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("集群信息", required = true)
        dockerDevCluster: DockerDevCluster
    ): Result<Boolean>


    @DELETE
    @Path("/{clusterId}")
    @ApiOperation("删除Docker构建机集群")
    fun deleteDispatchDocker(
        @ApiParam(
            "用户ID", required = true,
            defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("集群ID", required = true)
        @PathParam("clusterId")
        clusterId: String
    ): Result<Boolean>
}