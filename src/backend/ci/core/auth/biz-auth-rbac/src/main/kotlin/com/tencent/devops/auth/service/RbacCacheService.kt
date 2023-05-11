package com.tencent.devops.auth.service

import com.fasterxml.jackson.core.type.TypeReference
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.bk.sdk.iam.dto.SubjectDTO
import com.tencent.bk.sdk.iam.dto.V2QueryPolicyDTO
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.resource.V2ResourceNode
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.pojo.AuthGroupConfigAction
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Suppress("MagicNumber", "LongParameterList")
class RbacCacheService constructor(
    private val dslContext: DSLContext,
    private val authResourceTypeDao: AuthResourceTypeDao,
    private val authActionDao: AuthActionDao,
    private val policyService: PolicyService,
    private val iamConfiguration: IamConfiguration,
    private val authResourceGroupConfigDao: AuthResourceGroupConfigDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacCacheService::class.java)
    }

    /*获取资源类型下的动作*/
    private val resourceType2ActionCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*resourceType*/, List<ActionInfoVo>>()
    private val resourceTypeCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*resourceType*/, ResourceTypeInfoVo>()
    private val actionCache = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(7L, TimeUnit.DAYS)
        .build<String/*action*/, ActionInfoVo>()

    // 用户-管理员项目 缓存， 5分钟有效时间
    private val projectManager = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String, List<String>>()

    private val groupConfigActionsCache = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(1L, TimeUnit.DAYS)
        .build<String/*resourceType*/, List<AuthGroupConfigAction>>()

    fun listResourceTypes(): List<ResourceTypeInfoVo> {
        if (resourceTypeCache.asMap().values.isEmpty()) {
            authResourceTypeDao.list(dslContext).forEach {
                val resourceTypeInfo = ResourceTypeInfoVo(
                    id = it.id,
                    resourceType = it.resourceType,
                    name = it.name,
                    parent = it.parent,
                    system = it.system
                )
                resourceTypeCache.put(it.resourceType, resourceTypeInfo)
            }
        }
        return resourceTypeCache.asMap().values.toList().sortedBy { it.id }
    }

    fun listResourceType2Action(resourceType: String): List<ActionInfoVo> {
        if (resourceType2ActionCache.getIfPresent(resourceType) == null) {
            val actionList = authActionDao.list(dslContext, resourceType)
            if (actionList.isEmpty()) {
                throw ErrorCodeException(
                    errorCode = AuthMessageCode.RESOURCE_ACTION_EMPTY,
                    params = arrayOf(resourceType),
                    defaultMessage = "the action relate with the resource type($resourceType) does not exist"
                )
            }
            val actionInfoVoList = actionList.map {
                ActionInfoVo(
                    action = it.action,
                    actionName = it.actionName,
                    resourceType = it.resourceType,
                    relatedResourceType = it.relatedResourceType
                )
            }
            resourceType2ActionCache.put(resourceType, actionInfoVoList)
        }
        return resourceType2ActionCache.getIfPresent(resourceType)!!
    }

    fun getActionInfo(action: String): ActionInfoVo {
        if (actionCache.getIfPresent(action) == null) {
            val actionRecord = authActionDao.get(dslContext, action)
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.ACTION_NOT_EXIST,
                    params = arrayOf(action),
                    defaultMessage = "the action($action) does not exist"
                )
            val actionInfo = ActionInfoVo(
                action = actionRecord.action,
                actionName = actionRecord.actionName,
                resourceType = actionRecord.resourceType,
                relatedResourceType = actionRecord.relatedResourceType
            )
            actionCache.put(action, actionInfo)
        }
        return actionCache.getIfPresent(action)!!
    }

    fun getResourceTypeInfo(resourceType: String): ResourceTypeInfoVo {
        if (resourceTypeCache.getIfPresent(resourceType) == null) {
            listResourceTypes()
        }
        return resourceTypeCache.getIfPresent(resourceType) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.RESOURCE_TYPE_NOT_FOUND,
            params = arrayOf(resourceType),
            defaultMessage = "the resource type($resourceType) does not exist"
        )
    }

    fun checkProjectManager(userId: String, projectCode: String): Boolean {
        // TODO 开启管理员缓存，会存在当把用户从管理员移除，用户还是能够进入用户管理界面，但是iam那边会报错，所以先不缓存，后期优化
        return validateUserProjectPermission(
            userId = userId,
            projectCode = projectCode,
            permission = AuthPermission.MANAGE
        )
    }

    fun getGroupConfigAction(resourceType: String): List<AuthGroupConfigAction> {
        if (groupConfigActionsCache.getIfPresent(resourceType) == null) {
            val groupConfigActions =
                authResourceGroupConfigDao.get(dslContext = dslContext, resourceType = resourceType).map {
                    with(it) {
                        AuthGroupConfigAction(
                            id = id,
                            resourceType = resourceType,
                            groupCode = groupCode,
                            groupName = groupName,
                            actions = JsonUtil.to(
                                actions,
                                object : TypeReference<List<String>>() {}
                            )
                        )
                    }
                }
            groupConfigActionsCache.put(resourceType, groupConfigActions)
        }
        return groupConfigActionsCache.getIfPresent(resourceType)!!.sortedByDescending { it.id }
    }

    private fun validateUserProjectPermission(
        userId: String,
        projectCode: String,
        permission: AuthPermission
    ): Boolean {
        logger.info("[rbac] validate user project permission|userId = $userId|permission=$permission")
        val startEpoch = System.currentTimeMillis()
        try {
            val actionDTO = ActionDTO()
            actionDTO.id = RbacAuthUtils.buildAction(
                authPermission = permission,
                authResourceType = AuthResourceType.PROJECT
            )

            val resourceNode = V2ResourceNode.builder().system(iamConfiguration.systemId)
                .type(AuthResourceType.PROJECT.value)
                .id(projectCode)
                .build()

            val subject = SubjectDTO.builder()
                .id(userId)
                .type(ManagerScopesEnum.getType(ManagerScopesEnum.USER))
                .build()
            val queryPolicyDTO = V2QueryPolicyDTO.builder().system(iamConfiguration.systemId)
                .subject(subject)
                .action(actionDTO)
                .resources(listOf(resourceNode))
                .build()

            return policyService.verifyPermissions(queryPolicyDTO)
        } finally {
            logger.info(
                "It take(${System.currentTimeMillis() - startEpoch})ms to validate user project permission"
            )
        }
    }
}
