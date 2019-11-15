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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bk.codecc.defect.service.impl

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.defect.constant.DefectConstants.*
import com.tencent.bk.codecc.defect.model.SnapShotEntity
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.service.PipelineService
import com.tencent.bk.codecc.defect.service.RedLineReportService
import com.tencent.bk.codecc.defect.service.SnapShotService
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.api.ServiceToolRestResource
import com.tencent.bk.codecc.task.vo.TaskBaseVO
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.plugin.codecc.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccBuildInfo
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.process.api.ServiceBuildResource
import com.tencent.devops.quality.api.v2.ExternalQualityResource
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.request.MetadataCallback
import com.tencent.devops.repository.api.ExternalCodeccRepoResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineServiceImpl @Autowired constructor(
        private val client: Client,
        private val snapShotService: SnapShotService,
        private val redLineReportService: RedLineReportService,
        private val objectMapper: ObjectMapper
) : PipelineService {

    override fun getFileContent(taskId: Long, repoId: String, filePath: String,
                                reversion: String, branch: String?, subModule: String): String? {
        if (repoId.isNullOrBlank() || reversion.isNullOrBlank()) {
            return null
        }

        val fileContentResult = client.get(ExternalCodeccRepoResource::class.java).getFileContent(repoId, filePath, reversion,
                branch, subModule, RepositoryType.ID)
        if (fileContentResult.isNotOk()) {
            logger.error("get file content fail!")
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        return fileContentResult.data
    }


    override fun getBuildIdInfo(buildId: String): CodeccBuildInfo? {
        val buildInfoResult = client.get(ServiceCodeccResource::class.java).getCodeccBuildInfo(setOf(buildId))
        return if (buildInfoResult.isOk() && null != buildInfoResult.data &&
                buildInfoResult.data!!.containsKey(buildId)) {
            buildInfoResult.data!![buildId]
        } else null
    }

    override fun handleDevopsCallBack(tasklog: TaskLogEntity, taskStep: TaskLogEntity.TaskUnit, toolName: String) {
        val taskId = tasklog.taskId
        val pipelineId = tasklog.pipelineId
        val buildId = tasklog.buildId
        if (pipelineId.isNullOrBlank() || buildId.isNullOrBlank()) {
            logger.info("pipeline id or build id of task[{}] is empty", taskId)
            return
        }

        val resultStatus = getResultStatus(taskStep)

        if (null == resultStatus) {
            logger.info("analyze task not finish yet", taskId)
            return
        }

        val resultMessage = if (resultStatus != "success") taskStep.msg else ""
        val taskResult = client.get(ServiceTaskRestResource::class.java).getTaskInfoById(taskId)
        if (taskResult.isNotOk() || taskResult.data == null) {
            logger.error("get task info fail!, task id: {}", taskId)
            throw CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL)
        }
        val taskDetail = taskResult.data!!

        //如果不是从持续集成创建的，则返回
        if (taskDetail.projectId.isNullOrBlank()) {
            logger.info("task id[{}] is not created from pipeline! build id is {}", taskId, buildId)
            return
        }

        val snapShotEntity = snapShotService.saveToolBuildSnapShot(taskId, taskDetail.projectId, taskDetail.pipelineId, buildId, resultStatus,
                resultMessage, toolName)

        val effectiveTools = taskDetail.toolConfigInfoList.filter { toolConfigInfoVO ->
            toolConfigInfoVO.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value()
        }.map { toolConfigInfoVO ->
            toolConfigInfoVO.toolName
        }

        //如果接入的工具没有全部生成快照，就不需要发送给蓝盾
        if (!isAllToolsComplete(snapShotEntity, effectiveTools)) {
            logger.info("not all tool completed! build id is {}", buildId)
            return
        }

        logger.info("all tool completed! ready to send report! build id is {}", buildId)
        val toolOrderResult = client.get(ServiceToolRestResource::class.java).findToolOrder()
        if (toolOrderResult.isOk() && null != toolOrderResult.data) {
            val toolOrder = toolOrderResult.data!!.split(",")
            snapShotEntity.toolSnapshotList.sortBy { toolOrder.indexOf(it.toolNameEn) }
        }

        // 发送产出物报告
        uploadAnalyseSnapshot(snapShotEntity)

        // 上报红线指标数据
        uploadRedLineIndicators(snapShotEntity, taskDetail, effectiveTools)
    }


    override fun stopRunningTask(projectId: String, pipelineId: String, taskId: Long?, buildId: String, userName: String){
        logger.info("execute pipeline task! task id: $taskId")
        if (projectId.isNullOrBlank() || pipelineId.isNullOrBlank() || null == taskId) {
            logger.error("task not exists! task id is: {}", taskId)
            throw CodeCCException(
                    errorCode = CommonMessageCode.RECORD_NOT_EXITS,
                    params = arrayOf("任务参数"),
                    errorCause = null)
        }

        //停止流水线
        val shutdownResult = client.get(ServiceBuildResource::class.java).manualShutdown(userName, projectId,
                pipelineId, buildId, ChannelCode.CODECC)
        if (shutdownResult.isNotOk() || null == shutdownResult.data || shutdownResult.data != true) {
            logger.error("shut down pipeline fail! project id: {}, pipeline id: {}, build id: {}, msg: {}", projectId,
                    pipelineId, buildId, shutdownResult.message)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
//        updateTaskAbortStep(taskBaseVO.nameEn, toolName, taskLogVO, "任务被手动中断")
    }


    /**
     * 发送产出物报告
     */
    private fun uploadAnalyseSnapshot(snapShotEntity: SnapShotEntity) {
        val codeccCallback = CodeccCallback(
                projectId = snapShotEntity.projectId,
                pipelineId = snapShotEntity.pipelineId,
                taskId = snapShotEntity.taskId.toString(),
                buildId = snapShotEntity.buildId,
                toolSnapshotList = objectMapper.readValue(objectMapper.writeValueAsString(snapShotEntity.toolSnapshotList), object : TypeReference<List<Map<String, Any>>>() {})
        )
        val callbackResult = client.get(ServiceCodeccResource::class.java).callback(codeccCallback)
        if (callbackResult.isOk()) {
            logger.info("asynchronous post landun coverity result status success!")
        } else {
            logger.info("asynchronous post landun coverity result status failed!")
        }
    }


    /**
     * 上报红线质量数据
     */
    private fun uploadRedLineIndicators(snapShotEntity: SnapShotEntity, taskDetail: TaskBaseVO, effectiveTools: List<String>) {
        // 上报红线指标数据
        val redLineIndicators = redLineReportService.getRedLineIndicators(taskDetail, effectiveTools)
        val metadataCallback = MetadataCallback(
                elementType = redLineIndicators.elementType,
                data = redLineIndicators.data.map {
                    MetadataCallback.CallbackHisMetadata(
                            enName = it.enName,
                            cnName = it.cnName,
                            detail = it.detail,
                            type = QualityDataType.valueOf(it.type.toUpperCase()),
                            msg = it.msg,
                            value = it.value,
                            extra = it.extra
                    )
                }
        )
        val callbackResult = client.get(ExternalQualityResource::class.java).metadataCallback(snapShotEntity.projectId, snapShotEntity.pipelineId,
                snapShotEntity.buildId, metadataCallback)
        if (callbackResult.isOk()) {
            logger.info("upload red line indicators success!")
        } else {
            logger.info("upload red line indicators failed!")
        }
    }


    /**
     * 判断所有工具是否都已经生成快照
     */
    private fun isAllToolsComplete(snapShot: SnapShotEntity, effectiveTools: List<String>): Boolean {
        return effectiveTools.filterNot { effectiveTool ->
            snapShot.toolSnapshotList.any { toolSnapShotEntity ->
                toolSnapShotEntity.toolNameEn == effectiveTool
            }
        }.isNullOrEmpty()
    }


    /**
     * 获取结果状态
     */
    private fun getResultStatus(taskStep: TaskLogEntity.TaskUnit): String? {
        return if (taskStep.flag == TASK_FLAG_FAIL || taskStep.flag == TASK_FLAG_ABORT) {
            ComConstants.RDMCoverityStatus.failed.name
        } else if (taskStep.flag == TASK_FLAG_SUCC && taskStep.endTime != 0L) {
            if (taskStep.stepNum == ComConstants.Step4MutliTool.DEFECT_SUBMI.value()) {
                ComConstants.RDMCoverityStatus.success.name
            } else {
                null
            }
        } else {
            null
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}