package com.tencent.devops.stream.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.hibernate.validator.constraints.Range
import javax.validation.Valid
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_STREAM_SERVICES"], description = "stream repository conf管理")
@Path("/op/repository/conf")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpRepositoryConfResource {
    @ApiOperation("修改工蜂老域名")
    @POST
    @Path("/updateGitDomain")
    fun updateRepoConfGitDomain(
        @ApiParam(value = "git老域名", required = true)
        @QueryParam("oldGitDomain")
        oldGitDomain: String,
        @ApiParam(value = "git新域名", required = true)
        @QueryParam("newGitDomain")
        newGitDomain: String,
        @ApiParam(value = "更新的数量", required = true)
        @QueryParam("limitNumber")
        @Range(min = 1, max = 1000, message = "修改的数量不能小于1、大于1000")
        @Valid
        limitNumber: Int
    ): Result<Boolean>
}
