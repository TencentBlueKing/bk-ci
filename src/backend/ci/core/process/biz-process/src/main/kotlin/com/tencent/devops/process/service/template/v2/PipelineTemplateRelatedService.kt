package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.template.PipelineTemplateRelatedDao
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.pojo.template.TemplatePipelineStatus
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelated
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedSimple
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateRelatedUpdateInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模版与流水线关联类
 */
@Service
class PipelineTemplateRelatedService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTemplateRelatedDao: PipelineTemplateRelatedDao,
    private val templateDao: TemplateDao,
    private val pipelineTemplateResourceDao: PipelineTemplateResourceDao
) {
    fun create(
        transactionContext: DSLContext? = null,
        pipelineTemplateRelated: PipelineTemplateRelated
    ) {
        pipelineTemplateRelatedDao.create(
            dslContext = transactionContext ?: dslContext,
            record = pipelineTemplateRelated
        )
    }

    fun get(condition: PipelineTemplateRelatedCommonCondition): PipelineTemplateRelated? {
        return pipelineTemplateRelatedDao.get(
            dslContext = dslContext,
            condition = condition
        )
    }

    fun get(projectId: String, pipelineId: String): PipelineTemplateRelated? {
        return pipelineTemplateRelatedDao.get(
            dslContext = dslContext,
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                pipelineId = pipelineId
            )
        )
    }

    fun isPipelineInstanceFromTemplate(
        projectId: String,
        pipelineId: String
    ): Boolean {
        return get(projectId, pipelineId)?.let { it.instanceType == PipelineInstanceTypeEnum.CONSTRAINT } ?: false
    }

    fun count(condition: PipelineTemplateRelatedCommonCondition): Int {
        return pipelineTemplateRelatedDao.count(
            dslContext = dslContext,
            condition = condition
        )
    }

    fun delete(
        transactionContext: DSLContext? = null,
        condition: PipelineTemplateRelatedCommonCondition
    ) {
        pipelineTemplateRelatedDao.delete(
            dslContext = transactionContext ?: dslContext,
            condition = condition
        )
    }

    fun list(condition: PipelineTemplateRelatedCommonCondition): List<PipelineTemplateRelated> {
        return pipelineTemplateRelatedDao.list(
            dslContext = dslContext,
            condition = condition
        )
    }

    fun listByPipelineIds(
        projectId: String,
        pipelineIds: Set<String>
    ): List<PipelineTemplateRelated> {
        return pipelineTemplateRelatedDao.list(
            dslContext = dslContext,
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                pipelineIds = pipelineIds.toList()
            )
        )
    }

    fun listSimple(
        projectId: String,
        templateId: String,
        pipelineName: String?,
        updater: String?,
        templateVersion: Long?,
        status: TemplatePipelineStatus?,
        pipelineIds: List<String>?,
        instanceTypeEnum: PipelineInstanceTypeEnum,
        limit: Int,
        offset: Int
    ): List<PipelineTemplateRelatedSimple> {
        return pipelineTemplateRelatedDao.listSimple(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            pipelineName = pipelineName,
            updater = updater,
            templateVersion = templateVersion,
            status = status,
            pipelineIds = pipelineIds,
            instanceTypeEnum = instanceTypeEnum,
            limit = limit,
            offset = offset
        )
    }

    fun countSimple(
        projectId: String,
        templateId: String,
        pipelineName: String?,
        updater: String?,
        templateVersion: Long?,
        status: TemplatePipelineStatus?,
        pipelineIds: List<String>?,
        instanceTypeEnum: PipelineInstanceTypeEnum
    ): Int {
        return pipelineTemplateRelatedDao.countSimple(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            pipelineName = pipelineName,
            updater = updater,
            templateVersion = templateVersion,
            status = status,
            pipelineIds = pipelineIds,
            instanceTypeEnum = instanceTypeEnum
        )
    }

    fun update(
        transactionContext: DSLContext? = null,
        updateInfo: PipelineTemplateRelatedUpdateInfo,
        condition: PipelineTemplateRelatedCommonCondition
    ) {
        pipelineTemplateRelatedDao.update(
            dslContext = transactionContext ?: dslContext,
            condition = condition,
            updateInfo = updateInfo
        )
    }

    fun updateStatus(
        transactionContext: DSLContext? = null,
        projectId: String,
        pipelineIds: List<String>,
        status: TemplatePipelineStatus,
        instanceErrorInfo: String? = null,
        pullRequestUrl: String? = null,
        pullRequestId: Long? = null
    ) {
        pipelineTemplateRelatedDao.update(
            dslContext = transactionContext ?: dslContext,
            updateInfo = PipelineTemplateRelatedUpdateInfo(
                status = status,
                instanceErrorInfo = instanceErrorInfo,
                pullRequestUrl = pullRequestUrl,
                pullRequestId = pullRequestId
            ),
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                pipelineIds = pipelineIds
            )
        )
    }

    fun updateStatusByPullRequestId(
        projectId: String,
        pipelineId: String,
        pullRequestId: Long,
        status: TemplatePipelineStatus,
        instanceErrorInfo: String? = null
    ) {
        pipelineTemplateRelatedDao.update(
            dslContext = dslContext,
            updateInfo = PipelineTemplateRelatedUpdateInfo(
                status = status,
                instanceErrorInfo = instanceErrorInfo
            ),
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                pipelineId = pipelineId,
                pullRequestId = pullRequestId
            )
        )
    }

    fun isTemplateExistInstances(
        projectId: String,
        templateId: String
    ): Boolean {
        return count(
            condition = PipelineTemplateRelatedCommonCondition(
                projectId = projectId,
                templateId = templateId,
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT,
                deleted = false
            )
        ) > 0
    }

    fun createRelation(
        userId: String,
        projectId: String,
        templateId: String,
        pipelineId: String,
        instanceType: String,
        buildNo: BuildNo? = null,
        param: List<BuildFormProperty>? = null,
        fixTemplateVersion: Long? = null
    ): Pair<Long, String> {
        logger.info(
            "Start creating relation between template and pipeline|userId=$userId|" +
                "$templateId|$pipelineId|$instanceType|$fixTemplateVersion"
        )

        val (templateVersion, versionName) = if (fixTemplateVersion != null) {
            // 使用指定版本
            val v2Record = pipelineTemplateResourceDao.getLatestRecord(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                version = fixTemplateVersion
            )
            if (v2Record != null) {
                v2Record.version to v2Record.versionName!!
            } else {
                val v1Record = templateDao.getTemplate(
                    dslContext = dslContext,
                    version = fixTemplateVersion
                ) ?: throw ErrorCodeException(errorCode = ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS)
                v1Record.version to v1Record.versionName
            }
        } else {
            // 使用最新版本
            val v2LatestRecord = pipelineTemplateResourceDao.getLatestRecord(
                dslContext = dslContext,
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.RELEASED
            )
            if (v2LatestRecord != null) {
                v2LatestRecord.version to v2LatestRecord.versionName!!
            } else {
                val v1LatestRecord = templateDao.getLatestTemplate(dslContext, templateId)
                v1LatestRecord.version to v1LatestRecord.versionName
            }
        }
        create(
            pipelineTemplateRelated = PipelineTemplateRelated(
                projectId = projectId,
                templateId = templateId,
                pipelineId = pipelineId,
                version = templateVersion,
                versionName = versionName,
                instanceType = PipelineInstanceTypeEnum.valueOf(instanceType),
                rootTemplateId = templateId,
                creator = userId,
                updater = userId,
                buildNo = buildNo,
                params = param,
                instanceErrorInfo = null,
                deleted = false
            )
        )
        logger.info(
            "Successfully created relation: templateId=$templateId, pipelineId=$pipelineId, " +
                "version=$templateVersion"
        )
        return Pair(templateVersion, versionName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateCommonService::class.java)
    }
}
