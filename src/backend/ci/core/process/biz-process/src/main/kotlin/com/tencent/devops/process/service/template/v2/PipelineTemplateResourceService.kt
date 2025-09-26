package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_VERSION_NOT_EXISTS
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.pojo.setting.PipelineVersionSimple
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceUpdateInfo
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模版资源相关类
 */
@Service
class PipelineTemplateResourceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTemplateResourceDao: PipelineTemplateResourceDao
) {

    fun getTemplateResourceVersion(
        projectId: String,
        templateId: String,
        version: Long
    ): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.get(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            version = version
        )
    }

    /**
     * 获取模版最新版本
     *
     * 获取模版最新版本应该包含已经删除的,不然计算版本时会有异常,
     * 如新建一个草稿版本,然后再删除草稿,再创建一个草稿版本,如果不包含删除的,那么版本会与删除的一样
     */
    fun getLatestVersionResource(
        projectId: String,
        templateId: String
    ): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.getLatestRecord(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            includeDelete = true
        )
    }

    fun getLatestReleasedResource(
        projectId: String? = null,
        templateId: String
    ): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.getLatestRecord(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            status = VersionStatus.RELEASED
        )
    }

    // 获取最新上架版本
    fun getLatestPublishedResource(
        projectId: String,
        templateId: String
    ): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.getLatestRecord(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            status = VersionStatus.RELEASED,
            storeStatusList = listOf(
                TemplateStatusEnum.RELEASED,
                TemplateStatusEnum.AUDITING
            )
        )
    }

    fun listLatestReleasedVersions(templateIds: List<String>): List<PipelineVersionSimple> {
        return pipelineTemplateResourceDao.listLatestReleasedVersions(
            dslContext = dslContext,
            templateIds = templateIds
        )
    }

    fun getLatestBranchResource(
        projectId: String,
        templateId: String,
        branchName: String? = null
    ): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.getLatestRecord(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            status = VersionStatus.BRANCH,
            versionName = branchName
        )
    }

    fun getDraftVersionResource(
        projectId: String,
        templateId: String
    ): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.getLatestRecord(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            status = VersionStatus.COMMITTING
        )
    }

    fun getLatestResource(
        projectId: String,
        templateId: String,
        status: VersionStatus? = null,
        version: Long? = null,
        versionName: String? = null,
        includeDelete: Boolean = false
    ): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.getLatestRecord(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            status = status,
            version = version,
            versionName = versionName,
            includeDelete = includeDelete
        )
    }

    fun getDraftBaseVersionResource(
        projectId: String,
        templateId: String
    ): PipelineTemplateResource? {
        val draftBaseVersion = pipelineTemplateResourceDao.get(
            commonCondition = PipelineTemplateResourceCommonCondition(
                projectId = projectId,
                templateId = templateId,
                status = VersionStatus.COMMITTING
            ),
            dslContext = dslContext
        )?.baseVersion ?: return null
        val baseVersionResource = get(
            projectId = projectId,
            templateId = templateId,
            version = draftBaseVersion
        )
        return baseVersionResource
    }

    fun getTemplateVersions(
        commonCondition: PipelineTemplateResourceCommonCondition
    ): List<PipelineVersionSimple> {
        return pipelineTemplateResourceDao.getVersions(
            dslContext = dslContext,
            commonCondition = commonCondition
        )
    }

    fun get(commonCondition: PipelineTemplateResourceCommonCondition): PipelineTemplateResource {
        return pipelineTemplateResourceDao.get(
            commonCondition = commonCondition,
            dslContext = dslContext
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_VERSION_NOT_EXISTS)
    }

    fun getOrNull(commonCondition: PipelineTemplateResourceCommonCondition): PipelineTemplateResource? {
        return pipelineTemplateResourceDao.get(
            commonCondition = commonCondition,
            dslContext = dslContext
        )
    }

    fun get(
        projectId: String,
        templateId: String,
        version: Long
    ): PipelineTemplateResource {
        return pipelineTemplateResourceDao.get(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            version = version
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_VERSION_NOT_EXISTS)
    }

    fun count(commonCondition: PipelineTemplateResourceCommonCondition): Int {
        return pipelineTemplateResourceDao.count(
            commonCondition = commonCondition,
            dslContext = dslContext
        )
    }

    fun delete(
        transactionContext: DSLContext? = null,
        commonCondition: PipelineTemplateResourceCommonCondition
    ) {
        pipelineTemplateResourceDao.delete(
            dslContext = transactionContext ?: dslContext,
            commonCondition = commonCondition
        )
    }

    fun list(commonCondition: PipelineTemplateResourceCommonCondition): List<PipelineTemplateResource> {
        return pipelineTemplateResourceDao.list(
            dslContext = dslContext,
            commonCondition = commonCondition
        )
    }

    fun create(
        transactionContext: DSLContext? = null,
        pipelineTemplateResource: PipelineTemplateResource
    ) {
        pipelineTemplateResourceDao.create(
            dslContext = transactionContext ?: dslContext,
            record = pipelineTemplateResource
        )
    }

    fun update(
        transactionContext: DSLContext? = null,
        record: PipelineTemplateResourceUpdateInfo,
        commonCondition: PipelineTemplateResourceCommonCondition
    ): Int {
        return pipelineTemplateResourceDao.update(
            dslContext = transactionContext ?: dslContext,
            record = record,
            commonCondition = commonCondition
        )
    }

    fun transformTemplateToCustom(
        transactionContext: DSLContext,
        projectId: String,
        templateId: String
    ) {
        pipelineTemplateResourceDao.transformTemplateToCustom(
            dslContext = transactionContext,
            projectId = projectId,
            templateId = templateId
        )
    }
}
