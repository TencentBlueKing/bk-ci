package com.tencent.devops.dispatch.dao

import com.tencent.devops.model.dispatch.tables.TDispatchPipelineDockerTemplate
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerTemplateRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PipelineDockerTemplateDao {

    fun updateTemplate(
        dslContext: DSLContext,
        versionId: Int,
        showVersionId: Int,
        showVersionName: String,
        deploymentId: Int,
        deploymentName: String,
        ccAppId: Long,
        bcsProjectId: String,
        clusterId: String
    ) {
        with(TDispatchPipelineDockerTemplate.T_DISPATCH_PIPELINE_DOCKER_TEMPLATE) {
            dslContext.insertInto(this,
                    VERSION_ID,
                    SHOW_VERSION_ID,
                    SHOW_VERSION_NAME,
                    DEPLOYMENT_ID,
                    DEPLOYMENT_NAME,
                    CC_APP_ID,
                    BCS_PROJECT_ID,
                    CLUSTER_ID,
                    CREATED_TIME)
                    .values(
                            versionId,
                            showVersionId,
                            showVersionName,
                            deploymentId,
                            deploymentName,
                            ccAppId,
                            bcsProjectId,
                            clusterId,
                            LocalDateTime.now()
                    )
                    .execute()
        }
    }

    fun getTemplate(dslContext: DSLContext): TDispatchPipelineDockerTemplateRecord? {
        with(TDispatchPipelineDockerTemplate.T_DISPATCH_PIPELINE_DOCKER_TEMPLATE) {
            return dslContext.selectFrom(this)
                    .orderBy(ID.desc())
                    .limit(1)
                    .fetchOne()
        }
    }

    fun getTemplateById(dslContext: DSLContext, id: Int): TDispatchPipelineDockerTemplateRecord? {
        with(TDispatchPipelineDockerTemplate.T_DISPATCH_PIPELINE_DOCKER_TEMPLATE) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .orderBy(ID.desc())
                    .limit(1)
                    .fetchOne()
        }
    }
}