package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.ServiceBuildResource
import com.tencent.devops.gitci.service.BuildService
import com.tencent.devops.process.pojo.BuildId
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceBuildResourceImpl @Autowired constructor(
    private val buildService: BuildService
) : ServiceBuildResource {

    override fun retry(userId: String, gitProjectId: Long, buildId: String, taskId: String?): Result<BuildId> {
        checkParam(userId, buildId)
        return Result(buildService.retry(userId, gitProjectId, buildId, taskId))
    }

    override fun manualShutdown(userId: String, gitProjectId: Long, buildId: String): Result<Boolean> {
        checkParam(userId, buildId)
        return Result(buildService.manualShutdown(userId, gitProjectId, buildId))
    }

    private fun checkParam(userId: String, buildId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }
    }
}
