package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.RepositoryScmConfigSummary
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PAC_REPOSITORY", description = "服务-PAC-代码库")
@Path("/service/repositories/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRepositoryConfigResource {
    @Operation(summary = "获取代码库配置简要信息")
    @POST
    @Path("/{scmCode}/summary")
    fun getSummary(
        @Parameter(description = "项目ID", required = true)
        @PathParam("scmCode")
        scmCode: String
    ): Result<RepositoryScmConfigSummary>
}
