package com.tencent.devops.process.service.label

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.model.process.tables.records.TPipelineLabelRecord
import com.tencent.devops.process.dao.label.PipelineGroupDao
import com.tencent.devops.process.dao.label.PipelineLabelDao
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineGroupCopyService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineGroupDao: PipelineGroupDao,
    private val pipelineLabelDao: PipelineLabelDao,
    private val pipelineGroupService: PipelineGroupService
) {

    fun copyAcrossProject(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        labelId: String?
    ) {
        if (!labelId.isNullOrBlank()) {
            try {
                pipelineLabelDao.getById(
                    dslContext = dslContext,
                    projectId = sourceProjectId,
                    id = HashUtil.decodeIdToLong(labelId)
                )?.let { sourceLabel ->
                    copySingleLabel(
                        userId = userId,
                        sourceProjectId = sourceProjectId,
                        targetProjectId = targetProjectId,
                        sourceLabel = sourceLabel
                    )
                } ?: logger.warn("get source pipeline label failed|$sourceProjectId|$labelId")
            } catch (ignored: Exception) {
                logger.warn("get source pipeline label failed|$sourceProjectId|$labelId", ignored)
            }
            return
        }
        copyAllLabelsByGroup(userId, sourceProjectId, targetProjectId)
    }

    private fun copyAllLabelsByGroup(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String
    ) {
        pipelineGroupDao.list(dslContext, sourceProjectId).forEach { sourceGroup ->
            pipelineLabelDao.getByGroupIds(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupId = setOf(sourceGroup.id)
            ).forEach { sourceLabel ->
                copySingleLabel(
                    userId = userId,
                    sourceProjectId = sourceProjectId,
                    targetProjectId = targetProjectId,
                    sourceLabel = sourceLabel
                )
            }
        }
    }

    private fun copySingleLabel(
        userId: String,
        sourceProjectId: String,
        targetProjectId: String,
        sourceLabel: TPipelineLabelRecord
    ) {
        try {
            val sourceGroup = pipelineGroupDao.get(
                dslContext = dslContext,
                projectId = sourceProjectId,
                groupId = sourceLabel.groupId
            ) ?: run {
                logger.warn(
                    "get source pipeline label group failed|$sourceProjectId|${sourceLabel.groupId}"
                )
                return
            }
            val targetGroup = getOrCreateTargetGroup(
                userId = userId,
                targetProjectId = targetProjectId,
                groupName = sourceGroup.name
            )
            if (pipelineLabelDao.getByName(
                    dslContext = dslContext,
                    projectId = targetProjectId,
                    groupId = targetGroup.id,
                    name = sourceLabel.name
                ) != null
            ) {
                logger.warn(
                    "pipeline label already exists, skip copy|$sourceProjectId|$targetProjectId|${sourceLabel.name}"
                )
                return
            }
            pipelineGroupService.addLabel(
                userId = userId,
                projectId = targetProjectId,
                pipelineLabel = PipelineLabelCreate(
                    groupId = HashUtil.encodeLongId(targetGroup.id),
                    name = sourceLabel.name
                )
            )
        } catch (ignored: Exception) {
            logger.warn(
                "copy pipeline label failed|$sourceProjectId|$targetProjectId|${sourceLabel.name}",
                ignored
            )
        }
    }

    private fun getOrCreateTargetGroup(
        userId: String,
        targetProjectId: String,
        groupName: String
    ) = pipelineGroupDao.getByName(
        dslContext = dslContext,
        projectId = targetProjectId,
        name = groupName
    ) ?: run {
        pipelineGroupService.addGroup(
            userId = userId,
            pipelineGroup = PipelineGroupCreate(
                projectId = targetProjectId,
                name = groupName
            )
        )
        pipelineGroupDao.getByName(
            dslContext = dslContext,
            projectId = targetProjectId,
            name = groupName
        ) ?: throw IllegalStateException("create pipeline label group failed|$targetProjectId|$groupName")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineGroupCopyService::class.java)
    }
}
