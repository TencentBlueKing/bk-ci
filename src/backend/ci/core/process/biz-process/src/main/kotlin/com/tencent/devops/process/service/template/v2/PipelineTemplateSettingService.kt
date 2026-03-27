package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_SETTING_DRAFT_VERSION_NOT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_SETTING_NOT_EXISTS
import com.tencent.devops.process.dao.PipelineTemplateSettingDraftVersionDao
import com.tencent.devops.process.dao.template.PipelineTemplateSettingDao
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateSettingUpdateInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模版设置相关类
 */
@Service
class PipelineTemplateSettingService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTemplateSettingDao: PipelineTemplateSettingDao,
    private val pipelineTemplateSettingDraftVersionDao: PipelineTemplateSettingDraftVersionDao
) {
    fun get(commonCondition: PipelineTemplateSettingCommonCondition): PipelineSetting {
        return pipelineTemplateSettingDao.get(
            commonCondition = commonCondition,
            dslContext = dslContext
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_SETTING_NOT_EXISTS)
    }

    fun get(
        projectId: String,
        templateId: String,
        settingVersion: Int
    ): PipelineSetting {
        return pipelineTemplateSettingDao.get(
            commonCondition = PipelineTemplateSettingCommonCondition(
                projectId = projectId,
                templateId = templateId,
                settingVersion = settingVersion
            ),
            dslContext = dslContext
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_SETTING_NOT_EXISTS)
    }

    fun getOrNull(
        projectId: String,
        templateId: String,
        settingVersion: Int
    ): PipelineSetting? {
        return pipelineTemplateSettingDao.get(
            commonCondition = PipelineTemplateSettingCommonCondition(
                projectId = projectId,
                templateId = templateId,
                settingVersion = settingVersion
            ),
            dslContext = dslContext
        )
    }

    fun count(commonCondition: PipelineTemplateSettingCommonCondition): Int {
        return pipelineTemplateSettingDao.count(
            commonCondition = commonCondition,
            dslContext = dslContext
        )
    }

    fun getPipelineTemplateSetting(
        projectId: String,
        templateId: String,
        settingVersion: Int
    ): PipelineSetting {
        return pipelineTemplateSettingDao.get(
            commonCondition = PipelineTemplateSettingCommonCondition(
                projectId = projectId,
                templateId = templateId,
                settingVersion = settingVersion
            ),
            dslContext = dslContext
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_SETTING_NOT_EXISTS)
    }

    fun delete(
        transactionContext: DSLContext? = null,
        commonCondition: PipelineTemplateSettingCommonCondition
    ) {
        pipelineTemplateSettingDao.delete(
            dslContext = dslContext,
            commonCondition = commonCondition
        )
    }

    // 修剪最新的版本记录。
    fun pruneLatestVersions(
        transactionContext: DSLContext? = null,
        projectId: String,
        templateId: String,
        limit: Int
    ) {
        pipelineTemplateSettingDao.pruneLatestVersions(
            dslContext = transactionContext ?: dslContext,
            projectId = projectId,
            templateId = templateId,
            limit = limit
        )
    }

    fun list(commonCondition: PipelineTemplateSettingCommonCondition): List<PipelineSetting> {
        return pipelineTemplateSettingDao.list(
            dslContext = dslContext,
            commonCondition = commonCondition
        )
    }

    fun createOrUpdate(
        transactionContext: DSLContext? = null,
        pipelineTemplateSetting: PipelineSetting
    ) {
        pipelineTemplateSettingDao.createOrUpdate(
            dslContext = transactionContext ?: dslContext,
            record = pipelineTemplateSetting
        )
    }

    fun update(
        transactionContext: DSLContext? = null,
        record: PipelineTemplateSettingUpdateInfo,
        commonCondition: PipelineTemplateSettingCommonCondition
    ) {
        pipelineTemplateSettingDao.update(
            dslContext = transactionContext ?: dslContext,
            record = record,
            commonCondition = commonCondition
        )
    }

    fun getDraftVersion(
        projectId: String,
        templateId: String,
        settingVersion: Int,
        draftVersion: Int
    ): PipelineSetting {
        return pipelineTemplateSettingDraftVersionDao.get(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            settingVersion = settingVersion,
            draftVersion = draftVersion
        )?.toPipelineSetting() ?: throw ErrorCodeException(
            errorCode = ERROR_TEMPLATE_SETTING_DRAFT_VERSION_NOT_EXISTS,
            params = arrayOf(draftVersion.toString())
        )
    }
}
