package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_REPOSITORY"], description = "构建-构建物料")
@Path("/build/repository")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildRepositoryResource {
    @ApiOperation("获取指定构建或指定流水线下的构建变量")
    @Path("/saveBuildMaterial")
    @POST
    fun saveBuildMaterial(
        @ApiParam(value = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @ApiParam(value = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PIPELINE_ID)
        pipelineId: String,
        @ApiParam(value = "物料列表", required = true)
        pipelineBuildMaterials: List<PipelineBuildMaterial>
    ): Result<Int>
}