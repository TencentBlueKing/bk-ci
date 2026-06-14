package com.tencent.devops.process.service.task

import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.dao.yaml.PipelineYamlInfoDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskType
import com.tencent.devops.process.pojo.pipeline.task.PipelineBatchTaskDetail
import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskDetailStatus
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBatchTaskFactory @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineYamlInfoDao: PipelineYamlInfoDao,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService
) {

    fun buildBatchTaskDetail(
        projectId: String,
        taskId: String,
        taskType: PipelineBatchTaskType,
        status: PipelineBatchTaskDetailStatus,
        pipelineIds: List<String>,
        subPipeline: Boolean = false
    ): List<PipelineBatchTaskDetail> {
        val pipelineInfos = pipelineInfoDao.listInfoByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds.toSet()
        )
        val pipelineNameMap = pipelineInfos.associate { it.pipelineId to it.pipelineName }
        val pipelineCreatorMap = pipelineInfos.associate { it.pipelineId to it.creator }
        val pipelineLockedMap = pipelineInfos.associate { it.pipelineId to (it.locked ?: false) }
        val pipelineVersionStatusMap = pipelineInfos.associate {
            it.pipelineId to it.latestVersionStatus?.let { status -> VersionStatus.valueOf(status) }
        }
        val pipelineEnablePacSet = pipelineYamlInfoDao.listByPipelineIds(
            dslContext = dslContext,
            projectId = projectId,
            pipelineIds = pipelineIds
        ).map { it.pipelineId }.toSet()
        val pipelineConstraintSet = pipelineTemplateRelatedService.listByPipelineIds(
            projectId = projectId,
            pipelineIds = pipelineIds.toSet()
        ).filter { it.instanceType == PipelineInstanceTypeEnum.CONSTRAINT }
            .map { it.pipelineId }
            .toSet()
        return pipelineIds.map { pipelineId ->
            PipelineBatchTaskDetail(
                taskId = taskId,
                projectId = projectId,
                taskType = taskType,
                pipelineId = pipelineId,
                pipelineName = pipelineNameMap[pipelineId].orEmpty(),
                pipelineCreator = pipelineCreatorMap[pipelineId].orEmpty(),
                pac = pipelineEnablePacSet.contains(pipelineId),
                constraint = pipelineConstraintSet.contains(pipelineId),
                subPipeline = subPipeline,
                locked = pipelineLockedMap[pipelineId] ?: false,
                versionStatus = pipelineVersionStatusMap[pipelineId],
                change = true,
                status = status,
                errorType = null,
                errorMessage = null,
                startTime = null,
                endTime = null
            )
        }
    }
}
