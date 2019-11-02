package com.tencent.devops.store.api.image.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.image.pojo.DockerRepo
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

@Api(tags = ["USER_MARKET_IMAGE_REPO"], description = "镜像-镜像仓库")
@Path("/user/market/image/repo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImageRepoResource {

    @ApiOperation("查找蓝盾仓库关联镜像信息")
    @GET
    @Path("/bk/names/{imageRepoName}")
    fun getBkRelImageInfo(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("镜像在仓库中的名称", required = true)
        @PathParam("imageRepoName")
        imageRepoName: String,
        @ApiParam("需要回显镜像tag的镜像ID", required = false)
        @QueryParam("imageId")
        imageId: String?
    ): Result<DockerRepo?>
}