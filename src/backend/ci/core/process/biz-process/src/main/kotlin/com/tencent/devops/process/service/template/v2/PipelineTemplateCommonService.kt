package com.tencent.devops.process.service.template.v2

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.template.PipelineTemplateResourceDao
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResourceCommonCondition
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 流水线模版公共类
 */
@Service
class PipelineTemplateCommonService(
    val dslContext: DSLContext,
    val pipelineTemplateResourceDao: PipelineTemplateResourceDao,
    val pipelineTemplateSettingService: PipelineTemplateSettingService,
    val pipelineTemplateInfoService: PipelineTemplateInfoService
) {
    @Value("\${template.maxSaveVersionNum:300}")
    private val maxSaveVersionNum: Int = 300

    // 检查模板基本信息
    fun checkTemplateBasicInfo(
        projectId: String,
        name: String,
        templateId: String? = null
    ) {
        if (name.isBlank()) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_NAME_CAN_NOT_NULL
            )
        }
        val count = pipelineTemplateInfoService.count(
            commonCondition = PipelineTemplateCommonCondition(
                projectId = projectId,
                exactSearchName = name
            )
        )
        if (count > 0) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NAME_IS_EXISTS
            )
        }
        if (templateId != null) {
            val saveVersionNum = pipelineTemplateResourceDao.count(
                dslContext = dslContext,
                commonCondition = PipelineTemplateResourceCommonCondition(
                    projectId = projectId,
                    templateId = templateId
                )
            )
            if (saveVersionNum >= maxSaveVersionNum) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_COUNT_EXCEEDS_LIMIT,
                    params = arrayOf(maxSaveVersionNum.toString())
                )
            }
        }
    }

    companion object {
        private const val MAX_VERSION_NAME_LENGTH = 64 // 数据库 VARCHAR(64) 限制

        /**
         * 构建带后缀的版本名称，确保不超过最大长度限制
         *
         * @param originalName 原始版本名称
         * @param suffix 后缀（如 "-200"）
         * @param maxLength 最大长度限制，默认为 64
         * @return 截断后的版本名称（如果需要）
         */
        fun buildVersionNameWithSuffix(
            originalName: String,
            suffix: String,
            maxLength: Int = MAX_VERSION_NAME_LENGTH
        ): String {
            val newName = "$originalName$suffix"
            return if (newName.length > maxLength) {
                // 需要截断原始名称以容纳后缀
                val maxOriginalLength = maxLength - suffix.length
                if (maxOriginalLength <= 0) {
                    // 后缀本身就超长了，只能返回后缀
                    suffix.take(maxLength)
                } else {
                    // 截断原始名称，保留完整后缀
                    "${originalName.take(maxOriginalLength)}$suffix"
                }
            } else {
                newName
            }
        }
    }
}
