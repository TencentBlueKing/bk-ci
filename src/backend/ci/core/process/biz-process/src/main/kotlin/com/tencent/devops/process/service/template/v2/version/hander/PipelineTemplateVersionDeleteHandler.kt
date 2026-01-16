/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service.template.v2.version.hander

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.enums.PublicVerGroupReferenceTypeEnum
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_LATEST_RELEASED_VERSION_NOT_EXIST
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TEMPLATE_LATEST_VERSION_CAN_NOT_DELETE
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateCommonCondition
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateModelLock
import com.tencent.devops.process.service.template.v2.PipelineTemplatePersistenceService
import com.tencent.devops.process.service.template.v2.PipelineTemplateRelatedService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionDeleteContext
import com.tencent.devops.process.service.template.v2.version.processor.PTemplateVersionDeletePostProcessor
import com.tencent.devops.process.service.`var`.PublicVarGroupReferInfoService
import com.tencent.devops.process.yaml.PipelineYamlFacadeService
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线模版删除处理
 */
@Service
class PipelineTemplateVersionDeleteHandler @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineTemplateRelatedService: PipelineTemplateRelatedService,
    private val pipelineTemplatePersistenceService: PipelineTemplatePersistenceService,
    private val versionDeletePostProcessor: PTemplateVersionDeletePostProcessor,
    private val templatePipelineDao: TemplatePipelineDao,
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineYamlFacadeService: PipelineYamlFacadeService,
    private val publicVarGroupReferInfoService: PublicVarGroupReferInfoService

    ) {
    fun handle(context: PipelineTemplateVersionDeleteContext) {
        with(context) {
            logger.info(
                "handle pipeline template version delete|" +
                    "projectId:$projectId|templateId:$templateId|versionAction:$versionAction" +
                    "|version:$version|branch:$branch"
            )
            val lock = PipelineTemplateModelLock(redisOperation = redisOperation, templateId = templateId)
            try {
                lock.lock()
                return doHandle()
            } finally {
                lock.unlock()
            }
        }
    }

    private fun PipelineTemplateVersionDeleteContext.doHandle() {
        when (versionAction) {
            PipelineVersionAction.DELETE_VERSION -> {
                deleteVersion()
            }

            PipelineVersionAction.INACTIVE_BRANCH -> {
                inactiveBranchVersion()
            }

            PipelineVersionAction.DELETE_ALL_VERSIONS -> {
                deleteAllVersions()
            }

            else -> {}
        }
        versionDeletePostProcessor.postProcessAfterDelete(context = this)
    }

    private fun PipelineTemplateVersionDeleteContext.deleteVersion() {
        if (version == null) {
            throw IllegalArgumentException("version is null")
        }
        val latestReleasedResource = pipelineTemplateResourceService.getLatestReleasedResource(
            projectId = projectId,
            templateId = templateId
        ) ?: throw ErrorCodeException(errorCode = ERROR_TEMPLATE_LATEST_RELEASED_VERSION_NOT_EXIST)
        // 最新版本不能删除
        if (latestReleasedResource.version == version) {
            throw ErrorCodeException(errorCode = ERROR_TEMPLATE_LATEST_VERSION_CAN_NOT_DELETE)
        }
        val marketTemplateStatus = client.get(ServiceTemplateResource::class).getMarketTemplateStatus(templateId).data!!
        // 上架研发商店不允许删除
        if (marketTemplateStatus == TemplateStatusEnum.RELEASED) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_PUBLISH
            )
        }
        val instanceSize = templatePipelineDao.countByVersionFeat(
            dslContext = dslContext,
            projectId = projectId,
            templateId = templateId,
            instanceType = PipelineInstanceTypeEnum.CONSTRAINT.type,
            version = version
        )
        if (instanceSize > 0) {
            logger.warn("There are $instanceSize pipeline attach to $templateId of version $version")
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_HAVE_INSTANCE
            )
        }
        pipelineTemplatePersistenceService.deleteVersion(
            userId = userId,
            projectId = projectId,
            templateId = templateId,
            version = version
        )

        publicVarGroupReferInfoService.deletePublicGroupRefer(
            userId = userId,
            projectId = projectId,
            referId = templateId,
            referType = PublicVerGroupReferenceTypeEnum.TEMPLATE,
            referVersion = version.toInt()
        )
    }

    private fun PipelineTemplateVersionDeleteContext.deleteAllVersions() {
        logger.info("Start to delete all template versions $projectId|$templateId")
        val templateInfo = pipelineTemplateInfoService.get(
            projectId = projectId,
            templateId = templateId
        )
        if (templateInfo.enablePac) {
            // 检查yaml是否已经在默认分支删除
            val yamlExist = pipelineYamlFacadeService.yamlExistInDefaultBranch(
                projectId = projectId, pipelineId = templateInfo.id
            )
            if (yamlExist) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_DELETE_YAML_TEMPLATE_IN_DEFAULT_BRANCH
                )
            }
        }
        val isTemplateExistInstances = pipelineTemplateRelatedService.isTemplateExistInstances(
            projectId = projectId,
            templateId = templateId
        )

        if (isTemplateExistInstances) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_HAVE_INSTANCE
            )
        }

        if (templateInfo.mode == TemplateType.CUSTOMIZE && templateInfo.storeStatus == TemplateStatusEnum.RELEASED) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_PUBLISH
            )
        }
        val isExistInstalledTemplate = pipelineTemplateInfoService.count(
            PipelineTemplateCommonCondition(
                mode = TemplateType.CONSTRAINT,
                srcTemplateProjectId = projectId,
                srcTemplateId = templateId
            )
        ) > 0
        if (templateInfo.mode == TemplateType.CUSTOMIZE && isExistInstalledTemplate) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.TEMPLATE_CAN_NOT_DELETE_WHEN_INSTALL
            )
        }
        pipelineTemplatePersistenceService.deleteTemplateAllVersions(
            projectId = projectId,
            templateId = templateId
        )
    }

    private fun PipelineTemplateVersionDeleteContext.inactiveBranchVersion() {
        if (branch == null) {
            throw IllegalArgumentException("branchName is null")
        }
        pipelineTemplatePersistenceService.inactiveBranchVersion(
            projectId = projectId,
            templateId = templateId,
            branch = branch
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTemplateVersionDeleteHandler::class.java)
    }
}
