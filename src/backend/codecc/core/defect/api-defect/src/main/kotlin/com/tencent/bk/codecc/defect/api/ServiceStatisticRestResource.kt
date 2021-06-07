package com.tencent.bk.codecc.defect.api

import com.tencent.bk.codecc.task.vo.GrayTaskStatVO
import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_CLUSTER_STATISTIC"], description = "聚类统计接口")
@Path("/service/statistic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface(value = "defect")
interface ServiceStatisticRestResource {

    @ApiOperation("获取lint类工具统计信息")
    @Path("/taskId/{taskId}/toolName/{toolName}/buildId/{buildId}")
    @GET
    fun getLintStatInfo(
        @ApiParam(value = "任务id", required = true)
        @PathParam("taskId")
        taskId: Long,
        @ApiParam(value = "工具名", required = true)
        @PathParam("toolName")
        toolName: String,
        @ApiParam(value = "构建id", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<GrayTaskStatVO?>
}
