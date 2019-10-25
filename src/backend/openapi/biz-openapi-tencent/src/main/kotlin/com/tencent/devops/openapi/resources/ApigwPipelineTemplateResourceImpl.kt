package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.ApigwPipelineTemplateResource
import com.tencent.devops.process.api.service.ServicePipelineTemplateResource
import com.tencent.devops.process.pojo.PipelineTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwPipelineTemplateResourceImpl @Autowired constructor(private val client: Client) : ApigwPipelineTemplateResource {
    override fun listTemplate(userId: String, projectId: String): Result<Map<String, PipelineTemplate>> {
        logger.info("get project's pipeline template, projectId($projectId)")
        val templates = client.get(ServicePipelineTemplateResource::class).listTemplate(projectId)
        val templatesResult = mutableMapOf<String, PipelineTemplate>()
        if (templates.data != null) {
            (templates.data as Map<String, PipelineTemplate>).forEach {
                templatesResult[it.value.name] = it.value
            }
        }
        return Result(templatesResult)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwPipelineTemplateResourceImpl::class.java)
    }
}