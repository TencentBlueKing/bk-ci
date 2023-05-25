/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.engine.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.pipeline.enums.ProjectPipelineCallbackStatus
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.PipelineCallbackEvent
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.common.service.trace.TraceTag
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.ProjectPipelineCallbackDao
import com.tencent.devops.process.dao.ProjectPipelineCallbackHistoryDao
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.CallBackHeader
import com.tencent.devops.process.pojo.CreateCallBackResult
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import com.tencent.devops.process.pojo.setting.PipelineModelVersion
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Suppress("ALL")
@Service
class ProjectPipelineCallBackService @Autowired constructor(
    private val dslContext: DSLContext,
    val authProjectApi: AuthProjectApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val projectPipelineCallbackDao: ProjectPipelineCallbackDao,
    private val projectPipelineCallbackHistoryDao: ProjectPipelineCallbackHistoryDao,
    private val projectPipelineCallBackUrlGenerator: ProjectPipelineCallBackUrlGenerator,
    private val client: Client,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelinePermissionService: PipelinePermissionService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectPipelineCallBackService::class.java)
        private val JSON = "application/json;charset=utf-8".toMediaTypeOrNull()
    }

    fun createCallBack(
        userId: String,
        projectId: String,
        url: String,
        region: CallBackNetWorkRegionType?,
        event: String,
        secretToken: String?
    ): CreateCallBackResult {
        // 验证用户是否为管理员
        validProjectManager(userId, projectId)
        if (!OkhttpUtils.validUrl(url)) {
            logger.warn("$projectId|callback url Invalid")
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_CALLBACK_URL_INVALID)
        }
        val callBackUrl = projectPipelineCallBackUrlGenerator.generateCallBackUrl(
            region = region,
            url = url
        )
        if (event.isBlank()) {
            throw ParamBlankException("Invalid event")
        }
        val events = event.split(",").map {
            CallBackEvent.valueOf(it.trim())
        }

        val successEvents = mutableListOf<String>()
        val failureEvents = mutableMapOf<String, String>()
        events.forEach {
            try {
                val projectPipelineCallBack = ProjectPipelineCallBack(
                    projectId = projectId,
                    callBackUrl = callBackUrl,
                    events = it.name,
                    secretToken = secretToken
                )
                projectPipelineCallbackDao.save(
                    dslContext = dslContext,
                    projectId = projectPipelineCallBack.projectId,
                    events = projectPipelineCallBack.events,
                    userId = userId,
                    callbackUrl = projectPipelineCallBack.callBackUrl,
                    secretToken = projectPipelineCallBack.secretToken,
                    id = client.get(ServiceAllocIdResource::class).generateSegmentId("PROJECT_PIPELINE_CALLBACK").data
                )
                successEvents.add(it.name)
            } catch (e: Throwable) {
                logger.error("Fail to create callback|$projectId|${it.name}|$callBackUrl", e)
                failureEvents[it.name] = e.message ?: "Fail to create callback"
            }
        }
        return CreateCallBackResult(
            successEvents = successEvents,
            failureEvents = failureEvents
        )
    }

    fun listProjectCallBack(projectId: String, events: String): List<ProjectPipelineCallBack> {
        val list = mutableListOf<ProjectPipelineCallBack>()
        val records = projectPipelineCallbackDao.listProjectCallback(
            dslContext = dslContext,
            projectId = projectId,
            events = events
        )
        records.forEach {
            list.add(
                ProjectPipelineCallBack(
                    id = it.id,
                    projectId = it.projectId,
                    callBackUrl = it.callbackUrl,
                    events = it.events,
                    secretToken = it.secretToken,
                    enable = it.enable
                )
            )
        }
        return list
    }

    fun listByPage(
        userId: String,
        projectId: String,
        offset: Int,
        limit: Int
    ): SQLPage<ProjectPipelineCallBack> {
        checkParam(userId, projectId)
        // 验证用户是否有权限查看
        validAuth(userId, projectId)
        val count = projectPipelineCallbackDao.countByPage(dslContext, projectId)
        val records = projectPipelineCallbackDao.listByPage(dslContext, projectId, offset, limit)
        return SQLPage(
            count,
            records.map {
                ProjectPipelineCallBack(
                    id = it.id,
                    projectId = it.projectId,
                    callBackUrl = it.callbackUrl,
                    events = it.events,
                    secretToken = null,
                    enable = it.enable
                )
            }
        )
    }

    fun delete(userId: String, projectId: String, id: Long) {
        checkParam(userId, projectId)
        validProjectManager(userId, projectId)
        projectPipelineCallbackDao.get(
            dslContext = dslContext,
            projectId = projectId,
            id = id
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_CALLBACK_NOT_FOUND,
            params = arrayOf(id.toString())
        )
        projectPipelineCallbackDao.deleteById(
            dslContext = dslContext,
            projectId = projectId,
            id = id
        )
    }

    fun disable(callBack: ProjectPipelineCallBack) {
        // 修改接口状态
        projectPipelineCallbackDao.disable(
            dslContext = dslContext,
            projectId = callBack.projectId,
            id = callBack.id!!
        )
        // 通知用户接口被禁用
        sendDisableNotifyMessage(callBack)
    }

    /**
     *  发送回调禁用通知
     */
    fun sendDisableNotifyMessage(callBack: ProjectPipelineCallBack) {
        try {
            val callbackRecord = projectPipelineCallbackDao.get(
                dslContext = dslContext,
                projectId = callBack.projectId,
                id = callBack.id!!
            )
            callbackRecord?.run {
                with(callbackRecord) {
                    // 项目信息
                    val projectInfo = client.get(ServiceProjectResource::class).get(
                        englishName = projectId
                    ).data
                    // 禁用通知
                    client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(
                        SendNotifyMessageTemplateRequest(
                            templateCode =
                            PipelineNotifyTemplateEnum.PIPELINE_CALLBACK_DISABLE_NOTIFY_TEMPLATE.templateCode,
                            receivers = mutableSetOf(creator),
                            notifyType = mutableSetOf(NotifyType.RTX.name),
                            titleParams = mapOf(),
                            bodyParams = mapOf(
                                "projectName" to (projectInfo?.projectName ?: ""),
                                "events" to events,
                                "callbackUrl" to callbackUrl,
                                "pipelineListUrl" to projectPipelineListUrl(
                                    projectId = projectId
                                )
                            ),
                            cc = null,
                            bcc = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.warn(
                "Failure to send disable notify message for " +
                        "[${callBack.projectId}|${callBack.callBackUrl}|${callBack.events}]", e
            )
        }
    }

    fun enable(callBack: ProjectPipelineCallBack) {
        // 启用接口
        projectPipelineCallbackDao.enable(
            dslContext = dslContext,
            projectId = callBack.projectId,
            id = callBack.id!!
        )
    }

    fun enableByIds(
        projectId: String,
        callbackIds: String
    ) {
        val ids = callbackIds.split(",")
            .filter { it.isNotEmpty() }
            .map { it.toInt() }
        projectPipelineCallbackDao.enableByIds(
            dslContext = dslContext,
            projectId = projectId,
            ids = ids
        )
    }

    /**
     * 获取已被禁用的callback信息
     */
    fun getDisableCallbackList(
        offset: Int,
        limit: Int,
        projectId: String?,
        url: String?
    ): List<ProjectPipelineCallBack> {
        return projectPipelineCallbackDao.getDisableCallbackList(
            dslContext = dslContext,
            projectId = projectId,
            url = url,
            limit = limit,
            offset = offset
        ).map {
            ProjectPipelineCallBack(
                id = it.id,
                projectId = it.projectId,
                callBackUrl = it.callbackUrl,
                events = it.events,
                secretToken = null,
                enable = it.enable
            )
        }
    }

    fun createHistory(
        projectPipelineCallBackHistory: ProjectPipelineCallBackHistory
    ) {
        with(projectPipelineCallBackHistory) {
            projectPipelineCallbackHistoryDao.create(
                dslContext = dslContext,
                projectId = projectId,
                callBackUrl = callBackUrl,
                events = events,
                status = status,
                errorMsg = errorMsg,
                requestHeaders = requestHeaders?.let { JsonUtil.toJson(it, formatted = false) },
                requestBody = requestBody,
                responseCode = responseCode,
                responseBody = responseBody,
                startTime = startTime,
                endTime = endTime,
                id = id
            )
        }
    }

    fun getHistory(
        userId: String,
        projectId: String,
        id: Long
    ): ProjectPipelineCallBackHistory? {
        val record = projectPipelineCallbackHistoryDao.get(dslContext, projectId, id) ?: return null
        return projectPipelineCallbackHistoryDao.convert(record)
    }

    fun listHistory(
        userId: String,
        projectId: String,
        callBackUrl: String,
        events: String,
        startTime: Long?,
        endTime: Long?,
        offset: Int,
        limit: Int
    ): SQLPage<ProjectPipelineCallBackHistory> {
        checkParam(userId, projectId)
        // 验证用户是否有权限查看
        validAuth(userId, projectId)
        var startTimeTemp = startTime
        if (startTimeTemp == null) {
            startTimeTemp = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).timestampmilli()
        }
        var endTimeTemp = endTime
        if (endTimeTemp == null) {
            endTimeTemp = LocalDateTime.of(LocalDate.now(), LocalTime.MAX).timestampmilli()
        }
        val url = projectPipelineCallBackUrlGenerator.encodeCallbackUrl(url = callBackUrl)
        logger.info("list callback history param|$projectId|$events|$startTimeTemp|$endTimeTemp|$url")
        val count = projectPipelineCallbackHistoryDao.count(
            dslContext = dslContext,
            projectId = projectId,
            callBackUrl = url,
            events = events,
            startTime = startTimeTemp,
            endTime = endTimeTemp
        )
        val records = projectPipelineCallbackHistoryDao.list(
            dslContext = dslContext,
            projectId = projectId,
            callBackUrl = url,
            events = events,
            startTime = startTimeTemp,
            endTime = endTimeTemp,
            offset = offset,
            limit = limit
        )
        return SQLPage(
            count,
            records.map {
                projectPipelineCallbackHistoryDao.convert(it)
            }
        )
    }

    fun retry(
        userId: String,
        projectId: String,
        id: Long
    ) {
        checkParam(userId, projectId)
        validProjectManager(userId, projectId)
        val record = getHistory(userId, projectId, id) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_CALLBACK_HISTORY_NOT_FOUND,
            params = arrayOf(id.toString())
        )

        val requestBuilder = Request.Builder()
            .url(record.callBackUrl)
            .post(RequestBody.create(JSON, record.requestBody))
        record.requestHeaders?.filter {
            it.name != TraceTag.TRACE_HEADER_DEVOPS_BIZID
        }?.forEach {
            requestBuilder.addHeader(it.name, it.value)
        }
        val request = requestBuilder.header(TraceTag.TRACE_HEADER_DEVOPS_BIZID, TraceTag.buildBiz()).build()

        val startTime = System.currentTimeMillis()
        var responseCode: Int? = null
        var responseBody: String? = null
        var errorMsg: String? = null
        var status = ProjectPipelineCallbackStatus.SUCCESS
        try {
            OkhttpUtils.doHttp(request).use { response ->
                if (response.code != 200) {
                    logger.warn("[${record.projectId}]|CALL_BACK|url=${record.callBackUrl}| code=${response.code}")
                    throw ErrorCodeException(
                        statusCode = response.code,
                        errorCode = ProcessMessageCode.ERROR_CALLBACK_REPLY_FAIL
                    )
                } else {
                    logger.info("[${record.projectId}]|CALL_BACK|url=${record.callBackUrl}| code=${response.code}")
                }
                responseCode = response.code
                responseBody = response.body?.string()
                errorMsg = response.message
            }
        } catch (e: Exception) {
            logger.error("[$projectId]|[$userId]|CALL_BACK|url=${record.callBackUrl} error", e)
            errorMsg = e.message
            status = ProjectPipelineCallbackStatus.FAILED
        } finally {
            createHistory(
                ProjectPipelineCallBackHistory(
                    projectId = projectId,
                    callBackUrl = record.callBackUrl,
                    events = record.events,
                    status = status.name,
                    errorMsg = errorMsg,
                    requestHeaders = request.headers.names().map {
                        CallBackHeader(
                            name = it,
                            value = request.header(it) ?: ""
                        )
                    },
                    requestBody = record.requestBody,
                    responseCode = responseCode,
                    responseBody = responseBody,
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    id = client.get(ServiceAllocIdResource::class)
                        .generateSegmentId("PROJECT_PIPELINE_CALLBACK_HISTORY").data
                )
            )
        }
    }

    fun bindPipelineCallBack(
        userId: String,
        projectId: String,
        pipelineId: String,
        callbackInfo: PipelineCallbackEvent
    ) {
        // 验证用户是否可以编辑流水线
        pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT
        )
        if (!OkhttpUtils.validUrl(callbackInfo.callbackUrl)) {
            throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_CALLBACK_URL_INVALID)
        }
        val callBackUrl = projectPipelineCallBackUrlGenerator.generateCallBackUrl(
            region = callbackInfo.region,
            url = callbackInfo.callbackUrl
        )
        callbackInfo.callbackUrl = callBackUrl
        val model = pipelineRepositoryService.getModel(projectId, pipelineId) ?: return
        val newEventMap = mutableMapOf<String, PipelineCallbackEvent>()

        if (model.events?.isEmpty() == true) {
            newEventMap[callbackInfo.callbackName] = callbackInfo
        } else {
            newEventMap.putAll(model.events!!)
            // 若key存在会覆盖原来的value,否则就是追加新key
            newEventMap[callbackInfo.callbackName] = callbackInfo
        }
        model.events = newEventMap
        val newModel = mutableListOf<PipelineModelVersion>()
        newModel.add(
            PipelineModelVersion(
                pipelineId = pipelineId,
                projectId = projectId,
                model = JsonUtil.toJson(model, formatted = false),
                creator = model.pipelineCreator ?: userId
            )
        )
        pipelineRepositoryService.batchUpdatePipelineModel(
            userId = userId,
            pipelineModelVersionList = newModel
        )
    }

    private fun checkParam(
        userId: String,
        projectId: String
    ) {
        if (userId.isBlank()) {
            throw ParamBlankException("invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
    }

    private fun validAuth(userId: String, projectId: String) {
        if (!authProjectApi.checkProjectUser(userId, pipelineAuthServiceCode, projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.USER_NEED_PROJECT_X_PERMISSION,
                params = arrayOf(userId, projectId)
            )
        }
    }

    private fun validProjectManager(userId: String, projectId: String) {
        if (!authProjectApi.checkProjectManager(userId, pipelineAuthServiceCode, projectId)) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PERMISSION_NOT_PROJECT_MANAGER,
                params = arrayOf(userId, projectId)
            )
        }
    }

    private fun projectPipelineListUrl(projectId: String) =
        "${HomeHostUtil.innerServerHost()}/console/pipeline/$projectId/list/allPipeline"
}
