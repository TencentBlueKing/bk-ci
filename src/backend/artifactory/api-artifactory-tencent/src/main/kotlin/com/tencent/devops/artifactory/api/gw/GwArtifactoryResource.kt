package com.tencent.devops.artifactory.api.gw

import com.tencent.devops.artifactory.pojo.DownloadUrl
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["GW_ARTIFACTORY"], description = "版本仓库-仓库资源")
@Path("/gw/artifactories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface GwArtifactoryResource {
    @ApiOperation("是否有下载权限")
    //@Path("/projects/{projectId}/services/{serviceCode}/resources/{resourceType}/hasDownloadPermission")
    @Path("/{projectId}/{serviceCode}/{resourceType}/hasDownloadPermission")
    @GET
    fun hasDownloadPermission(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("resourceType")
        resourceType: String,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String
    ): Result<Boolean>

    @ApiOperation("获取体验列表")
    @Path("/downloadUrl")
    @GET
    fun getDownloadUrl(
        @ApiParam("token令牌", required = true)
        @QueryParam("token")
        token: String
    ): Result<DownloadUrl>
}