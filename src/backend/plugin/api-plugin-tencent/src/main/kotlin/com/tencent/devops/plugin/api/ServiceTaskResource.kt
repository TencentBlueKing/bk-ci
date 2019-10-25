package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.TaskData
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Created by Aaron Sheng on 2018/4/26.
 */
@Api(tags = ["SERVICE_TASK"], description = "服务-创建异步任务")
@Path("/service/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTaskResource {
    @ApiOperation("生成任务")
    @POST
    @Path("/create")
    fun create(
        @ApiParam("任务数据", required = true)
        taskData: TaskData
    ): Result<Boolean>
}