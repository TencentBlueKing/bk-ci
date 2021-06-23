package com.tencent.devops.auth.service.iam.impl

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.AuthUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

open class AbsPermissionService @Autowired constructor(
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val iamConfiguration: IamConfiguration
) : PermissionService {

    private val projectManager = CacheBuilder.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, List<String>>()

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        logger.info("[iam V3] validateUserActionPermission $userId $action")
        return authHelper.isAllowed(userId, action)
    }

    override fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean {
        return validateUserResourcePermissionByRelation(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            relationResourceType = null
        )
    }

    override fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean {
        logger.info("[iam V3]validateUserResourcePermissionByRelation: $userId $action $projectCode " +
            "$resourceCode $resourceType $relationResourceType")

        if (checkProjectManager(userId, projectCode)) {
            return true
        }

        val instanceDTO = InstanceDTO()
        instanceDTO.system = iamConfiguration.systemId
        // 若不关注操作资源实例，则必须关注是否在项目下
        if (resourceCode == "*") {
            instanceDTO.id = projectCode
            instanceDTO.type = AuthResourceType.PROJECT.value
        } else {
            instanceDTO.id = resourceCode
            instanceDTO.type = resourceType

            // 因除项目外的所有资源都需关联项目, 需要拼接策略path供sdk计算
            val path = PathInfoDTO()
            path.type = AuthResourceType.PROJECT.value
            path.id = projectCode
            instanceDTO.path = path
        }
        // 有可能出现提供的resourceCode是关联项目资源的code,需将type类型调整为对应的关联资源。
        if (relationResourceType != null) {
            instanceDTO.type = relationResourceType
        }

        logger.info("[iam V3] validateUserResourcePermission instanceDTO[$instanceDTO]")
        return authHelper.isAllowed(userId, action, instanceDTO)
    }

    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        try {
            logger.info("[iam V3] getUserResourceByPermission $userId $action $projectCode $resourceType")
            // 管理员直接返回“*”
            if (checkProjectManager(userId, projectCode)) {
                return arrayListOf("*")
            }
            val actionDto = ActionDTO()
            actionDto.id = action
            val expression = (policyService.getPolicyByAction(userId, actionDto, null) ?: return emptyList())
            logger.info("[iam V3] getUserResourceByPermission action: $actionDto, expression:$expression")

            if (expression.operator == null && expression.content == null) {
                return emptyList()
            }

            // 管理员权限
            if (expression.operator == ExpressionOperationEnum.ANY) {
                return listOf("*")
            }

            return if (resourceType == AuthResourceType.PROJECT.value) {
                AuthUtils.getProjects(expression)
            } else {
                val instancesList = AuthUtils.getResourceInstance(expression, projectCode, resourceType)
                if (!instancesList.contains("*")) {
                    instancesList.toList()
                } else {
                    listOf("*")
                }
            }
        } catch (e: Exception) {
            logger.warn("getUserResourceByAction fail {}", e)
        }
        return emptyList()
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        logger.info("[iam V3] getUserResourcesByActions $userId $actions $projectCode $resourceType")
        val result = mutableMapOf<AuthPermission, List<String>>()
        actions.forEach {
            val actionResourceList = getUserResourceByAction(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceType = resourceType
            )
            val authPermission = it.substringAfterLast("_")
            result[AuthPermission.get(authPermission)] = actionResourceList
        }
        return result
    }

    // 通过all_action 判断是否为项目管理员, 优先查缓存, 缓存时效10分钟
    private fun checkProjectManager(userId: String, projectCode: String): Boolean {
        if (projectManager.getIfPresent(userId) != null) {
            if (projectManager.getIfPresent(userId)!!.contains(projectCode)) {
                return true
            }
        }

        val managerAction = "all_action"
        val managerActionDto = ActionDTO()
        managerActionDto.id = managerAction
        val actionPolicyDTO = policyService.getPolicyByAction(userId, managerActionDto, null) ?: return false
        logger.info("[IAM] getUserProjects actionPolicyDTO $actionPolicyDTO")
        val projectCodes = AuthUtils.getProjects(actionPolicyDTO)
        projectManager.put(userId, projectCodes)
        return projectCodes.contains(projectCode)
    }

    companion object {
        val logger = LoggerFactory.getLogger(AbsPermissionService::class.java)
    }
}
