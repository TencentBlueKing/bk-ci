package com.tencent.devops.plugin.codecc.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.plugin.codecc.dao.PluginCodeccDao
import com.tencent.devops.plugin.codecc.pojo.BlueShieldRequest
import com.tencent.devops.plugin.codecc.pojo.BlueShieldResponse
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.process.api.ServiceMetadataResource
import com.tencent.devops.process.api.ServicePipelineResource
import com.tencent.devops.process.pojo.Property
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.Date

@Service
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
                it.projectId,
                it.pipelineId,
                it.taskId,
                it.buildId,
                toolSnapshotList
            )
        } ?: listOf()
        val taskProjMap = taskCallbacks.map { it.taskId to it.projectId }.toMap()
        return getCodeccTask(taskProjMap.keys, beginDate, endDate).map {
            (taskProjMap[it.taskId] ?: "") to it
        }.toMap()
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
                    it.projectId,
                    it.pipelineId,
                    it.taskId,
                    it.buildId,
                    toolSnapshotList
                )
            } ?: listOf()
        val taskPipelineMap = taskCallbacks.map { it.taskId to it.pipelineId }.toMap()
        return getCodeccTask(taskPipelineMap.keys, beginDate, endDate).map {
            (taskPipelineMap[it.taskId] ?: "") to it
        }.toMap()
    }

    fun getCodeccBuildInfo(buildId: Set<String>): Map<String/*buildId*/, CodeccBuildInfo> {
        val resultMap = mutableMapOf<String, CodeccBuildInfo>()
        val buildNoMap = client.get(ServicePipelineResource::class).getBuildNoByBuildIds(buildId).data ?: mapOf()
        val buildInfoMap = client.get(ServiceBuildResource::class).batchServiceBasic(buildId).data ?: mapOf()
        buildInfoMap.values.groupBy { it.projectId }.forEach { projectId, infoList ->
            val buildStatusList = client.get(ServiceBuildResource::class).getBatchBuildStatus(
                projectId,
                infoList.map { it.buildId }.toSet(),
                ChannelCode.BS
            ).data
                ?: throw RuntimeException("no build status buildId($buildId)")
            resultMap.putAll(buildStatusList.map {
                it.id to CodeccBuildInfo(
                    buildNoMap[it.id] ?: "",
                    it.startTime,
                    it.userId
                )
            })
        }
        return resultMap
    }

    fun callback(callback: CodeccCallback): String {
        // 创建元数据
        try {
            logger.info("codecc callback: " + callback.toString())
            val toolSnapshot = callback.toolSnapshotList[0]
            val metadatas = mutableListOf<Property>()
            // 遗留告警数
            metadatas.add(Property("codecc.coverity.warning.count", toolSnapshot["total_new"].toString()))
            // 遗留严重告警数
            val serious = if (toolSnapshot["total_new_serious"] is Int) toolSnapshot["total_new_serious"] as Int else 0
            metadatas.add(Property("codecc.coverity.warning.high.count", serious.toString()))
            // 遗留严重+一般告警数
            val normal = if (toolSnapshot["total_new_normal"] is Int) toolSnapshot["total_new_normal"] as Int else 0
            metadatas.add(Property("codecc.coverity.warning.highAndLight.count", (normal + serious).toString()))
            client.get(ServiceMetadataResource::class)
                .create(callback.projectId, callback.pipelineId, callback.buildId, metadatas)
        } catch (e: Exception) {
            logger.error("创建元数据失败: ${e.message}", e)
        }
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
            callback.projectId,
            callback.pipelineId,
            callback.taskId,
            callback.buildId,
            toolSnapshotList
        )
    }

    fun saveCodeccTask(projectId: String, pipelineId: String, buildId: String): Int {
        val callback =
            CodeccCallback(projectId, pipelineId, "", buildId, listOf())
        return pluginCodeccDao.saveCallback(dslContext, callback)
    }

    private fun getCodeccBlueShield(request: BlueShieldRequest): BlueShieldResponse {
        val mediaType = MediaType.parse("application/json")
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
                val body = response.body()!!.string()
                logger.info("codecc blueShield response: $body")
                if (!response.isSuccessful) {
                    throw RuntimeException("get codecc blueShield response fail")
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
        return if (taskId != null && taskId != "" && taskId != "null")
            "<a target='_blank' href='${HomeHostUtil.innerServerHost()}/console/codecc/$projectId/procontrol/prodesc/?proj_id=$taskId'>查看详情</a>"
        else ""
    }

    fun getCodeccTaskResult(
        beginDate: Long?,
        endDate: Long?,
        pipelineIds: Set<String>
    ): Map<String/*pipelineId*/, CodeccCallback> {
        val result = pluginCodeccDao.getCallbackByPipeline(dslContext, pipelineIds)?.map { callback ->
            val toolSnapshotList = objectMapper.readValue<List<Map<String, Any>>>(callback.toolSnapshotList)
            CodeccCallback(
                callback.projectId,
                callback.pipelineId,
                callback.taskId,
                callback.buildId,
                toolSnapshotList
            )
        } ?: listOf()
        val codeccTasks = getCodeccTask(result.map { it.taskId }, beginDate, endDate)
        val resultMap = result.map { it.taskId to it }.toMap()
        val taskPipelineMap = result.map { it.taskId to it.pipelineId }.toMap()
        return codeccTasks.map {
            (taskPipelineMap[it.taskId] ?: "") to (resultMap[it.taskId] ?: CodeccCallback())
        }.toMap()
    }

    fun getCodeccTaskResultByBuildIds(buildIds: Set<String>): Map<String, CodeccCallback> {
        return pluginCodeccDao.getCallbackByBuildId(dslContext, buildIds).map { callback ->
            val toolSnapshotList = objectMapper.readValue<List<Map<String, Any>>>(callback.toolSnapshotList)
            callback.buildId to CodeccCallback(
                callback.projectId,
                callback.pipelineId,
                callback.taskId,
                callback.buildId,
                toolSnapshotList
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
            BlueShieldRequest(
                taskIds,
                formatter.format(beginDate),
                formatter.format(Date())
            )
        } else {
            BlueShieldRequest(taskIds, "2018-01-01", formatter.format(Date()))
        }
        val response = getCodeccBlueShield(blueShieldRequest)
        return response.data
    }
}
