package com.tencent.devops.turbo.service

import com.tencent.devops.auth.api.service.ServiceManagerResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Service
class TurboAuthService @Autowired constructor(
    private val serviceProjectAuthResource: ServiceProjectAuthResource,
    private val serviceManagerResource: ServiceManagerResource
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TurboAuthService::class.java)
    }

    @Value("\${auth.token}")
    private val token: String? = null

    /**
     * 获取鉴权结果
     */
    fun getAuthResult(projectId: String, userId: String): Boolean {
        return validateProjectMember(projectId, userId) || validatePlatformMember(projectId, userId)
    }

    /**
     * 校验是否是项目成员
     */
    private fun validateProjectMember(projectId: String, userId: String): Boolean {
        logger.info("project id: $projectId, user id: $userId, token : $token")
        val projectValidateResult = try {
            serviceProjectAuthResource.isProjectUser(
                token = token!!,
                userId = userId,
                projectCode = projectId
            ).data ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            logger.info("validate project member fail! error message : ${e.message}")
            false
        }
        logger.info("project validate result: $projectValidateResult")
        return projectValidateResult
    }

    /**
     * 校验是否是平台管理员
     */
    fun validatePlatformMember(projectId: String, userId: String): Boolean {
        val adminValidateResult = try {
            serviceManagerResource.validateManagerPermission(
                userId = userId,
                token = token!!,
                projectCode = projectId,
                resourceCode = "TURBO",
                action = "VIEW"
            ).data ?: false
        } catch (e: Exception) {
            logger.info("validate admin member fail! error message : ${e.message}")
            false
        }
        logger.info("admin validate result: $adminValidateResult")
        return adminValidateResult
    }
}
