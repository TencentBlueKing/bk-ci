package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.api.ApigwTemplateResource
import com.tencent.devops.process.api.template.ServiceTemplateResource
import com.tencent.devops.process.pojo.template.OptionalTemplateList
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import org.slf4j.LoggerFactory

@RestResource
class ApigwTemplateResourceImpl @Autowired constructor(private val client: Client) : ApigwTemplateResource {
    override fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?
    ): Result<TemplateListModel> {
        logger.info("get project's pipeline all template, projectId($projectId) by user $userId")
        return client.get(ServiceTemplateResource::class).listTemplate(userId, projectId, templateType, storeFlag)
    }

    override fun getTemplate(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long?
    ): Result<TemplateModelDetail> {
        logger.info("get project's pipeline template, projectId($projectId) templateId($templateId) version($version) by $userId")
        return client.get(ServiceTemplateResource::class).getTemplate(userId, projectId, templateId, version)
    }

    override fun listAllTemplate(userId: String, projectId: String): Result<OptionalTemplateList> {
        logger.info("get project's pipeline all template, projectId($projectId) by user $userId")
        return client.get(ServiceTemplateResource::class).listAllTemplate(userId, projectId, null)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTemplateResourceImpl::class.java)
    }
}