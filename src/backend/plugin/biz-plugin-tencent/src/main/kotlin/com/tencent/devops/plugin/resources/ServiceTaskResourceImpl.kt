package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.ServiceTaskResource
import com.tencent.devops.plugin.pojo.TaskData
import com.tencent.devops.plugin.service.TaskService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTaskResourceImpl @Autowired constructor(
    private val taskService: TaskService
) : ServiceTaskResource {
    override fun create(taskData: TaskData): Result<Boolean> {
        taskService.create(taskData)
        return Result(true)
    }
}