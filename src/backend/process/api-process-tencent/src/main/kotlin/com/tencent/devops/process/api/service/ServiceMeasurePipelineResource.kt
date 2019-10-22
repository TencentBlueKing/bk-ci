package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.Pipeline
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MEASURE_PIPELINE"], description = "服务-流水线资源")
@Path("/service/measures")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMeasurePipelineResource {

    @ApiOperation("获取所有流水线")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: Set<String>,
        @ApiParam("渠道号，默认为DS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<List<Pipeline>>

    @ApiOperation("获取使用原子的流水线个数")
    @GET
    @Path("/atom/{atomCode}/count")
    fun getPipelineCountByAtomCode(
        @ApiParam("原子标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @ApiParam("项目标识", required = false)
        @QueryParam("projectCode")
        projectCode: String?
    ): Result<Int>

    @ApiOperation("获取使用原子的流水线个数")
    @GET
    @Path("/atomCount")
    fun batchGetPipelineCountByAtomCode(
        @ApiParam("原子标识", required = false)
        @QueryParam("atomCodes")
        atomCodes: String,
        @ApiParam("项目标识", required = false)
        @QueryParam("projectCode")
        projectCode: String?
    ): Result<Map<String, Int>>
}