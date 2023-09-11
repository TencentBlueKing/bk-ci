package com.tencent.devops.auth.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.service.SystemService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit

class RbacPermissionAuthorizationScopesService constructor(
    private val systemService: SystemService,
    private val authMonitorSpaceService: AuthMonitorSpaceService,
    private val objectMapper: ObjectMapper,
    private val iamConfiguration: IamConfiguration
) : AuthAuthorizationScopesService {
    /*监控平台组配置*/
    private val monitorGroupConfigCache = Caffeine.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(7, TimeUnit.DAYS)
        .build<String/*配置名称*/, String/*配置*/>()

    @Value("\${monitor.register:false}")
    private val registerMonitor: Boolean = false

    override fun generateBkciAuthorizationScopes(
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String
    ): List<AuthorizationScopes> {
        return buildAuthorizationScopes(
            systemId = iamConfiguration.systemId,
            authorizationScopesStr = authorizationScopesStr,
            projectCode = projectCode,
            projectName = projectName,
            iamResourceCode = iamResourceCode,
            resourceName = resourceName
        )
    }

    @Suppress("LongParameterList")
    private fun buildAuthorizationScopes(
        systemId: String,
        authorizationScopesStr: String,
        projectCode: String,
        projectName: String,
        iamResourceCode: String,
        resourceName: String
    ): List<AuthorizationScopes> {
        val replaceAuthorizationScopesStr =
            authorizationScopesStr.replace(SYSTEM_PLACEHOLDER, systemId)
                .replace(PROJECT_ID_PLACEHOLDER, projectCode)
                .replace(PROJECT_NAME_PLACEHOLDER, projectName)
                .replace(RESOURCE_CODE_PLACEHOLDER, iamResourceCode)
                // 如果资源名中有\,需要转义,不然json序列化时会报错
                .replace(RESOURCE_NAME_PLACEHOLDER, resourceName.replace("\\", "\\\\"))
        logger.info("$systemId|$projectCode authorization scopes after replace $replaceAuthorizationScopesStr ")
        return JsonUtil.to(replaceAuthorizationScopesStr, object : TypeReference<List<AuthorizationScopes>>() {})
    }

    override fun generateMonitorAuthorizationScopes(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): List<AuthorizationScopes> {
        if (!registerMonitor)
            return listOf()
        val spaceBizId = authMonitorSpaceService.getMonitorSpaceBizId(
            projectName = projectName,
            projectCode = projectCode,
            groupCode = groupCode,
            userId = userId
        )
        logger.info("RbacPermissionMonitorService|generateMonitorAuthorizationScopes|$spaceBizId")
        return buildAuthorizationScopes(
            systemId = MONITOR_SYSTEM_ID,
            authorizationScopesStr = getMonitorGroupConfig(groupCode)!!,
            projectCode = "-$spaceBizId",
            projectName = projectName,
            iamResourceCode = projectCode,
            resourceName = projectName
        )
    }

    private fun getMonitorGroupConfig(groupCode: String): String? {
        val configName = when (groupCode) {
            BkAuthGroup.GRADE_ADMIN.value -> MANAGER_GROUP_CONFIG_NAME
            BkAuthGroup.MANAGER.value, BkAuthGroup.MAINTAINER.value -> OP_GROUP_CONFIG_NAME
            else -> READ_ONLY_GROUP_CONFIG_NAME
        }
        return monitorGroupConfigCache.getIfPresent(configName) ?: putAndGetMonitorGroupConfigCache(configName)
    }

    private fun putAndGetMonitorGroupConfigCache(configName: String): String? {
        // 0、构造 三个授权范围组，分别为管理员配置、业务运维配置、业务只读配置
        val managerGroupConfig = mutableListOf<AuthorizationScopes>()
        val opGroupConfig = mutableListOf<AuthorizationScopes>()
        val readOnlyGroupConfig = mutableListOf<AuthorizationScopes>()

        // 1、通过system接口获取监控平台action组。
        val actionList = systemService.getSystemFieldsInfo(MONITOR_SYSTEM_ID).actions
        logger.info("putAndGetMonitorGroupConfigCache|actionList:$actionList")
        // 2、获取监控平台常用配置组动作组
        val commonActions = systemService.getSystemFieldsInfo(MONITOR_SYSTEM_ID).commonActions
        // 3、分别获取业务只读动作组和业务运维动作组
        val readOnlyActions = commonActions.find { it.englishName == READ_ONLY_ACTIONS }?.actions?.map { it.id }
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_MONITOR_READ_ONLY_ACTIONS_NOT_EXIST,
                defaultMessage = "monitor read only actions does not exist"
            )
        logger.info("putAndGetMonitorGroupConfigCache|readOnlyActions:$readOnlyActions")
        val opsActions = commonActions.find { it.englishName == OPS_ACTIONS }?.actions?.map { it.id }
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_MONITOR_OPS_ACTIONS_NOT_EXIST,
                defaultMessage = "monitor ops actions does not exist"
            )
        logger.info("putAndGetMonitorGroupConfigCache|opsActions:$opsActions")
        // 4、遍历action组。获取该action的关联类型。
        actionList.forEach foreach@{ action ->
            // 监控平台测试环境有些动作，已经被废除，但并没有删除，正确的动作都包含”v2“标识
            if (!action.id.contains("v2"))
                return@foreach
            // 过滤掉监控平台全局动作
            if (action.relatedResourceTypes.isEmpty())
                return@foreach
            generateGroupAuthorizationScopes(action, managerGroupConfig)
            if (readOnlyActions.contains(action.id))
                generateGroupAuthorizationScopes(action, readOnlyGroupConfig)
            if (opsActions.contains(action.id))
                generateGroupAuthorizationScopes(action, opGroupConfig)
        }
        monitorGroupConfigCache.put(MANAGER_GROUP_CONFIG_NAME, objectMapper.writeValueAsString(managerGroupConfig))
        monitorGroupConfigCache.put(OP_GROUP_CONFIG_NAME, objectMapper.writeValueAsString(opGroupConfig))
        monitorGroupConfigCache.put(READ_ONLY_GROUP_CONFIG_NAME, objectMapper.writeValueAsString(readOnlyGroupConfig))
        logger.info(
            "putAndGetMonitorGroupConfigCache|MANAGER_GROUP_CONFIG:" +
                "${monitorGroupConfigCache.getIfPresent(MANAGER_GROUP_CONFIG_NAME)}"
        )
        logger.info(
            "putAndGetMonitorGroupConfigCache|OP_GROUP_CONFIG:" +
                "${monitorGroupConfigCache.getIfPresent(OP_GROUP_CONFIG_NAME)}"
        )
        logger.info(
            "putAndGetMonitorGroupConfigCache|READ_ONLY:" +
                "${monitorGroupConfigCache.getIfPresent(READ_ONLY_GROUP_CONFIG_NAME)}"
        )
        return monitorGroupConfigCache.getIfPresent(configName)
    }

    @Suppress("NestedBlockDepth")
    private fun generateGroupAuthorizationScopes(
        action: ActionDTO,
        groupConfig: MutableList<AuthorizationScopes>
    ) {
        val actionRelatedResourceType = action.relatedResourceTypes.firstOrNull()?.id
        val isExistResourceType = groupConfig.find {
            it.resources.firstOrNull()?.type == actionRelatedResourceType
        } != null
        logger.info("generateGroupAuthorizationScopes:$actionRelatedResourceType|$action|$isExistResourceType")
        if (isExistResourceType) {
            groupConfig.forEach { authorizationScopes ->
                if (authorizationScopes.resources.firstOrNull()?.type == actionRelatedResourceType) {
                    val newActions = authorizationScopes.actions.toMutableList().apply { add(Action(action.id)) }
                    authorizationScopes.actions = newActions
                }
            }
        } else {
            val managerPath = ManagerPath().apply {
                system = MONITOR_SYSTEM_ID
                type = SPACE_RESOURCE_TYPE
                id = PROJECT_ID_PLACEHOLDER
                name = PROJECT_NAME_PLACEHOLDER
            }
            val managerResources = ManagerResources().apply {
                system = MONITOR_SYSTEM_ID
                type = actionRelatedResourceType
                paths = listOf(listOf(managerPath))
            }
            val authorizationScopes = AuthorizationScopes().apply {
                system = MONITOR_SYSTEM_ID
                actions = listOf(Action(action.id))
                resources = listOf(managerResources)
            }
            groupConfig.add(authorizationScopes)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionAuthorizationScopesService::class.java)
        private const val SYSTEM_PLACEHOLDER = "#system#"
        private const val PROJECT_ID_PLACEHOLDER = "#projectId#"
        private const val PROJECT_NAME_PLACEHOLDER = "#projectName#"
        private const val RESOURCE_CODE_PLACEHOLDER = "#resourceCode#"
        private const val RESOURCE_NAME_PLACEHOLDER = "#resourceName#"
        private const val SPACE_RESOURCE_TYPE = "space"
        private const val MANAGER_GROUP_CONFIG_NAME = "managerGroupConfig"
        private const val OP_GROUP_CONFIG_NAME = "opGroupConfig"
        private const val READ_ONLY_GROUP_CONFIG_NAME = "readOnlyGroupConfig"
        private const val READ_ONLY_ACTIONS = "Read-only Actions"
        private const val OPS_ACTIONS = "Ops Actions"
        private const val MONITOR_SYSTEM_ID = "bk_monitorv3"
    }
}
