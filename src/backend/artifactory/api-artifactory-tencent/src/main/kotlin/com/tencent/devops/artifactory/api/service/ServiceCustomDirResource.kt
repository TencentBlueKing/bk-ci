package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.common.api.pojo.Result
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

@Api(tags = ["SERVICE_CUSTOM_DIR"], description = "版本仓库-仓库资源")
@Path("/service/customDir")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCustomDirResource {
    @ApiOperation("获取安全加固下载链接")
    //@Path("/projects/{projectId}/files/{fileName: .*}")
    @Path("/{projectId}/{fileName: .*}")
    @GET
    fun getGsDownloadUrl(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("文件名", required = true)
        @PathParam("fileName")
        fileName: String,
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<Url>
}