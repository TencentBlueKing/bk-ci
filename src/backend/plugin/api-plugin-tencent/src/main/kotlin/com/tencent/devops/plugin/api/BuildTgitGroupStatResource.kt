package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.api.pojo.GitGroupStatRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_TGIT_GROUP_STAT_PLUGIN"], description = "工蜂项目组统计信息")
@Path("/build/tgitGroupStat/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildTgitGroupStatResource {

    @ApiOperation("上报工蜂项目组统计数据")
    @POST
    @Path("/groups/{group}")
    fun reportGitGroupStat(
        @ApiParam("工蜂项目组", required = true)
        @PathParam("group")
        group: String,
        @ApiParam("统计数据", required = true)
        gitGroupStatRequest: GitGroupStatRequest
    ): Result<Boolean>
}