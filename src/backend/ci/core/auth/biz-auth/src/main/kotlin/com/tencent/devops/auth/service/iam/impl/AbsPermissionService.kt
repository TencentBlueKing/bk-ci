package com.tencent.devops.auth.service.iam.impl

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.InstanceDTO
import com.tencent.bk.sdk.iam.dto.PathInfoDTO
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.service.iam.IamCacheService
import com.tencent.devops.auth.service.iam.PermissionService
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.utils.AuthUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

open class AbsPermissionService @Autowired constructor(
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val iamConfiguration: IamConfiguration,
    private val iamCacheService: IamCacheService
) : PermissionService {

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
        logger.info("[iam V3] validateUserActionPermission :  userId = $userId | action = $action")
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
        logger.info(
            "[iam V3]validateUserResourcePermissionByRelation : user = $userId | action = $action " +
                "| projectCode = $projectCode | resourceCode = $resourceCode |" +
                " resourceType = $resourceType | relationResourceType = $relationResourceType"
        )

        if (iamCacheService.checkProjectManager(userId, projectCode)) {
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

        logger.info("[iam V3] validateUserResourcePermission : instanceDTO = $instanceDTO")
        return authHelper.isAllowed(userId, action, instanceDTO)
    }

    override fun batchValidateUserResourcePermission(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceCode = resourceCode,
                resourceType = resourceType,
                relationResourceType = null
            )
        }
    }

    override fun batchValidateUserResourcePermissionByInstance(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resource: AuthResourceInstance
    ): Map<String, Boolean> {
        return actions.associateWith { action ->
            validateUserResourcePermissionByRelation(
                userId = userId,
                action = action,
                projectCode = projectCode,
                resourceCode = resource.resourceCode,
                resourceType = resource.resourceType,
                relationResourceType = null
            )
        }
    }

    override fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean {
        return validateUserResourcePermissionByRelation(
            userId = userId,
            action = action,
            projectCode = projectCode,
            resourceCode = resource.resourceCode,
            resourceType = resource.resourceType,
            relationResourceType = null
        )
    }

    @Suppress("ReturnCount", "ComplexMethod")
    override fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String> {
        try {
            logger.info(
                "[iam V3] getUserResourceByAction : userId = $userId | action = $action " +
                    "| projectCode = $projectCode | resourceType = $resourceType"
            )
            // 管理员直接返回“*”
            if (iamCacheService.checkProjectManager(userId, projectCode)) {
                return arrayListOf("*")
            }
            val expression = iamCacheService.getUserExpression(userId, action, resourceType)
            logger.info("[iam V3] getUserResourceByAction : action = $action | expression = $expression")

            if (expression == null) {
                return emptyList()
            }

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
            logger.warn("getUserResourceByAction fail : $e")
        }
        return emptyList()
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        logger.info(
            "[iam V3] getUserResourcesByActions : userId = $userId | actions = $actions |" +
                " projectCode = $projectCode | resourceType = $resourceType"
        )
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

    override fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>> {
        return actions.associate { action ->
            val authPermission = action.substringAfterLast("_")
            AuthPermission.get(authPermission) to resources.map { it.resourceCode }
        }
    }

    override fun getUserResourceAndParentByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): Map<String, List<String>> {
        return emptyMap()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbsPermissionService::class.java)
    }
}
