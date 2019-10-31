package com.tencent.devops.process.api.quality

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.quality.UserQualityTemplateResource
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.pojo.template.TemplateListModel
import com.tencent.devops.process.pojo.template.TemplateModelDetail
import com.tencent.devops.process.pojo.template.TemplateType
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserQualityTemplateResourceImpl @Autowired constructor(
    private val templateService: TemplateService
) : UserQualityTemplateResource {
    override fun listTemplate(
        userId: String,
        projectId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?,
        keywords: String?
    ): Result<TemplateListModel> {
        return Result(templateService.listTemplate(
            projectId, userId, templateType, storeFlag, page, pageSize, keywords))
    }

    override fun getTemplateInfo(userId: String, projectId: String, templateId: String): Result<TemplateModelDetail> {
        return Result(templateService.getTemplate(
            projectId, userId, templateId, null))
    }
}