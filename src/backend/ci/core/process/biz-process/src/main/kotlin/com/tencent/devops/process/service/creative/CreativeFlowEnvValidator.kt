package com.tencent.devops.process.service.creative

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.process.constant.ProcessMessageCode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreativeFlowEnvValidator @Autowired constructor(
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CreativeFlowEnvValidator::class.java)
    }

    fun getEnvOsType(userId: String, projectId: String, envHashId: String): String? {
        return try {
            val env = client.get(ServiceEnvironmentResource::class)
                .get(userId = userId, projectId = projectId, envHashId = envHashId, checkPermission = false)
                .data
            env?.os?.name
        } catch (e: Exception) {
            logger.warn("CreativeFlowEnvValidator|getEnvOsType|failed: $projectId/$envHashId", e)
            null
        }
    }

    fun validate(
        userId: String,
        targetProjectId: String,
        targetEnvHashId: String,
        rules: com.tencent.devops.process.pojo.creative.CreativeFlowShareValidateRules?
    ) {
        val targetEnv = try {
            client.get(ServiceEnvironmentResource::class)
                .get(userId = userId, projectId = targetProjectId, envHashId = targetEnvHashId, checkPermission = false)
                .data
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_TARGET_ENV_INVALID,
                params = arrayOf(targetEnvHashId)
            )
        } ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_TARGET_ENV_INVALID,
            params = arrayOf(targetEnvHashId)
        )

        val requiredOs = rules?.envOsType
        if (!requiredOs.isNullOrBlank()) {
            val targetOs = targetEnv.os?.name
            if (targetOs != null && !requiredOs.equals(targetOs, ignoreCase = true)) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_CREATIVE_FLOW_COPY_ENV_OS_NOT_MATCH,
                    params = arrayOf(requiredOs, targetOs)
                )
            }
        }
    }
}
