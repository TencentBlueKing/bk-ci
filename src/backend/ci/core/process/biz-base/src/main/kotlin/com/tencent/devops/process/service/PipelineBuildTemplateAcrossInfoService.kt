package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.process.dao.PipelineBuildTemplateAcrossInfoDao
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.process.pojo.TemplateAcrossInfoType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineBuildTemplateAcrossInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateAcrossInfoDao: PipelineBuildTemplateAcrossInfoDao
) {
    fun batchCreateAcrossInfo(
        projectId: String,
        pipelineId: String,
        buildId: String? = null,
        userId: String,
        templateAcrossInfos: List<BuildTemplateAcrossInfo>
    ) {
        templateAcrossInfoDao.batchCreate(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            userId = userId,
            templateAcrossInfos = templateAcrossInfos
        )
    }

    fun batchUpdateAcrossInfo(
        projectId: String,
        pipelineId: String,
        buildId: String,
        templateAcrossInfos: List<BuildTemplateAcrossInfo>
    ) {
        templateAcrossInfoDao.batchUpdate(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            templateAcrossInfos = templateAcrossInfos
        )
    }

    fun getAcrossInfo(
        projectId: String,
        pipelineId: String?,
        templateId: String
    ): List<BuildTemplateAcrossInfo> {
        val records = if (pipelineId != null) {
            templateAcrossInfoDao.get(dslContext, projectId, pipelineId, templateId)
        } else {
            templateAcrossInfoDao.getByTemplateId(dslContext, projectId, templateId)
        }
        return records.map { record ->
            BuildTemplateAcrossInfo(
                templateId = record.templateId,
                templateType = TemplateAcrossInfoType.valueOf(record.templateType),
                templateInstancesIds = JsonUtil.to(record.templateInstanceIds),
                targetProjectId = record.targetProjectId
            )
        }
    }

    fun updateAcrossInfoBuildId(
        projectId: String,
        pipelineId: String,
        templateId: String,
        buildId: String
    ): Boolean {
        return templateAcrossInfoDao.updateBuildId(dslContext, projectId, pipelineId, templateId, buildId) > 0
    }

    fun deleteAcrossInfo(
        projectId: String,
        pipelineId: String,
        buildId: String?,
        templateId: String?
    ): Boolean {
        if (buildId != null) {
            return templateAcrossInfoDao.deleteByBuildId(dslContext, projectId, pipelineId, buildId) > 0
        }
        if (templateId != null) {
            return templateAcrossInfoDao.delete(dslContext, projectId, pipelineId, templateId) > 0
        }

        return templateAcrossInfoDao.deleteByPipelineId(dslContext, projectId, pipelineId) > 0
    }
}
