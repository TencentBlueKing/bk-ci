package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.code.PipelineBuildCommit
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PIPELINE", description = "服务-流水线构建commit")
@Path("/service/pipeline/build/commit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineBuildCommitResource {

    @Operation(summary = "保存流水线构建提交信息")
    @POST
    @Path("/save")
    fun save(commits: List<PipelineBuildCommit>): Result<Boolean>
}
