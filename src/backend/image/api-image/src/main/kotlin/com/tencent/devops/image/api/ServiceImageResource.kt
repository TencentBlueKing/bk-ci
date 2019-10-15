package com.tencent.devops.image.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.image.pojo.DockerTag
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_IMAGE"], description = "镜像-镜像服务")
@Path("/service/image")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.MULTIPART_FORM_DATA)
interface ServiceImageResource {

    @ApiOperation("获取项目Docker构建镜像列表")
    @Path("/{projectId}/listDockerBuildImages")
    @GET
    fun listDockerBuildImages(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<DockerTag>>

    @ApiOperation("镜像仓库支持升级为构建镜像")
    @Path("/{projectId}/setBuildImage")
    @POST
    fun setBuildImage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "镜像repo", required = true)
        @QueryParam("imageRepo")
        imageRepo: String,
        @ApiParam(value = "镜像tag", required = true)
        @QueryParam("imageTag")
        imageTag: String
    ): Result<Boolean>
}