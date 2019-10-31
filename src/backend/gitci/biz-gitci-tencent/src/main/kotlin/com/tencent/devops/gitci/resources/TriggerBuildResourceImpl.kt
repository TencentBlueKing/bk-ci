package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.TriggerBuildResource
import com.tencent.devops.gitci.pojo.TriggerBuildReq
import com.tencent.devops.gitci.service.GitCIRequestService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class TriggerBuildResourceImpl @Autowired constructor(
    private val gitCIRequestService: GitCIRequestService
) : TriggerBuildResource {
    companion object {
        private val logger = LoggerFactory.getLogger(TriggerBuildResourceImpl::class.java)
    }

    override fun triggerStartup(userId: String, triggerBuildReq: TriggerBuildReq): Result<Boolean> {
        checkParam(userId)
        return Result(gitCIRequestService.triggerBuild(userId, triggerBuildReq))
    }

    override fun checkYaml(userId: String, yaml: String): Result<String> {
        checkParam(userId)
        try {
            gitCIRequestService.createCIBuildYaml(yaml)
        } catch (e: Throwable) {
            logger.error("check yaml failed, error: ${e.message}, yaml: $yaml")
            return Result(1, "Invalid", e.message)
        }

        return Result("OK")
    }

    override fun getYamlByBuildId(userId: String, gitProjectId: Long, buildId: String): Result<String> {
        checkParam(userId)
        return Result(gitCIRequestService.getYaml(gitProjectId, buildId))
    }

    private fun checkParam(userId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
    }
}
