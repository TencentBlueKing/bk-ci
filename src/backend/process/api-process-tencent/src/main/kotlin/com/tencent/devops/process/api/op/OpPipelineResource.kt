package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.process.pojo.DockerEnableProject
import com.tencent.devops.process.pojo.Pipeline
import com.tencent.devops.process.pojo.PipelineSortType
import com.tencent.devops.process.pojo.app.PipelinePage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PIPELINE"], description = "OP-流水线")
@Path("/op/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineResource {

    @ApiOperation("根据项目id获取所有的流水线")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("项目ID", required = false, defaultValue = "")
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("流水线排序", required = false, defaultValue = "CREATE_TIME")
        @QueryParam("sortType")
        sortType: PipelineSortType? = PipelineSortType.CREATE_TIME
    ): Result<PipelinePage<Pipeline>>

    @ApiOperation("项目启动docker构建方式")
    @PUT
    @Path("/dockers/enable")
    fun enableDocker(
        @ApiParam("项目ID", required = true, defaultValue = "")
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("是否启动", required = true, defaultValue = "true")
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @ApiOperation("获取所有灰度docker构建的项目")
    @GET
    @Path("/dockers")
    fun getAllEnableDockerProjects(): Result<List<DockerEnableProject>>
}