package com.tencent.devops.process.service.builds

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

    fun create(
        projectId: String,
        pipelineId: String,
        buildId: String? = null,
        userId: String,
        templateAcrossInfo: BuildTemplateAcrossInfo
    ) {
        templateAcrossInfoDao.create(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            templateId = templateAcrossInfo.templateId,
            templateType = templateAcrossInfo.templateType,
            templateInstancesIds = templateAcrossInfo.templateInstancesIds,
            targetProjectId = templateAcrossInfo.targetProjectId,
            userId = userId
        )
    }

    fun get(
        projectId: String,
        pipelineId: String,
        templateId: String
    ): List<BuildTemplateAcrossInfo> {
        val records = templateAcrossInfoDao.get(dslContext, projectId, pipelineId, templateId)
        return records.map { record ->
            BuildTemplateAcrossInfo(
                templateId = record.templateId,
                templateType = TemplateAcrossInfoType.valueOf(record.templateType),
                templateInstancesIds = JsonUtil.to(record.templateInstanceIds),
                targetProjectId = record.targetProjectId
            )
        }
    }

    fun updateBuildId(
        projectId: String,
        pipelineId: String,
        templateId: String,
        buildId: String
    ): Boolean {
        return templateAcrossInfoDao.updateBuildId(dslContext, projectId, pipelineId, templateId, buildId) > 0
    }

    fun delete(
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
