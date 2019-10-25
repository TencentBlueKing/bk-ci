package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.wetest.WetestEmailGroup
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_WETEST_EAMIL_GROUP"], description = "服务-WETEST邮件组")
@Path("/service/wetest/emailgroup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceWetestEmailGroupResource {

    @ApiOperation("获取单个WETEST邮件组")
    @POST
    @Path("/{projectId}/get")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("ID", required = true)
        @QueryParam("ID")
        id: Int
    ): Result<WetestEmailGroup?>
}