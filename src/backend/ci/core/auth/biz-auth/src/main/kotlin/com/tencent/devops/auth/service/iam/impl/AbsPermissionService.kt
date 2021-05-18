package com.tencent.devops.auth.service.iam.impl

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

open class AbsPermissionService @Autowired constructor(
    private val authHelper: AuthHelper,
    private val policyService: PolicyService,
    private val iamConfiguration: IamConfiguration
) : PermissionService {

    override fun validateUserActionPermission(userId: String, action: String): Boolean {
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
        val actionDto = ActionDTO()
        actionDto.id = action
        val expression = (policyService.getPolicyByAction(userId, actionDto, null) ?: return emptyList())
        logger.info("[iam V3] getUserResourceByPermission expression:$expression")

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
    }

    override fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>> {
        val result = mutableMapOf<AuthPermission, List<String>>()
        actions.forEach {
            val actionResourceList = getUserResourceByAction(
                userId = userId,
                action = it,
                projectCode = projectCode,
                resourceType = resourceType
            )
            result[AuthPermission.valueOf(it)] = actionResourceList
        }
        return result
    }

    companion object{
        val logger = LoggerFactory.getLogger(AbsPermissionService::class.java)
    }
}
