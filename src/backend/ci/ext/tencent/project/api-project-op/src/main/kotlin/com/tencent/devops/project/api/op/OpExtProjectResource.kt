package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.MigrateProjectInfo
import com.tencent.devops.project.pojo.ProjectUpdateCreatorDTO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_EXT_PROJECT"], description = "OP-内部-项目")
@Path("/op/ext/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpExtProjectResource {
    @GET
    @Path("/getMigrateProjectInfo")
    @ApiOperation("获取迁移项目信息")
    fun getMigrateProjectInfo(): Result<List<MigrateProjectInfo>>

    @POST
    @Path("/updateProjectCreator")
    @ApiOperation("修改项目创建人")
    fun updateProjectCreator(
        projectUpdateCreatorDtoList: List<ProjectUpdateCreatorDTO>
    ): Result<Boolean>
}
