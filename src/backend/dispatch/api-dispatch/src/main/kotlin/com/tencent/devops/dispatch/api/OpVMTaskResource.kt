package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.TaskCreate
import com.tencent.devops.dispatch.pojo.TaskDetail
import com.tencent.devops.dispatch.pojo.TaskWithPage
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

@Api(tags = arrayOf("OP_VM_TASK"), description = "VM TASK任务管理")
@Path("/op/vmtask")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpVMTaskResource {

    @ApiOperation("添加vm任务")
    @POST
    @Path("/add")
    fun addTask(
        @ApiParam(value = "vm task 信息", required = true)
        task: TaskCreate
    ): Result<Boolean>

    @ApiOperation("列出对应用户所有的vm任务（按开始时间倒序排序）")
    @GET
    @Path("/list")
    fun listTasks(
        @ApiParam(value = "用户id", required = false)
        @QueryParam("userid")
        userid: String?,
        @ApiParam(value = "第几页", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<TaskWithPage>

    @ApiOperation("查看vm任务详情（按时间顺序排序）")
    @GET
    @Path("/listDetails/{taskId}")
    fun getTaskDetails(
        @ApiParam(value = "task id", required = true)
        @PathParam("taskId")
        taskId: Int
    ): Result<List<TaskDetail>>
}