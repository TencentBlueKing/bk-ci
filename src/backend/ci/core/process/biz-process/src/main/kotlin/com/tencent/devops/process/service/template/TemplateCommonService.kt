package com.tencent.devops.process.service.template

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.PipelineSettingDao
import com.tencent.devops.process.engine.dao.template.TemplateDao
import com.tencent.devops.process.permission.PipelinePermissionService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

/**
 * 随着 PAC 的改动抽出的代码逻辑会很多，这里用来放各种 TemplateService 的公共逻辑，避免漏改少改
 */
@Service
@RefreshScope
class TemplateCommonService @Autowired constructor(
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineSettingDao: PipelineSettingDao,
    private val templateDao: TemplateDao
) {

    @Value("\${template.maxSaveVersionNum:300}")
    private val maxSaveVersionNum: Int = 300

    /**
     * 只有管理员权限才能对模板操作
     */
    fun checkPermission(projectId: String, userId: String) {
        val isProjectUser = hasManagerPermission(projectId = projectId, userId = userId)
        if (!isProjectUser) {
            logger.warn("The manager users is empty of project $projectId")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ONLY_MANAGE_CAN_OPERATE_TEMPLATE
            )
        }
    }

    fun hasManagerPermission(projectId: String, userId: String): Boolean =
        pipelinePermissionService.checkProjectManager(userId = userId, projectId = projectId)

    fun checkTemplateName(
        dslContext: DSLContext,
        name: String,
        projectId: String,
        templateId: String
    ) {
        val count = pipelineSettingDao.getSettingByName(
            dslContext = dslContext,
            name = name,
            projectId = projectId,
            pipelineId = templateId,
            isTemplate = true
        )?.value1() ?: 0
        if (count > 0) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_NAME_IS_EXISTS
            )
        }
        // 判断提交的模板数量是否超过系统规定的阈值
        val versionNameNum = templateDao.countTemplateVersionNum(dslContext, projectId, templateId)
        if (versionNameNum >= maxSaveVersionNum) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_VERSION_COUNT_EXCEEDS_LIMIT,
                params = arrayOf(maxSaveVersionNum.toString())
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateCommonService::class.java)
    }
}
