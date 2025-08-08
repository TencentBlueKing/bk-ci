package com.tencent.devops.artifactory.api.user

import com.tencent.devops.artifactory.pojo.PipelineOutput
import com.tencent.devops.artifactory.pojo.PipelineOutputSearchOption
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_OUTPUT", description = "流水线产出物管理")
@Path("/user/pipeline/output")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineOutputResource {

    @Operation(summary = "查询流水线单次构建产出物")
    @Path("/{projectId}/{pipelineId}/{buildId}/search")
    @POST
    fun searchByBuild(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "项目代码", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "搜索过滤条件", required = false)
        option: PipelineOutputSearchOption?,
        @Parameter(description = "是否查询归档数据", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false
    ): Result<List<PipelineOutput>>
}
