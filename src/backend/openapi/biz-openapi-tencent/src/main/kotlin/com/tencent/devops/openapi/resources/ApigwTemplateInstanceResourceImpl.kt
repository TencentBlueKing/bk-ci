package com.tencent.devops.openapi.resources

import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired
import com.tencent.devops.common.client.Client
import com.tencent.devops.openapi.api.ApigwTemplateInstanceResource
import com.tencent.devops.process.api.template.ServiceTemplateInstanceResource
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateOperationRet
import org.slf4j.LoggerFactory

@RestResource
class ApigwTemplateInstanceResourceImpl @Autowired constructor(private val client: Client) :
    ApigwTemplateInstanceResource {
    override fun createTemplateInstances(
        userId: String,
        projectId: String,
        templateId: String,
        version: Long,
        useTemplateSettings: Boolean,
        instances: List<TemplateInstanceCreate>
    ): TemplateOperationRet {
        logger.info("create TemplateInstances :userId=$userId,projectId=$projectId,templateId:$templateId,version:$version,useTemplateSettings:$useTemplateSettings,instances:$instances")
        return client.get(ServiceTemplateInstanceResource::class).createTemplateInstances(
            userId,
            projectId,
            templateId,
            version,
            useTemplateSettings,
            instances)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTemplateInstanceResourceImpl::class.java)
    }
}