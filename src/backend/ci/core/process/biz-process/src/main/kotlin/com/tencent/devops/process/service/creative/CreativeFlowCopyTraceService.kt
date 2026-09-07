package com.tencent.devops.process.service.creative

import com.tencent.devops.process.engine.dao.creative.CreativeFlowCopyTrace
import com.tencent.devops.process.engine.dao.creative.PipelineShareCopyTraceDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.creative.CreativeFlowCopyTraceVo
import com.tencent.devops.process.utils.CreativeFlowVersionNumUtil
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CreativeFlowCopyTraceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineShareCopyTraceDao: PipelineShareCopyTraceDao,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    fun record(trace: CreativeFlowCopyTrace): Long {
        return pipelineShareCopyTraceDao.add(dslContext, trace)
    }

    /**
     * 命中溯源且目标流水线在当前空间真实存在，才算"已建过副本"
     */
    fun getLatestAlive(
        targetProjectId: String,
        shareId: String,
        flowId: String
    ): CreativeFlowCopyTrace? {
        val trace = pipelineShareCopyTraceDao.getLatestByTargetShare(
            dslContext, targetProjectId, shareId, flowId
        ) ?: return null
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(
            projectId = targetProjectId,
            pipelineId = trace.targetPipelineId
        )
        return if (pipelineInfo != null) trace else null
    }

    fun listByTargetProject(
        targetProjectId: String,
        shareId: String?,
        flowId: String?,
        targetPipelineId: String?
    ): List<CreativeFlowCopyTraceVo> {
        return pipelineShareCopyTraceDao.listByTargetProject(
            dslContext, targetProjectId, shareId, flowId, targetPipelineId
        ).map { toVo(it) }
    }

    private fun toVo(trace: CreativeFlowCopyTrace): CreativeFlowCopyTraceVo {
        return CreativeFlowCopyTraceVo(
            id = trace.id ?: 0L,
            shareId = trace.shareId,
            flowId = trace.flowId,
            scene = trace.scene,
            shareMode = trace.shareMode,
            talentCode = trace.talentCode,
            sourceProjectId = trace.sourceProjectId,
            sourcePipelineId = trace.sourcePipelineId,
            sourceVersion = trace.sourceVersion,
            sourceVersionNum = trace.sourceVersionNum?.let { CreativeFlowVersionNumUtil.format(it) },
            targetProjectId = trace.targetProjectId,
            targetPipelineId = trace.targetPipelineId,
            targetPipelineName = trace.targetPipelineName,
            targetVersion = trace.targetVersion,
            targetVersionNum = trace.targetVersionNum?.let { CreativeFlowVersionNumUtil.format(it) },
            targetEnvHashId = trace.targetEnvHashId,
            copyAction = trace.copyAction,
            variableOverrides = trace.variableOverrides,
            operator = trace.operator,
            createTime = trace.createTime ?: 0L
        )
    }
}
