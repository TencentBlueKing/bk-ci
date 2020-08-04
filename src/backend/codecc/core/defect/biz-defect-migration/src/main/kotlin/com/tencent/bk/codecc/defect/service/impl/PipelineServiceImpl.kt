/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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
import com.tencent.bk.codecc.defect.dao.mongotemplate.CodeRepoInfoDao
import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.SnapShotEntity
import com.tencent.bk.codecc.defect.model.TaskLogEntity
import com.tencent.bk.codecc.defect.service.PipelineService
import com.tencent.bk.codecc.defect.service.RedLineReportService
import com.tencent.bk.codecc.defect.service.SnapShotService
import com.tencent.bk.codecc.defect.vo.coderepository.CodeRepoVO
import com.tencent.bk.codecc.task.api.ServiceTaskRestResource
import com.tencent.bk.codecc.task.api.ServiceToolRestResource
import com.tencent.bk.codecc.task.enums.EmailType
import com.tencent.bk.codecc.task.pojo.EmailNotifyModel
import com.tencent.bk.codecc.task.pojo.RtxNotifyModel
import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.CodeCCException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.CommonMessageCode
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.mq.EXCHANGE_CODECC_GENERAL_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_CODECC_EMAIL_NOTIFY
import com.tencent.devops.common.web.mq.ROUTE_CODECC_RTX_NOTIFY
import com.tencent.devops.plugin.api.ExternalCodeccResource
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.quality.api.v2.ExternalQualityResource
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.request.MetadataCallback
import com.tencent.devops.repository.api.ExternalCodeccRepoResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import org.apache.commons.collections.CollectionUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.lang.Exception

@Service
open class PipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val snapShotService: SnapShotService,
    private val redLineReportService: RedLineReportService,
    private val rabbitTemplate: RabbitTemplate,
    private val codeRepoInfoDao : CodeRepoInfoDao,
    private val objectMapper: ObjectMapper
) : PipelineService {

    @Value("\${codecc.privatetoken:#{null}}")
    lateinit var codeccToken: String

    override fun getFileContent(
        taskId: Long, repoId: String?, filePath: String,
        reversion: String?, branch: String?, subModule: String?
    ): String? {

        val fileContentResult = if (repoId.isNullOrBlank()) {
            val repoUrl = client.get(ServiceTaskRestResource::class.java).getGongfengRepoUrl(taskId)
            logger.info("gongfeng project url is: ${repoUrl.data}")
            if (repoUrl.isNotOk() || repoUrl.data == null) {
                logger.error("get gongfeng repo url fail!")
                throw CodeCCException(CommonMessageCode.CODE_NORMAL_CONTENT_ERROR)
            }

            var fileContentResp: com.tencent.devops.common.api.pojo.Result<kotlin.String> = com.tencent.devops.common.api.pojo.Result("")
            try {
                fileContentResp = client.getDevopsService(ExternalCodeccRepoResource::class.java).getGitFileContentCommon(
                        repoUrl = repoUrl.data!!,
                        filePath = filePath,
                        ref = "master",
                        //todo 要区分情景
                        token = codeccToken!!
                )
            } catch (e: Exception){
                logger.error("get git file content fail!, repoUrl: {}, filePath: {}, token: {}", repoUrl.data!!, filePath, codeccToken, e)
                throw CodeCCException(CommonMessageCode.CODE_CONTENT_ERROR)
            }
            if(fileContentResp.isNotOk()){
                logger.info("get git file content fail!, repoUrl: {}, filePath: {}, token: {}", repoUrl.data!!, filePath, codeccToken)
                throw CodeCCException(CommonMessageCode.CODE_CONTENT_ERROR)
            }
            fileContentResp
        } else {
            if (reversion.isNullOrBlank()) {
                return null
            }
            var fileContentResp: com.tencent.devops.common.api.pojo.Result<kotlin.String> = com.tencent.devops.common.api.pojo.Result("")
            try {
                fileContentResp = client.getDevopsService(ExternalCodeccRepoResource::class.java).getFileContentV2(
                        repoId, filePath, reversion,
                        branch, subModule ?: "", RepositoryType.ID
                )
            } catch (e: Exception){
                logger.error("get file content v2 fail!, repoId: {}, filePath: {}, reversion: {}, branch: {}, subModule: {}, ", repoId, filePath, reversion,
                        branch, subModule ?: "", e)
            }
            fileContentResp
        }

        if (fileContentResult.isNotOk()) {
            logger.error("get file content fail!")
            throw CodeCCException(CommonMessageCode.CODE_NORMAL_CONTENT_ERROR)
        }
        return fileContentResult.data
    }

    override fun getBuildIdInfo(buildId: String): BuildEntity? {
        val buildInfoResult =
            client.getDevopsService(ServiceCodeccResource::class.java).getCodeccBuildInfo(setOf(buildId))
        if (buildInfoResult.isNotOk() || buildInfoResult.data.isNullOrEmpty()) {
            logger.info("get build info from devops failed! buildId: {}", buildId)
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
        val buildEntity = BuildEntity()
        BeanUtils.copyProperties(buildInfoResult.data!![buildId], buildEntity)
        buildEntity.buildId = buildId
        return buildEntity
    }

    @Async("asyncTaskExecutor")
    override fun handleDevopsCallBack(
        tasklog: TaskLogEntity,
        taskStep: TaskLogEntity.TaskUnit,
        toolName: String,
        taskDetailVO: TaskDetailVO
    ) {
        val taskId = tasklog.taskId
        val pipelineId = tasklog.pipelineId
        val buildId = tasklog.buildId
        if (pipelineId.isNullOrBlank() || buildId.isNullOrBlank()) {
            logger.info("pipeline id or build id of task[{}] is empty", taskId)
            return
        }

        val resultStatus = getResultStatus(taskStep, toolName)

        if (null == resultStatus) {
            logger.info("analyze task not finish yet", taskId)
            return
        }

        val resultMessage = if (resultStatus != "success") taskStep.msg else ""


        val snapShotEntity = snapShotService.saveToolBuildSnapShot(
            taskId, taskDetailVO.projectId, taskDetailVO.pipelineId, buildId, resultStatus,
            resultMessage, toolName
        )

        val effectiveTools = taskDetailVO.toolConfigInfoList.filter { toolConfigInfoVO ->
            toolConfigInfoVO.followStatus != ComConstants.FOLLOW_STATUS.WITHDRAW.value()
        }.map { toolConfigInfoVO ->
            toolConfigInfoVO.toolName
        }

        //如果接入的工具没有全部生成快照，就不需要发送给蓝盾
        if (!isAllToolsComplete(snapShotEntity, effectiveTools)) {
            logger.info("not all tool completed! build id is {}", buildId)
            return
        }

        val emailNotifyModel = EmailNotifyModel(taskId, buildId, EmailType.INSTANT)
        //发送邮件
        rabbitTemplate.convertAndSend(EXCHANGE_CODECC_GENERAL_NOTIFY, ROUTE_CODECC_EMAIL_NOTIFY, emailNotifyModel)
        val rtxNotifyModel = RtxNotifyModel(taskId, resultStatus == ComConstants.RDMCoverityStatus.success.name)
        //发送企业微信
        rabbitTemplate.convertAndSend(EXCHANGE_CODECC_GENERAL_NOTIFY, ROUTE_CODECC_RTX_NOTIFY, rtxNotifyModel)

        logger.info("all tool completed! ready to send report! build id is {}", buildId)

        //如果不是从持续集成创建的，则返回
        if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() != taskDetailVO.createFrom) {
            logger.info("task id[{}] is not created from pipeline! build id is {}", taskId, buildId)
            return
        }

        val toolOrderResult = client.get(ServiceToolRestResource::class.java).findToolOrder()
        if (toolOrderResult.isOk() && null != toolOrderResult.data) {
            val toolOrder = toolOrderResult.data!!.split(",")
            snapShotEntity.toolSnapshotList.sortBy { toolOrder.indexOf(it.toolNameEn) }
        }

        // 发送产出物报告
        uploadAnalyseSnapshot(snapShotEntity)

        // 上报红线指标数据
        uploadRedLineIndicators(snapShotEntity, taskDetailVO, buildId)
    }

    /**
     * 组装pcg回调接口
     */
    /*private fun assembleCallback(snapShotEntity: SnapShotEntity, taskDetailVO: TaskDetailVO) : CustomProjCallbackModel {
        val pcgRdCallBackModel = CustomProjCallbackModel()
        return with(pcgRdCallBackModel){
            taskId = snapShotEntity.taskId
            buildId = snapShotEntity.buildId
            url = taskDetailVO.customProjInfo.url
            if(null != snapShotEntity.toolSnapshotList && snapShotEntity.toolSnapshotList.isNotEmpty()){
                snapShotEntity.toolSnapshotList.forEach {
                    it.defectDetailUrl = null
                    it.defectReportUrl = null
                }
            }
            toolSnapshotList = snapShotEntity.toolSnapshotList
            this
        }
    }*/

    override fun stopRunningTask(
            projectId: String,
            pipelineId: String,
            taskId: Long?,
            buildId: String,
            userName: String,
            nameEn: String
    ) {
        logger.info("execute pipeline task! task id: $taskId")
        if (projectId.isBlank() || pipelineId.isBlank() || null == taskId) {
            logger.error("task not exists! task id is: {}", taskId)
            throw CodeCCException(
                errorCode = CommonMessageCode.RECORD_NOT_EXITS,
                params = arrayOf("任务参数"),
                errorCause = null
            )
        }

        var channelCode = ChannelCode.CODECC_EE
        if (nameEn.startsWith(ComConstants.OLD_CODECC_ENNAME_PREFIX)) {
            channelCode = ChannelCode.CODECC
        }

        //停止流水线
        val shutdownResult = client.getDevopsService(ServiceBuildResource::class.java).manualShutdown(
            userName, projectId, pipelineId, buildId, channelCode)
        if (shutdownResult.isNotOk() || null == shutdownResult.data || shutdownResult.data != true) {
            logger.error(
                "shut down pipeline fail! project id: {}, pipeline id: {}, build id: {}, msg: {}", projectId,
                pipelineId, buildId, shutdownResult.message
            )
            throw CodeCCException(CommonMessageCode.BLUE_SHIELD_INTERNAL_ERROR)
        }
//        updateTaskAbortStep(taskBaseVO.nameEn, toolName, taskLogVO, "任务被手动中断")
    }

    override fun getCodeRepoListByTaskIds(taskIds: Set<Long>, projectId: String): Map<Long, Set<CodeRepoVO>> {

        val codeRepoInfoEntities = codeRepoInfoDao.findFirstByTaskIdOrderByCreatedDate(taskIds)
        val repoResult = client.getDevopsService(ServiceRepositoryResource::class.java).listByProjects(setOf(projectId), 1, 20000)
        val repoList = if(repoResult.isNotOk()) listOf() else repoResult.data?.records?: listOf()
        val repoMap = repoList.associate { it.repositoryHashId to it.aliasName }
        return if (CollectionUtils.isEmpty(codeRepoInfoEntities)) {
            mapOf()
        } else codeRepoInfoEntities.associate {
            it.taskId to if(it.repoList.isEmpty()) setOf() else it.repoList.map { codeRepoEntity ->
                with(codeRepoEntity)
                {
                    CodeRepoVO(repoId, revision, branch, repoMap[repoId])
                }
            }.toSet()
        }
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
            toolSnapshotList = objectMapper.readValue(
                objectMapper.writeValueAsString(snapShotEntity.toolSnapshotList),
                object : TypeReference<List<Map<String, Any>>>() {})
        )
        val callbackResult = client.getDevopsService(ExternalCodeccResource::class.java).callback(codeccCallback)
        if (callbackResult.isOk()) {
            logger.info("asynchronous post landun coverity result status success!")
        } else {
            logger.info("asynchronous post landun coverity result status failed!")
        }
    }

    /**
     * 上报红线质量数据
     */
    private fun uploadRedLineIndicators(
        snapShotEntity: SnapShotEntity,
        taskDetail: TaskDetailVO,
        build: String
    ) {
        // 上报红线指标数据
        val redLineIndicators = redLineReportService.getPipelineCallback(taskDetail, build)
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
        val callbackResult = client.getDevopsService(ExternalQualityResource::class.java).metadataCallback(
            snapShotEntity.projectId, snapShotEntity.pipelineId,
            snapShotEntity.buildId, metadataCallback
        )
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
    private fun getResultStatus(
        taskStep: TaskLogEntity.TaskUnit,
        toolName: String
    ): String? {
        val finishStep = if (ComConstants.Tool.COVERITY.name == toolName || ComConstants.Tool.KLOCWORK.name == toolName){
            ComConstants.Step4Cov.DEFECT_SYNS.value()
        } else{
            ComConstants.Step4MutliTool.COMMIT.value()
        }
        return if (taskStep.flag == ComConstants.StepFlag.FAIL.value() || taskStep.flag == ComConstants.StepFlag.ABORT.value()) {
            ComConstants.RDMCoverityStatus.failed.name
        } else if (taskStep.flag == ComConstants.StepFlag.SUCC.value() && taskStep.endTime != 0L) {
            if (taskStep.stepNum == finishStep) {
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