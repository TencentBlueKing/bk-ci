package com.tencent.devops.stream.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.hibernate.validator.constraints.Range
import javax.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_STREAM_SERVICES", description = "stream repository conf管理")
@Path("/op/repository/conf")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpRepositoryConfResource {
    @Operation(summary = "修改工蜂老域名")
    @POST
    @Path("/updateGitDomain")
    fun updateRepoConfGitDomain(
        @Parameter(description = "git老域名", required = true)
        @QueryParam("oldGitDomain")
        oldGitDomain: String,
        @Parameter(description = "git新域名", required = true)
        @QueryParam("newGitDomain")
        newGitDomain: String,
        @Parameter(description = "更新的数量", required = true)
        @QueryParam("limitNumber")
        @Range(min = 1, max = 1000, message = "修改的数量不能小于1、大于1000")
        @Valid
        limitNumber: Int
    ): Result<Boolean>
}
