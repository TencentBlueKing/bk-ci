package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.code.PipelineBuildCommit
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE"], description = "服务-流水线构建commit")
@Path("/service/pipeline/build/commit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineBuildCommitResource {

    @ApiOperation("保存流水线构建提交信息")
    @POST
    @Path("/save")
    fun save(commits: List<PipelineBuildCommit>): Result<Boolean>
}
