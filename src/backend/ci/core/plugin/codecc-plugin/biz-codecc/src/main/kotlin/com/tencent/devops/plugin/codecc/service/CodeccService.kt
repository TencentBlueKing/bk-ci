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

package com.tencent.devops.plugin.codecc.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_VIEW_DETAILS
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.plugin.codecc.config.CodeccScriptConfig
import com.tencent.devops.plugin.codecc.dao.PluginCodeccDao
import com.tencent.devops.plugin.codecc.pojo.BlueShieldRequest
import com.tencent.devops.plugin.codecc.pojo.BlueShieldResponse
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import java.text.SimpleDateFormat
import java.util.Date
import javax.ws.rs.NotFoundException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class CodeccService @Autowired constructor(
    private val client: Client,
    private val pluginCodeccDao: PluginCodeccDao,
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CodeccService::class.java)
    }

    @Value("\${codecc.host:#{null}}")
    private lateinit var codeccHost: String

    @Value("\${devopsGateway.devnet:}")
    private lateinit var devopsDevnetGateway: String

    @Value("\${codeccGateway.scriptName:build_tool_external_dev.py}")
    private lateinit var codeccScript: String

    @Value("\${codeccGateway.api.fileSize:}")
    private lateinit var fileSizePath: String

    @Value("\${codeccGateway.api.script:}")
    private lateinit var scriptPath: String

    fun getCodeccTaskByProject(
        beginDate: Long?,
        endDate: Long?,
        projectIds: Set<String>
    ): Map<String/*projectId*/, BlueShieldResponse.Item> {
        logger.info("projectIds: $projectIds")
        val taskCallbacksTemp = pluginCodeccDao.getCallbackByProject(dslContext, projectIds)
        val taskCallbacks = taskCallbacksTemp?.filter { !it.taskId.isNullOrBlank() }?.map {
            val toolSnapshotList = objectMapper.readValue<List<Map<String, Any>>>(it.toolSnapshotList)
            CodeccCallback(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                taskId = it.taskId,
                buildId = it.buildId,
                toolSnapshotList = toolSnapshotList
            )
        } ?: listOf()
        val taskProjMap = taskCallbacks.associate { it.taskId to it.projectId }
        return getCodeccTask(taskIds = taskProjMap.keys, beginDate = beginDate, endDate = endDate)
            .associateBy {
                (taskProjMap[it.taskId] ?: "")
            }
    }

    fun getCodeccTaskByPipeline(
        beginDate: Long?,
        endDate: Long?,
        pipelineIds: Set<String>
    ): Map<String/*pipelineId*/, BlueShieldResponse.Item> {
        val taskCallbacks =
            pluginCodeccDao.getCallbackByPipeline(dslContext, pipelineIds)?.filter { !it.taskId.isNullOrBlank() }?.map {
                val toolSnapshotList = objectMapper.readValue<List<Map<String, Any>>>(it.toolSnapshotList)
                CodeccCallback(
                    projectId = it.projectId,
                    pipelineId = it.pipelineId,
                    taskId = it.taskId,
                    buildId = it.buildId,
                    toolSnapshotList = toolSnapshotList
                )
            } ?: listOf()
        val taskPipelineMap = taskCallbacks.associate { it.taskId to it.pipelineId }
        return getCodeccTask(taskIds = taskPipelineMap.keys, beginDate = beginDate, endDate = endDate)
            .associateBy {
                (taskPipelineMap[it.taskId] ?: "")
            }
    }

    fun getCodeccBuildInfo(buildId: Set<String>): Map<String/*buildId*/, CodeccBuildInfo> {
        val resultMap = mutableMapOf<String, CodeccBuildInfo>()
        val buildNoMap = client.get(ServicePipelineResource::class).getBuildNoByBuildIds(buildId).data ?: mapOf()
        val buildInfoMap = client.get(ServiceBuildResource::class).batchServiceBasic(buildId).data ?: mapOf()
        buildInfoMap.values.groupBy { it.projectId }.forEach { (projectId, infoList) ->
            val buildStatusList = client.get(ServiceBuildResource::class).getBatchBuildStatus(
                projectId = projectId,
                buildId = infoList.map { it.buildId }.toSet(),
                channelCode = ChannelCode.BS
            ).data
                ?: throw NotFoundException("no build status buildId($buildId)")
            resultMap.putAll(buildStatusList.map {
                it.id to CodeccBuildInfo(
                    buildNo = buildNoMap[it.id] ?: "",
                    buildTime = it.startTime,
                    buildUser = it.userId
                )
            })
        }
        return resultMap
    }

    fun callback(callback: CodeccCallback): String {
        // 标识完成
        val key = "code_cc_${callback.projectId}_${callback.pipelineId}_${callback.buildId}_done"
        redisOperation.set(key, callback.taskId, 300)
        // 落表
        return pluginCodeccDao.saveCallback(dslContext, callback).toString()
    }

    fun getCodeccReport(buildId: String): CodeccCallback? {
        val callback = pluginCodeccDao.getCallback(dslContext, buildId) ?: return null
        val toolSnapshotList = objectMapper.readValue<List<Map<String, Any>>>(callback.toolSnapshotList)
        return CodeccCallback(
            projectId = callback.projectId,
            pipelineId = callback.pipelineId,
            taskId = callback.taskId,
            buildId = callback.buildId,
            toolSnapshotList = toolSnapshotList
        )
    }

    fun saveCodeccTask(projectId: String, pipelineId: String, buildId: String): Int {
        val callback =
            CodeccCallback(projectId, pipelineId, "", buildId, listOf())
        return pluginCodeccDao.saveCallback(dslContext, callback)
    }

    private fun getCodeccBlueShield(request: BlueShieldRequest): BlueShieldResponse {
        val mediaType = "application/json".toMediaTypeOrNull()
        val json = objectMapper.writeValueAsString(request)
        val requestBody = RequestBody.create(mediaType, json)
        val url = "http://$codeccHost/blueShield/dataMeasure"
        val httpReq = Request.Builder()
            .url(url)
            .header("key", "blueShieldDataMeasure")
            .post(requestBody)
            .build()
        logger.info("codecc blue shield url: $url")
        logger.info("codecc blue shield request: $json")

        try {
            OkhttpUtils.doHttp(httpReq).use { response ->
                val body = response.body!!.string()
                logger.info("codecc blueShield response: $body")
                if (!response.isSuccessful) {
                    throw RemoteServiceException(
                        errorCode = response.code,
                        errorMessage = "get codecc blueShield response fail $body"
                    )
                }
                return objectMapper.readValue(body, BlueShieldResponse::class.java)
            }
        } catch (e: Exception) {
            logger.info("error getCodeccBlueShield: ${e.message}")
            return BlueShieldResponse(0, "fail", listOf())
        }
    }

    fun queryCodeccTaskDetailUrl(projectId: String, pipelineId: String, buildId: String): String {
        val taskId = redisOperation.get("code_cc_${projectId}_${pipelineId}_${buildId}_done")
        return if (taskId != null && taskId != "" && taskId != "null") {
            "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/procontrol/prodesc/" +
                "?proj_id=$taskId'>${I18nUtil.getCodeLanMessage(BK_VIEW_DETAILS)}</a>"
        } else ""
    }

    fun getCodeccTaskResult(
        beginDate: Long?,
        endDate: Long?,
        pipelineIds: Set<String>
    ): Map<String/*pipelineId*/, CodeccCallback> {
        val result = pluginCodeccDao.getCallbackByPipeline(dslContext, pipelineIds)?.map { callback ->
            val toolSnapshotList = objectMapper.readValue<List<Map<String, Any>>>(callback.toolSnapshotList)
            CodeccCallback(
                projectId = callback.projectId,
                pipelineId = callback.pipelineId,
                taskId = callback.taskId,
                buildId = callback.buildId,
                toolSnapshotList = toolSnapshotList
            )
        } ?: listOf()
        val codeccTasks = getCodeccTask(result.map { it.taskId }, beginDate, endDate)
        val resultMap = result.associateBy { it.taskId }
        val taskPipelineMap = result.associate { it.taskId to it.pipelineId }
        return codeccTasks.associate {
            (taskPipelineMap[it.taskId] ?: "") to (resultMap[it.taskId] ?: CodeccCallback())
        }
    }

    fun getCodeccTaskResultByBuildIds(buildIds: Set<String>): Map<String, CodeccCallback> {
        return pluginCodeccDao.getCallbackByBuildId(dslContext, buildIds).map { callback ->
            val toolSnapshotList = objectMapper.readValue<List<Map<String, Any>>>(callback.toolSnapshotList)
            callback.buildId to CodeccCallback(
                projectId = callback.projectId,
                pipelineId = callback.pipelineId,
                taskId = callback.taskId,
                buildId = callback.buildId,
                toolSnapshotList = toolSnapshotList
            )
        }.toMap()
    }

    private fun getCodeccTask(
        taskIds: Collection<String>,
        beginDate: Long?,
        endDate: Long?
    ): List<BlueShieldResponse.Item> {
        val formatter = SimpleDateFormat("YYYY-MM-dd")
        val blueShieldRequest = if (beginDate != null && endDate != null) {
            BlueShieldRequest(taskIds, formatter.format(beginDate), formatter.format(Date()))
        } else {
            BlueShieldRequest(taskIds, "2018-01-01", formatter.format(Date()))
        }
        val response = getCodeccBlueShield(blueShieldRequest)
        return response.data
    }

    fun getSingleCodeccScriptConfig(): CodeccScriptConfig {
        return CodeccScriptConfig(
            devnetHost = devopsDevnetGateway,
            scriptFileName = codeccScript,
            fileSizeUrl = fileSizePath,
            downloadUrl = scriptPath
        )
    }
}
