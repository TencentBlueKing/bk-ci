package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_REGION
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
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

@Api(tags = ["SERVICE_ARTIFACTORY"], description = "版本仓库-仓库资源")
@Path("/service/artifactories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceArtifactoryDownLoadResource {

    @ApiOperation("获取文件第三方下载链接")
    @Path("/thirdPartyDownloadUrl")
    @GET
    fun getThirdPartyDownloadUrl(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @ApiParam("版本仓库类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("有效时间(s)", required = true)
        @QueryParam("ttl")
        ttl: Int?,
        @ApiParam("项目ID", required = false)
        @QueryParam("projectId")
        crossProjectId: String?,
        @ApiParam("流水线ID", required = false)
        @QueryParam("pipelineId")
        crossPipineId: String?,
        @ApiParam("构建No", required = false)
        @QueryParam("buildNo")
        crossBuildNo: String?,
        @ApiParam("客户端区域", required = false)
        @HeaderParam(AUTH_HEADER_REGION)
        region: String?
    ): Result<List<String>>

    @ApiOperation("创建下载链接")
    // @Path("/projects/{projectId}/artifactoryTypes/{artifactoryType}/downloadUrl")
    @Path("/{projectId}/{artifactoryType}/downloadUrl")
    @POST
    fun downloadUrl(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("版本仓库类型", required = true)
        @PathParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @ApiParam("用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("渠道", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Url>
}