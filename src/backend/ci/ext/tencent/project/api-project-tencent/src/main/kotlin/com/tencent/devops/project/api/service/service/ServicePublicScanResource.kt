package com.tencent.devops.project.api.service.service

import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectVO
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PUBLICSCAN"], description = "开源代码扫描项目接口")
@Path("/service/publicScan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePublicScanResource {

    @POST
    @Path("/project/{userId}")
    @ApiOperation("为开源代码扫描创建项目")
    fun createCodeCCScanProject(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam(value = "项目创建信息", required = true)
        projectCreateInfo: ProjectCreateInfo
    ): Result<ProjectVO>
}