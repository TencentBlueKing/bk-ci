package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.UserDockerConfigResource
import com.tencent.devops.environment.pojo.BatchDockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigRequest
import com.tencent.devops.environment.pojo.DockerConfigScope
import com.tencent.devops.environment.service.DockerConfigService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserDockerConfigResourceImpl @Autowired constructor(
    private val service: DockerConfigService
) : UserDockerConfigResource {
    override fun get(
        userId: String, projectId: String, scope: DockerConfigScope, resourceId: String
    ): Result<AgentDockerConfig> = Result(service.get(userId, projectId, scope, HashUtil.decodeIdToLong(resourceId)))

    override fun save(
        userId: String, projectId: String, scope: DockerConfigScope, resourceId: String, request: DockerConfigRequest
    ): Result<Boolean> {
        service.save(userId, projectId, scope, HashUtil.decodeIdToLong(resourceId), request)
        return Result(true)
    }

    override fun batch(userId: String, projectId: String, request: BatchDockerConfigRequest): Result<Int> =
        Result(service.batch(userId, projectId, request))
}
