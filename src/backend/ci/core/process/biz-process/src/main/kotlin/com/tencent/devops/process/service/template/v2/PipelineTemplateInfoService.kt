package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.constant.PipelineTemplateConstant
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_NOT_EXISTS
import com.tencent.devops.process.dao.template.PipelineTemplateInfoDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PTemplateSource2Count
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoUpdateInfo
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateInfoV2
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模版基础信息类
 */
@Service
class PipelineTemplateInfoService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTemplateInfoDao: PipelineTemplateInfoDao
) {
    fun createOrUpdate(
        transactionContext: DSLContext? = null,
        pipelineTemplateInfo: PipelineTemplateInfoV2
    ) {
        pipelineTemplateInfoDao.create(
            dslContext = transactionContext ?: dslContext,
            record = pipelineTemplateInfo
        )
    }

    fun get(commonCondition: PipelineTemplateCommonCondition): PipelineTemplateInfoV2 {
        return pipelineTemplateInfoDao.get(
            dslContext = dslContext,
            commonCondition = commonCondition
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_TEMPLATE_NOT_EXISTS
        )
    }

    fun get(templateId: String): PipelineTemplateInfoV2 {
        return pipelineTemplateInfoDao.get(
            dslContext = dslContext,
            templateId = templateId
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_TEMPLATE_NOT_EXISTS
        )
    }

    fun get(
        projectId: String,
        templateId: String
    ): PipelineTemplateInfoV2 {
        return pipelineTemplateInfoDao.get(
            dslContext = dslContext,
            commonCondition = PipelineTemplateCommonCondition(
                projectId = projectId,
                templateId = templateId
            )
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_TEMPLATE_NOT_EXISTS
        )
    }

    fun getOrNull(
        projectId: String,
        templateId: String
    ): PipelineTemplateInfoV2? {
        return pipelineTemplateInfoDao.get(
            dslContext = dslContext,
            commonCondition = PipelineTemplateCommonCondition(
                projectId = projectId,
                templateId = templateId
            )
        )
    }

    fun getType2Count(
        projectId: String,
        templateIds: List<String>
    ): Map<String, Int> {
        val type2Count = pipelineTemplateInfoDao.getType2Count(
            dslContext = dslContext,
            projectId = projectId,
            templateIds = templateIds
        ).toMutableMap()
        val totalCount = count(
            PipelineTemplateCommonCondition(
                projectId = projectId,
                filterTemplateIds = templateIds
            )
        )
        type2Count[PipelineTemplateConstant.ALL] = totalCount
        return type2Count
    }

    fun getSource2Count(commonCondition: PipelineTemplateCommonCondition): PTemplateSource2Count {
        val rawCounts = pipelineTemplateInfoDao.getSource2count(
            dslContext = dslContext,
            commonCondition = commonCondition
        )
        val custom = rawCounts[TemplateType.CUSTOMIZE.name] ?: 0
        val market = rawCounts[TemplateType.CONSTRAINT.name] ?: 0
        return PTemplateSource2Count(
            all = custom + market,
            custom = custom,
            market = market
        )
    }

    fun isNameExist(
        projectId: String,
        templateName: String,
        excludeTemplateId: String?
    ): Boolean {
        return pipelineTemplateInfoDao.isNameExist(
            dslContext = dslContext,
            projectId = projectId,
            templateName = templateName,
            excludeTemplateId = excludeTemplateId
        )
    }

    fun count(commonCondition: PipelineTemplateCommonCondition): Int {
        return pipelineTemplateInfoDao.count(
            dslContext = dslContext,
            commonCondition = commonCondition
        )
    }

    fun delete(
        transactionContext: DSLContext? = null,
        commonCondition: PipelineTemplateCommonCondition
    ) {
        pipelineTemplateInfoDao.delete(
            dslContext = transactionContext ?: dslContext,
            commonCondition = commonCondition
        )
    }

    fun list(commonCondition: PipelineTemplateCommonCondition): List<PipelineTemplateInfoV2> {
        return pipelineTemplateInfoDao.list(
            dslContext = dslContext,
            commonCondition = commonCondition
        )
    }

    fun listAllIds(
        projectId: String
    ): List<String> {
        return pipelineTemplateInfoDao.listAllIds(
            dslContext = dslContext,
            projectId = projectId
        )
    }

    fun listSrcTemplateIds(
        projectId: String
    ): List<String> {
        return pipelineTemplateInfoDao.listSrcTemplateIds(
            dslContext = dslContext,
            projectId = projectId
        )
    }

    fun update(
        transactionContext: DSLContext? = null,
        record: PipelineTemplateInfoUpdateInfo,
        commonCondition: PipelineTemplateCommonCondition
    ) {
        pipelineTemplateInfoDao.update(
            dslContext = transactionContext ?: dslContext,
            commonCondition = commonCondition,
            record = record
        )
    }

    fun listPacSettings(
        templateIds: List<String>
    ): Map<String, Boolean> {
        return pipelineTemplateInfoDao.listPacSettings(
            dslContext = dslContext,
            templateIds = templateIds
        )
    }
}
