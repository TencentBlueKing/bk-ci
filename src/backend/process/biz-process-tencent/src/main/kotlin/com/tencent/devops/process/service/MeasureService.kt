package com.tencent.devops.process.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.event.pojo.measure.MeasureRequest
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.process.engine.dao.PipelineBuildVarDao
import com.tencent.devops.process.engine.pojo.event.PipelineBuildCancelEvent
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.pojo.measure.PipelineBuildData
import com.tencent.devops.process.service.measure.MeasureEventDispatcher
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_PIPELINE_ID
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MeasureService @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val templateService: TemplateService,
    private val pipelineBuildVarDao: PipelineBuildVarDao,
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val measureEventDispatcher: MeasureEventDispatcher
) {

    @Value("\${gateway.service:#{null}}")
    private val serviceGateway: String? = null

    fun postPipelineData(
        projectId: String,
        pipelineId: String,
        buildId: String,
        startTime: Long,
        startType: String,
        username: String,
        buildStatus: BuildStatus,
        buildNum: Int,
        model: Model?
    ) {
        try {
            if (model == null) {
                logger.warn("The pipeline.json is not exist of pipeline($pipelineId)")
                return
            }

            val json = JsonUtil.getObjectMapper().writeValueAsString(model)

            val variable = pipelineBuildVarDao.getVars(dslContext, buildId)
            val metaInfo = mapOf(
                "parentPipelineId" to (variable[PIPELINE_START_PARENT_PIPELINE_ID] ?: ""),
                "parentBuildId" to (variable[PIPELINE_START_PARENT_BUILD_ID] ?: "")
            )

            val templateId = getTemplateId(pipelineId)
            val data = PipelineBuildData(
                projectId, pipelineId, templateId, buildId, startTime, System.currentTimeMillis(),
                StartType.toStartType(startType), username, false, buildStatus, json, buildNum, metaInfo
            )
            val url = "http://$serviceGateway/measure/api/service/pipelines/addData"

            val requestBody = objectMapper.writeValueAsString(data)
            measureEventDispatcher.dispatch(MeasureRequest(projectId, pipelineId, buildId, url, requestBody))
        } catch (t: Throwable) {
            logger.warn("Fail to post the pipeline measure data of build($buildId)", t)
        }
    }

    fun onCancelNew(event: PipelineBuildCancelEvent) {
        try {
            val tasks = pipelineRuntimeService.getAllBuildTask(event.buildId)
            if (tasks.isEmpty()) {
                return
            }
            tasks.forEach { task ->
                with(task) {
                    if (BuildStatus.isRunning(status)) {
                        val tStartTime = startTime?.timestampmilli() ?: 0
                        val atomCode = task.taskParams["atomCode"] as String? ?: ""
                        postElementDataNew(
                            event.projectId, pipelineId, taskId, atomCode, taskName,
                            event.buildId, tStartTime, BuildStatus.CANCELED, taskType, executeCount
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Fail to post the cancel event elements of ${event.buildId}", e)
        }
    }

    fun postElementDataNew(
        projectId: String,
        pipelineId: String,
        taskId: String,
        atomCode: String,
        name: String,
        buildId: String,
        startTime: Long,
        status: BuildStatus,
        type: String,
        executeCount: Int?,
        extraInfo: Map<String, Any>? = null
    ) {
        try {
            val url = "http://$serviceGateway/measure/api/service/elements/addData"

            val requestMap = mutableMapOf(
                "id" to taskId,
                "name" to name,
                "pipelineId" to pipelineId,
                "templateId" to getTemplateId(pipelineId),
                "projectId" to projectId,
                "buildId" to buildId,
                "atomCode" to atomCode,
                "status" to status,
                "beginTime" to startTime,
                "endTime" to System.currentTimeMillis(),
                "type" to type
            )
            if (extraInfo != null && extraInfo.isNotEmpty()) {
                val extraInfoStr = ObjectMapper().writeValueAsString(extraInfo)
                requestMap["extraInfo"] = extraInfoStr
            }
            val requestBody = ObjectMapper().writeValueAsString(requestMap)
            logger.info("add the element data, request data: $requestMap")
            measureEventDispatcher.dispatch(MeasureRequest(projectId, pipelineId, buildId, url, requestBody))
        } catch (e: Throwable) {
            logger.error("Fail to add the element data, $e")
        }
    }

    private fun getTemplateId(pipelineId: String): String {
        return templateService.listPipelineTemplate(setOf(pipelineId))?.firstOrNull()?.templateId ?: ""
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MeasureService::class.java)
    }
}
