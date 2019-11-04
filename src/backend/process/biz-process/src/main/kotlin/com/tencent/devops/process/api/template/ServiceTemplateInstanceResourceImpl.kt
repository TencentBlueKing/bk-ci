package com.tencent.devops.process.api.template

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.pojo.PipelineId
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstanceParams
import com.tencent.devops.process.pojo.template.TemplateInstances
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import org.springframework.beans.factory.annotation.Autowired

/**
 * deng
 * 2019-01-08
 */
@RestResource
class ServiceTemplateInstanceResourceImpl @Autowired constructor(private val templateService: TemplateService) :
    ServiceTemplateInstanceResource {

    override fun createTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet {
        return templateService.createTemplateInstances(projectId, userId, templateId, version, useTemplateSettings, instances)
    }

    override fun countTemplateInstance(projectId: String, templateIds: Collection<String>): Result<Int> {
        return Result(templateService.serviceCountTemplateInstances(projectId, templateIds))
    }

    override fun countTemplateInstanceDetail(projectId: String, templateIds: Collection<String>): Result<Map<String, Int>> {
        return Result(templateService.serviceCountTemplateInstancesDetail(projectId, templateIds))
    }

    override fun listTemplate(userId: String, projectId: String, templateId: String): Result<TemplateInstances> {
        return Result(templateService.listTemplateInstances(projectId, userId, templateId))
    }

    override fun listTemplateInstancesParams(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        pipelineIds: List<PipelineId>
    ): Result<Map<String, TemplateInstanceParams>> {
        return Result(templateService.listTemplateInstancesParams(userId, projectId, templateId, version, pipelineIds.map { it.id }.toSet()))
    }
}