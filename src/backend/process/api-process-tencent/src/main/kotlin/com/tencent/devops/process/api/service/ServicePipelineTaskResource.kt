package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.PipelineProjectRel
import com.tencent.devops.process.pojo.PipelineModelTask
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE"], description = "服务-流水线-任务资源")
@Path("/service/pipelineTasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePipelineTaskResource {

    @ApiOperation("获取流水线所有原子")
    @POST
    @Path("/projects/{projectId}/list")
    fun list(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线id集合", required = true)
        pipelineIds: Collection<String>
    ): Result<Map<String, List<PipelineModelTask>>>

    @ApiOperation("获取使用指定插件的流水线")
    @GET
    @Path("/atoms/{atomCode}")
    fun listByAtomCode(
        @ApiParam("插件标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("项目标识", required = false)
        @QueryParam("projectCode")
        projectCode: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineProjectRel>>
}