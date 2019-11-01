package com.tencent.devops.process.api.v2.template

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.v2.template.ServiceProjectTemplateResource
import com.tencent.devops.process.engine.service.template.TemplateService
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateType
import org.springframework.beans.factory.annotation.Autowired

/**
 * 统计多个项目下的模板接口实现
 */
@RestResource
class ServiceProjectTemplateResourceImpl @Autowired constructor(
    private val templateService: TemplateService
) : ServiceProjectTemplateResource {
    override fun listTemplateByProjectIds(
        userId: String,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int,
        pageSize: Int,
        channelCode: ChannelCode?,
        checkPermission: Boolean?,
        projectIds: Set<String>
    ): Result<Page<TemplateModel>> {
        return Result(
            templateService.listTemplateByProjectIds(
                userId = userId,
                templateType = templateType,
                storeFlag = storeFlag,
                page = page,
                pageSize = pageSize,
                keywords = null,
                projectIds = projectIds
            )
        )
    }
}