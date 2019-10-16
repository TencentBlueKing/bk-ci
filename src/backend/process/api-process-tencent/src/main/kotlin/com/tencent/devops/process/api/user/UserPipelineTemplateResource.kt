package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineTemplate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PIPELINE_TEMPLATE"], description = "用户-流水线-模板资源")
@Path("/user/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineTemplateResource {

    @ApiOperation("列举流水线模板")
    @GET
    @Path("/projects/{projectCode}")
    fun listTemplate(
        @ApiParam("项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Map<String, PipelineTemplate>>
}