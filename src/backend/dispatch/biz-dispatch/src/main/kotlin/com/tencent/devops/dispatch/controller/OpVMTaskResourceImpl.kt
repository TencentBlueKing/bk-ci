package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.OpVMTaskResource
import com.tencent.devops.dispatch.pojo.TaskCreate
import com.tencent.devops.dispatch.pojo.TaskDetail
import com.tencent.devops.dispatch.pojo.TaskWithPage
import com.tencent.devops.dispatch.service.VmTaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpVMTaskResourceImpl @Autowired constructor(private val vmTaskService: VmTaskService) : OpVMTaskResource {

    override fun addTask(task: TaskCreate): Result<Boolean> {
//        vmTaskService.addTask(task)
        return Result(true)
    }

    override fun listTasks(userid: String?, page: Int, pageSize: Int) = Result(TaskWithPage(0, emptyList()))

    override fun getTaskDetails(taskId: Int): Result<List<TaskDetail>> = Result(emptyList())
}