package com.tencent.devops.auth.provider.rbac.service

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
import com.tencent.devops.auth.pojo.MonitorSpaceUpdateInfo
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.auth.service.AuthMonitorSpaceService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.concurrent.TimeUnit

class RbacPermissionAuthMonitorSpaceService constructor(
    private val authMonitorSpaceDao: AuthMonitorSpaceDao,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val systemService: SystemService
) : AuthMonitorSpaceService {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${monitor.url:#{null}}")
    private val monitorUrlPrefix = ""

    @Value("\${monitor.iamSystem:}")
    private val monitorSystemId = ""

    /*监控平台组配置*/
    private val monitorGroupConfigCache = Caffeine.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(7, TimeUnit.DAYS)
        .build<String/*配置名称*/, String/*配置*/>()

    /*监控动作名称*/
    private val monitorActionNameCache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(7, TimeUnit.DAYS)
        .build<String/*action*/, String/*actionName*/>()

    override fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): String {
        logger.info("RbacPermissionMonitorService|createMonitorSpace|$monitorSpaceCreateInfo")
        executeHttpRequest(
            urlSuffix = MONITOR_SPACE_CREATE_SUFFIX,
            method = POST_METHOD,
            body = objectMapper.writeValueAsString(monitorSpaceCreateInfo)
        ).data?.let { monitorSpaceDetail ->
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

    override fun getOrCreateMonitorSpace(
        projectName: String,
        projectCode: String,
        groupCode: String,
        userId: String?
    ): String {
        logger.info("RbacPermissionMonitorService|getOrCreateMonitorSpace|$projectName|$projectCode|$groupCode|$userId")
        val dbMonitorSpaceRecord = authMonitorSpaceDao.get(dslContext, projectCode)
        // 若为项目下其他组，由于在创建分级管理员时，已经创建好监控空间，并已经落库，只需要直接从数据库返回数据。
        if (groupCode != BkAuthGroup.GRADE_ADMIN.value) {
            return dbMonitorSpaceRecord?.spaceBizId?.toString()
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.ERROR_MONITOR_SPACE_NOT_EXIST,
                    defaultMessage = "The monitoring space($projectCode) does not exist "
                )
        }
        // 如果组id为GRADE_ADMIN，则为创建或者修改项目，需要同步修改或者创建监控空间，并存储数据库信息。
        return if (dbMonitorSpaceRecord != null) {
            updateMonitorSpace(
                projectCode = projectCode,
                monitorSpaceUpdateInfo = MonitorSpaceUpdateInfo(
                    spaceName = projectName,
                    spaceTypeId = appCode,
                    spaceUid = dbMonitorSpaceRecord.spaceUid,
                    updater = userId!!
                )
            )
        } else {
            val spaceUid = appCode.plus("__".plus(projectCode))
            val monitorSpaceDetailInfo = getMonitorSpaceDetail(spaceUid = spaceUid)
            if (monitorSpaceDetailInfo == null) {
                createMonitorSpace(
                    MonitorSpaceCreateInfo(
                        spaceName = projectName,
                        spaceTypeId = appCode,
                        spaceId = projectCode,
                        creator = requireNotNull(userId)
                    )
                )
            } else {
                updateMonitorSpace(
                    projectCode = projectCode,
                    monitorSpaceUpdateInfo = MonitorSpaceUpdateInfo(
                        spaceName = projectName,
                        spaceTypeId = appCode,
                        spaceUid = spaceUid,
                        updater = userId!!
                    ),
                    needCreateDbRecord = true
                )
            }
        }
    }

    override fun getMonitorSpaceBizId(projectCode: String): String {
        val dbMonitorSpaceRecord = authMonitorSpaceDao.get(dslContext, projectCode)
            ?: throw ErrorCodeException(
                errorCode = AuthMessageCode.ERROR_MONITOR_SPACE_NOT_EXIST,
                defaultMessage = "The monitoring space($projectCode) does not exist "
            )
        return "-${dbMonitorSpaceRecord.spaceBizId}"
    }

    override fun listMonitorSpaceBizIds(projectCode: List<String>): Map<String, String> {
        return authMonitorSpaceDao.list(
            dslContext = dslContext,
            projectCodes = projectCode
        ).mapValues { "-${it.value}" }
    }

    private fun updateMonitorSpace(
        projectCode: String,
        monitorSpaceUpdateInfo: MonitorSpaceUpdateInfo,
        /*若为false,说明数据库已有数据，只需对记录进行修改；否则插入新的数据库记录*/
        needCreateDbRecord: Boolean = false
    ): String {
        logger.info("update monitor Space|$projectCode|$needCreateDbRecord|$monitorSpaceUpdateInfo")
        executeHttpRequest(
            urlSuffix = MONITOR_SPACE_UPDATE_SUFFIX,
            method = POST_METHOD,
            body = objectMapper.writeValueAsString(monitorSpaceUpdateInfo)
        ).data?.let { monitorSpaceDetail ->
            if (needCreateDbRecord) {
                authMonitorSpaceDao.create(
                    dslContext = dslContext,
                    projectCode = monitorSpaceDetail.spaceId!!,
                    spaceBizId = monitorSpaceDetail.id!!,
                    spaceUid = monitorSpaceDetail.spaceUid!!,
                    creator = monitorSpaceUpdateInfo.updater
                )
            } else {
                authMonitorSpaceDao.update(
                    dslContext = dslContext,
                    projectCode = projectCode,
                    spaceUid = monitorSpaceDetail.spaceUid!!,
                    updateUser = monitorSpaceUpdateInfo.updater
                )
            }
            return monitorSpaceDetail.id.toString()
        }
        throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_MONITOR_SPACE_NOT_EXIST,
            defaultMessage = "Failed to create the monitoring space(${monitorSpaceUpdateInfo.spaceName})"
        )
    }

    override fun getMonitorSpaceDetail(spaceUid: String): MonitorSpaceDetailVO? {
        val monitorSpaceDetailResp = executeHttpRequest(
            urlSuffix = MONITOR_SPACE_DETAIL_SUFFIX.format(spaceUid),
            method = GET_METHOD
        )
        if (monitorSpaceDetailResp.code == MONITOR_SPACE_NOT_EXIST_CODE)
            return null
        return monitorSpaceDetailResp.data
    }

    private fun executeHttpRequest(
        urlSuffix: String,
        method: String,
        body: String? = null
    ): ResponseDTO<MonitorSpaceDetailVO> {
        val headerMap = mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        val headerStr = objectMapper.writeValueAsString(headerMap).replace("\\s".toRegex(), "")
        val url = monitorUrlPrefix + urlSuffix

        val requestBody = body?.let {
            body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
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
            val responseDTO = objectMapper.readValue<ResponseDTO<MonitorSpaceDetailVO>>(responseStr)
            if (responseDTO.code != REQUEST_SUCCESS_CODE && responseDTO.code != MONITOR_SPACE_NOT_EXIST_CODE) {
                // 请求错误
                logger.warn("request failed, url:($url)|response :($it)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            logger.info("request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return responseDTO
        }
    }

    override fun getMonitorGroupConfig(groupCode: String): String? {
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
        val actionList = systemService.getSystemFieldsInfo(monitorSystemId).actions
        logger.info("putAndGetMonitorGroupConfigCache|actionList:$actionList")
        // 2、获取监控平台常用配置组动作组
        val commonActions = systemService.getSystemFieldsInfo(monitorSystemId).commonActions
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
            if (action.name.contains("已废弃"))
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
                system = monitorSystemId
                type = SPACE_RESOURCE_TYPE
                id = PROJECT_ID_PLACEHOLDER
                name = PROJECT_NAME_PLACEHOLDER
            }
            val managerResources = ManagerResources().apply {
                system = monitorSystemId
                type = actionRelatedResourceType
                paths = listOf(listOf(managerPath))
            }
            val authorizationScopes = AuthorizationScopes().apply {
                system = monitorSystemId
                actions = listOf(Action(action.id))
                resources = listOf(managerResources)
            }
            groupConfig.add(authorizationScopes)
        }
    }

    override fun getMonitorActionName(action: String): String? {
        return monitorActionNameCache.getIfPresent(action) ?: putAndGetMonitorActionNameCache(action)
    }

    private fun putAndGetMonitorActionNameCache(action: String): String? {
        val actionList = systemService.getSystemFieldsInfo(monitorSystemId).actions
        actionList.forEach {
            monitorActionNameCache.put(it.id, it.name)
        }
        return monitorActionNameCache.getIfPresent(action)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionAuthMonitorSpaceService::class.java)
        private const val MONITOR_SPACE_CREATE_SUFFIX = "metadata_create_space"
        private const val MONITOR_SPACE_UPDATE_SUFFIX = "metadata_update_space"
        private const val MONITOR_SPACE_DETAIL_SUFFIX = "metadata_get_space_detail?space_uid=%s"
        private const val POST_METHOD = "POST"
        private const val GET_METHOD = "GET"
        private const val REQUEST_SUCCESS_CODE = 200L
        private const val MONITOR_SPACE_NOT_EXIST_CODE = 404L
        private const val PROJECT_ID_PLACEHOLDER = "#projectId#"
        private const val PROJECT_NAME_PLACEHOLDER = "#projectName#"
        private const val SPACE_RESOURCE_TYPE = "space"
        private const val MANAGER_GROUP_CONFIG_NAME = "managerGroupConfig"
        private const val OP_GROUP_CONFIG_NAME = "opGroupConfig"
        private const val READ_ONLY_GROUP_CONFIG_NAME = "readOnlyGroupConfig"
        private const val READ_ONLY_ACTIONS = "Read-only Actions"
        private const val OPS_ACTIONS = "Ops Actions"
    }
}
