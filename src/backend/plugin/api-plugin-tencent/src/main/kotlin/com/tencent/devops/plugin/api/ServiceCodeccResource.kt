package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.codecc.BlueShieldResponse
import com.tencent.devops.plugin.pojo.codecc.CodeccBuildInfo
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/**
 * Created by ddlin on 2018/07/26.
 */
@Api(tags = ["SERVICE_CODECC"], description = "服务-创建异步任务")
@Path("/service/codecc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCodeccResource {

    @ApiOperation("提供根据流水线构建id查询构建号（页面上展示用的）、构建时间、构建人等信息件")
    @POST
    @Path("/codeccBuildInfo")
    fun getCodeccBuildInfo(
        @ApiParam(value = "构建id集合", required = true)
        buildIds: Set<String>
    ): Result<Map<String, CodeccBuildInfo>>

    @ApiOperation("根据项目id获取codecc任务信息")
    @POST
    @Path("/task/byProject")
    fun getCodeccTaskByProject(
        @QueryParam("开始时间")
        beginDate: Long?,
        @QueryParam("结束时间")
        endDate: Long?,
        projectIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>>

    @ApiOperation("根据流水线id获取codecc任务信息")
    @POST
    @Path("/task/byPipeline")
    fun getCodeccTaskByPipeline(
        @QueryParam("开始时间")
        beginDate: Long?,
        @QueryParam("结束时间")
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, BlueShieldResponse.Item>>

    @ApiOperation("根据流水线id获取codecc任务结果")
    @POST
    @Path("/task/result")
    fun getCodeccTaskResult(
        @QueryParam("开始时间")
        beginDate: Long?,
        @QueryParam("结束时间")
        endDate: Long?,
        pipelineIds: Set<String>
    ): Result<Map<String, CodeccCallback>>

    @ApiOperation("根据构建ID获取CodeCC任务结果")
    @POST
    @Path("/task/result/builds")
    fun getCodeccTaskResult(
        buildIds: Set<String>
    ): Result<Map<String, CodeccCallback>>
}