package com.tencent.devops.process.service.template.v2.version.processor

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PipelineVersionAction
import com.tencent.devops.common.pipeline.pojo.setting.PipelineSetting
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.process.pojo.template.v2.PipelineTemplateResource
import com.tencent.devops.process.service.template.v2.PipelineTemplateInfoService
import com.tencent.devops.process.service.template.v2.PipelineTemplateResourceService
import com.tencent.devops.process.service.template.v2.version.PipelineTemplateVersionCreateContext
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo
import org.springframework.stereotype.Service

/**
 * 流水线模板版本创建研发商店安装后置处理器
 */
@Service
class PTemplateMarketInstallVersionPostProcessor(
    private val pipelineTemplateInfoService: PipelineTemplateInfoService,
    private val pipelineTemplateResourceService: PipelineTemplateResourceService,
    private val client: Client
) : PTemplateVersionCreatePostProcessor {

    override fun postProcessAfterVersionCreate(
        context: PipelineTemplateVersionCreateContext,
        pipelineTemplateResource: PipelineTemplateResource,
        pipelineTemplateSetting: PipelineSetting
    ) {
        with(context) {
            if (versionAction != PipelineVersionAction.CREATE_RELEASE) {
                return
            }
            val templateInfo = pipelineTemplateInfoService.get(
                projectId = projectId,
                templateId = templateId
            )
            if (templateInfo.mode != TemplateType.CONSTRAINT)
                return

            val latestReleasedResource = pipelineTemplateResourceService.getLatestReleasedResource(
                projectId = projectId,
                templateId = templateId
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_TEMPLATE_LATEST_RELEASED_VERSION_NOT_EXIST
            )
            val srcTemplateProjectId = latestReleasedResource.srcTemplateProjectId!!
            val srcTemplateId = latestReleasedResource.srcTemplateId!!
            val srcTemplateVersion = latestReleasedResource.srcTemplateVersion!!
            val srcTemplateResource = pipelineTemplateResourceService.get(
                projectId = srcTemplateProjectId,
                templateId = srcTemplateId,
                version = srcTemplateVersion
            )
            client.get(ServiceTemplateResource::class).createTemplateInstallHistory(
                installHistoryInfo = TemplateVersionInstallHistoryInfo(
                    srcMarketTemplateProjectCode = srcTemplateProjectId,
                    srcMarketTemplateCode = srcTemplateId,
                    version = srcTemplateVersion,
                    versionName = srcTemplateResource.versionName!!,
                    number = srcTemplateResource.number,
                    projectCode = projectId,
                    templateCode = templateId,
                    creator = userId
                )
            )
        }
    }
}
