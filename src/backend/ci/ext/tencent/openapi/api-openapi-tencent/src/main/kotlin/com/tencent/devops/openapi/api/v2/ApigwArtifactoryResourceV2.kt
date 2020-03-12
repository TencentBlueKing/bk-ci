package com.tencent.devops.openapi.api.v2

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_V2_ARTIFACTORY"], description = "OPEN-API-V2-ARTIFACTORY")
@Path("/{apigw:apigw-user|apigw-app|apigw}/v2/artifactory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwArtifactoryResourceV2 {
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
        crossBuildNo: String?
    ): Result<List<String>>
}