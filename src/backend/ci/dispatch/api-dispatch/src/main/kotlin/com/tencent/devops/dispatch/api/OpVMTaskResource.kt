/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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