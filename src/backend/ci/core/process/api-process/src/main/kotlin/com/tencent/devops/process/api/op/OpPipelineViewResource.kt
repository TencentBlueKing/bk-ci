package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_VIEW", description = "用户-流水线视图")
@Path("/op/pipelineViews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineViewResource {
    @Operation(summary = "初始化所有动态视图")
    @GET
    @Path("/initAllView")
    fun initAllView(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Boolean>

    @Operation(summary = "删除yaml流水线组")
    @DELETE
    @Path("{projectId}/{repoHashId}/deleteYamlView")
    fun deleteYamlView(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库hashId", required = true)
        @PathParam("repoHashId")
        repoHashId: String,
        @Parameter(description = "yaml文件目录", required = true)
        @QueryParam("directory")
        directory: String
    ): Result<Boolean>
}
