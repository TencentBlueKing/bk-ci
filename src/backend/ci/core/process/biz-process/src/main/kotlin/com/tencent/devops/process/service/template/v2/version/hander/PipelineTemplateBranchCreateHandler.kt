package com.tencent.devops.process.service.template.v2.version.hander

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.VersionStatus
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.pipeline.DeployTemplateResult
import com.tencent.devops.process.service.template.v2.PipelineTemplateGenerator
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelLock
import com.tencent.devops.process.service.template.v2.PipelineTemplatePersistenceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 创建流水线模版分支版本
 */
@Service
class PipelineTemplateBranchCreateHandler @Autowired constructor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplatePersistenceService: PipelineTemplatePersistenceService,
    private val pipelineTemplateGenerator: PipelineTemplateGenerator,
    private val redisOperation: RedisOperation
) : PipelineTemplateVersionCreateHandler {
    override fun support(context: PipelineTemplateVersionCreateContext): Boolean {
        return context.versionAction == PipelineVersionAction.CREATE_BRANCH
    }

    override fun handle(context: PipelineTemplateVersionCreateContext): DeployTemplateResult {
        with(context) {
            logger.info("create template branch version with context={}", JsonUtil.toJson(context, false))
            if (!enablePac) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf("enablePac")
                )
            }
            if (yamlFileInfo == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("yamlFileInfo")
                )
            }
            if (branchName == null) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf("branchName")
                )
            }
            if (pTemplateResourceWithoutVersion.status != VersionStatus.BRANCH) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_STATUS_NOT_MATCHED,
                    params = arrayOf(VersionStatus.BRANCH.name, pTemplateResourceWithoutVersion.status.name)
                )
            }
            val lock = PipelineTemplateModelLock(redisOperation = redisOperation, templateId = templateId)
            try {
                lock.lock()
                return doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineTemplateVersionCreateContext.doHandle(): DeployTemplateResult {
        val templateInfo = pipelineTemplateInfoService.getOrNull(
            projectId = projectId,
            templateId = templateId
        )
        val resourceOnlyVersion = if (templateInfo == null) {
            val resourceOnlyVersion = pipelineTemplateGenerator.getDefaultVersion(
                versionStatus = pTemplateResourceWithoutVersion.status,
                branchName = branchName
            )
            pipelineTemplatePersistenceService.initializeTemplate(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
            resourceOnlyVersion
        } else {
            val resourceOnlyVersion = pipelineTemplateGenerator.generateBranchVersion(
                projectId = projectId,
                templateId = templateId,
                branchName = branchName!!
            )
            pipelineTemplatePersistenceService.createBranchVersion(
                context = this,
                resourceOnlyVersion = resourceOnlyVersion
            )
            resourceOnlyVersion
        }
        return DeployTemplateResult(
            projectId = projectId,
            userId = userId,
            version = resourceOnlyVersion.version,
            templateId = templateId,
            templateName = pipelineTemplateInfo.name,
            number = resourceOnlyVersion.number,
            versionNum = resourceOnlyVersion.versionNum,
            versionName = resourceOnlyVersion.versionName,
            versionAction = versionAction
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateBranchCreateHandler::class.java)
    }
}
