package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthMonitorSpaceDao
import com.tencent.devops.auth.pojo.MonitorSpaceCreateInfo
import com.tencent.devops.auth.pojo.MonitorSpaceDetailVO
import com.tencent.devops.auth.pojo.MonitorSpaceUpdateInfo
import com.tencent.devops.auth.pojo.ResponseDTO
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

class RbacPermissionAuthMonitorSpaceService constructor(
    private val authMonitorSpaceDao: AuthMonitorSpaceDao,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper
) : AuthMonitorSpaceService {
    @Value("\${auth.appCode:}")
    private val appCode = ""

    @Value("\${auth.appSecret:}")
    private val appSecret = ""

    @Value("\${monitor.url:#{null}}")
    private val monitorUrlPrefix = ""
    override fun createMonitorSpace(monitorSpaceCreateInfo: MonitorSpaceCreateInfo): String {
        logger.info("RbacPermissionMonitorService|createMonitorSpace|$monitorSpaceCreateInfo")
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

    override fun getMonitorSpaceBizId(
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

    private fun updateMonitorSpace(
        projectCode: String,
        monitorSpaceUpdateInfo: MonitorSpaceUpdateInfo,
        /*若为false,说明数据库已有数据，只需对记录进行修改；否则插入新的数据库记录*/
        needCreateDbRecord: Boolean = false
    ): String {
        logger.info("RbacPermissionMonitorService|updateMonitorSpace|$projectCode|$needCreateDbRecord|$monitorSpaceUpdateInfo")
        executeHttpRequest(
            urlSuffix = MONITOR_SPACE_UPDATE_SUFFIX,
            method = POST_METHOD,
            body = monitorSpaceUpdateInfo
        ).data?.let { monitorSpaceDetailData ->
            val monitorSpaceDetail = generateMonitorSpaceDetail(monitorSpaceDetailData)
                ?: throw ErrorCodeException(
                    errorCode = AuthMessageCode.ERROR_MONITOR_SPACE_NOT_EXIST,
                    defaultMessage = "The monitoring space(${monitorSpaceUpdateInfo.spaceName}) does not exist "
                )
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
        if (monitorSpaceDetailResp.code == MONITOR_SPACE_NOT_EXIST)
            return null
        return generateMonitorSpaceDetail(monitorSpaceDetailResp.data)
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
            if (responseDTO.code != REQUEST_SUCCESS_CODE) {
                // 请求错误
                logger.warn("request failed, url:($url)|response :($it)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            logger.info("request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            return responseDTO
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionAuthMonitorSpaceService::class.java)
        private const val MONITOR_SPACE_CREATE_SUFFIX = "metadata_create_space"
        private const val MONITOR_SPACE_UPDATE_SUFFIX = "metadata_update_space"
        private const val MONITOR_SPACE_DETAIL_SUFFIX = "metadata_get_space_detail?space_uid=%s"
        private const val POST_METHOD = "POST"
        private const val GET_METHOD = "GET"
        private const val REQUEST_SUCCESS_CODE = 200L
        private const val MONITOR_SPACE_NOT_EXIST = 404L
    }
}
