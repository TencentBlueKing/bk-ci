package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.bk.sdk.iam.dto.action.ActionDTO
import com.tencent.bk.sdk.iam.dto.manager.Action
import com.tencent.bk.sdk.iam.dto.manager.AuthorizationScopes
import com.tencent.bk.sdk.iam.dto.manager.ManagerPath
import com.tencent.bk.sdk.iam.dto.manager.ManagerResources
import com.tencent.bk.sdk.iam.service.SystemService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthMonitorSpaceDao
import com.tencent.devops.auth.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.auth.pojo.MonitorSpaceDetailVO
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit

@Suppress("LongParameterList")
class RbacPermissionMonitorService constructor(
    private val systemService: SystemService,
    private val authMonitorSpaceDao: AuthMonitorSpaceDao,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper
) : AuthMonitorService {
    /*监控平台组配置*/
    private val monitorGroupConfigCache = Caffeine.newBuilder()
        .maximumSize(10)
        .expireAfterWrite(12L, TimeUnit.HOURS)
        .build<String/*配置名称*/, String/*配置*/>()

    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${monitor.url:}")
    private val monitorUrlPrefix = ""

    override fun generateMonitorAuthorizationScopes(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): List<AuthorizationScopes> {
        val spaceBizId = getOrCreateMonitorSpaceBizId(
            projectName = projectName,
            projectCode = projectCode,
            groupCode = groupCode,
            userId = userId
        )
        logger.info("RbacPermissionMonitorService|generateMonitorAuthorizationScopes|$spaceBizId")
        return RbacAuthUtils.buildAuthorizationScopes(
            systemId = MONITOR_SYSTEM_ID,
            authorizationScopesStr = getMonitorGroupConfig(groupCode)!!,
            projectCode = "-$spaceBizId",
            projectName = projectName,
            iamResourceCode = projectCode,
            resourceName = projectName
        )
    }

    private fun getOrCreateMonitorSpaceBizId(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): String {
        logger.info("RbacPermissionMonitorService|getOrCreateMonitorSpace|$projectName|$projectCode|$groupCode|$userId")
        authMonitorSpaceDao.get(dslContext, projectCode)?.let {
            return it.spaceBizId.toString()
        }
        if (groupCode == BkAuthGroup.GRADE_ADMIN.value) {
            return createMonitorSpace(
                MonitorSpaceCreateInfo(
                    spaceName = projectName,
                    spaceTypeId = appCode,
                    spaceId = projectCode,
                    creator = requireNotNull(userId)
                )
            )
        }
        throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_MONITOR_SPACE_NOT_EXIST,
            defaultMessage = "The monitoring space($projectCode) does not exist "
        )
    }

    override fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): String {
        executeHttpRequest(
            urlSuffix = MONITOR_SPACE_CREATE_SUFFIX,
            method = POST_METHOD,
            body = monitorSpaceCreateInfo
        ).data?.let { monitorSpaceDetailData ->
            val monitorSpaceDetail = generateMonitorSpaceDetail(monitorSpaceDetailData)
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.ERROR_MONITOR_SPACE_NOT_EXIST,
                    defaultMessage = "The monitoring space(${monitorSpaceCreateInfo.spaceId}) does not exist "
                )
            authMonitorSpaceDao.create(
                dslContext = dslContext,
                projectCode = monitorSpaceDetail.spaceId!!,
                spaceBizId = monitorSpaceDetail.id!!,
                spaceUid = monitorSpaceDetail.spaceUid!!,
                creator = monitorSpaceCreateInfo.creator
            )
            return monitorSpaceDetail.id.toString()
        }
        throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_MONITOR_SPACE_NOT_EXIST,
            defaultMessage = "Failed to create the monitoring space(${monitorSpaceCreateInfo.spaceId})"
        )
    }

    override fun getMonitorSpaceDetail(spaceUid: String): MonitorSpaceDetailVO? {
        val monitorSpaceDetailData = executeHttpRequest(
            urlSuffix = MONITOR_SPACE_DETAIL_SUFFIX.format(spaceUid),
            method = GET_METHOD
        ).data
        return generateMonitorSpaceDetail(monitorSpaceDetailData)
    }

    private fun generateMonitorSpaceDetail(monitorSpaceDetailData: Any?): MonitorSpaceDetailVO? {
        if (monitorSpaceDetailData == null)
            return null
        val monitorSpaceDetailMap = monitorSpaceDetailData as Map<*, *>
        val monitorSpaceDetailVO =
            MonitorSpaceDetailVO(
                id = monitorSpaceDetailMap["id"]?.toString()?.toLong(),
                spaceName = monitorSpaceDetailMap["space_name"] as String?,
                spaceTypeId = monitorSpaceDetailMap["space_type_id"] as String?,
                spaceId = monitorSpaceDetailMap["space_id"] as String?,
                spaceUid = monitorSpaceDetailMap["space_uid"] as String?,
                status = monitorSpaceDetailMap["status"] as String?,
                creator = monitorSpaceDetailMap["creator"] as String?
            )
        logger.info("generateMonitorSpaceDetail:monitorSpaceDetailVO($monitorSpaceDetailVO)")
        return monitorSpaceDetailVO
    }

    private fun getMonitorGroupConfig(groupCode: String): String? {
        val configName = when (groupCode) {
            BkAuthGroup.GRADE_ADMIN.value -> MANAGER_GROUP_CONFIG_NAME
            BkAuthGroup.MANAGER.value, BkAuthGroup.MAINTAINER.value -> READ_ONLY_GROUP_CONFIG_NAME
            else -> OP_GROUP_CONFIG_NAME
        }
        return monitorGroupConfigCache.getIfPresent(configName) ?: putAndGetMonitorGroupConfigCache(configName)
    }

    private fun putAndGetMonitorGroupConfigCache(configName: String): String? {
        // 0、构造 三个授权范围组，分别为管理员配置、业务只读配置、业务运维配置
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
            logger.info(
                "putAndGetMonitorGroupConfigCache:action|$action|" +
                    "${action.englishName}|${action.relatedResourceTypes}"
            )
            if (!action.id.contains("v2"))
                return@foreach
            // 过滤掉监控平台全局动作
            if (action.relatedResourceTypes.isEmpty())
                return@foreach
            logger.info(
                "putAndGetMonitorGroupConfigCache:greysonfangaction|$action|" +
                    "${action.englishName}|${action.relatedResourceTypes}"
            )
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
                    authorizationScopes.actions.toMutableList().add(Action(action.id))
                }
                logger.info("generateGroupAuthorizationScopes:old|$groupConfig")
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
            logger.info("generateGroupAuthorizationScopes:new|$groupConfig")
        }
    }

    private fun executeHttpRequest(urlSuffix: String, method: String, body: Any? = null): ResponseDTO {
        val headerMap = mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        val headerStr = objectMapper.writeValueAsString(headerMap).replace("\\s".toRegex(), "")
        val url = monitorUrlPrefix + urlSuffix

        val requestBody = body?.let {
            objectMapper.writeValueAsString(it).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr)

        when (method) {
            GET_METHOD -> requestBuilder.get()
            POST_METHOD -> requestBuilder.post(requestBody!!)
        }
        OkhttpUtils.doHttp(requestBuilder.build()).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, uri:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            logger.info("executeHttpRequest:${it.body!!}")
            val responseStr = it.body!!.string()
            logger.info("executeHttpRequest:$responseStr")
            val responseDTO = objectMapper.readValue<ResponseDTO>(responseStr)
            if (responseDTO.code != 200L) {
                // 请求错误
                logger.warn("request failed, url:($url)|response :($it)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            logger.info("request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return responseDTO
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionMonitorService::class.java)
        private const val PROJECT_ID_PLACEHOLDER = "#projectId#"
        private const val PROJECT_NAME_PLACEHOLDER = "#projectName#"
        private const val SPACE_RESOURCE_TYPE = "space"
        private const val MANAGER_GROUP_CONFIG_NAME = "managerGroupConfig"
        private const val OP_GROUP_CONFIG_NAME = "opGroupConfig"
        private const val READ_ONLY_GROUP_CONFIG_NAME = "readOnlyGroupConfig"
        private const val READ_ONLY_ACTIONS = "Read-only Actions"
        private const val OPS_ACTIONS = "Ops Actions"
        private const val MONITOR_SPACE_CREATE_SUFFIX = "metadata_create_space"
        private const val MONITOR_SPACE_DETAIL_SUFFIX = "metadata_get_space_detail?space_uid=%s"
        private const val POST_METHOD = "POST"
        private const val GET_METHOD = "GET"
        private const val MONITOR_SYSTEM_ID = "bk_monitorv3"
    }
}
