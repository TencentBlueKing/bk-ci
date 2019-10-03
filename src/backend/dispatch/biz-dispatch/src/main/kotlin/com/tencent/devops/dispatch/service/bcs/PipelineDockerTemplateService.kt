package com.tencent.devops.dispatch.service.bcs

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.dao.PipelineDockerTemplateDao
import com.tencent.devops.dispatch.pojo.DockerTemplate
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineDockerTemplateService @Autowired constructor(
    private val dslContext: DSLContext,
    private val dockerTemplateDao: PipelineDockerTemplateDao
) {

    fun updateTemplate(
        versionId: Int,
        showVersionId: Int,
        showVersionName: String,
        deploymentId: Int,
        deploymentName: String,
        ccAppId: Long,
        bcsProjectId: String,
        clusterId: String
    ) =
            dockerTemplateDao.updateTemplate(dslContext, versionId, showVersionId, showVersionName, deploymentId, deploymentName, ccAppId, bcsProjectId, clusterId)

    fun getTemplate(): DockerTemplate {
        val templateRecord = dockerTemplateDao.getTemplate(dslContext) ?: throw RuntimeException("Docker template is not exist")
        return DockerTemplate(
                templateRecord.versionId,
                templateRecord.showVersionId,
                templateRecord.showVersionName,
                templateRecord.deploymentId,
                templateRecord.deploymentName,
                templateRecord.ccAppId,
                templateRecord.bcsProjectId,
                templateRecord.clusterId,
                templateRecord.createdTime.timestamp()
        )
    }

    fun getTemplateById(id: Int): DockerTemplate {
        val templateRecord = dockerTemplateDao.getTemplateById(dslContext, id) ?: throw RuntimeException("Docker template is not exist")
        return DockerTemplate(
                templateRecord.versionId,
                templateRecord.showVersionId,
                templateRecord.showVersionName,
                templateRecord.deploymentId,
                templateRecord.deploymentName,
                templateRecord.ccAppId,
                templateRecord.bcsProjectId,
                templateRecord.clusterId,
                templateRecord.createdTime.timestamp()
        )
    }
}