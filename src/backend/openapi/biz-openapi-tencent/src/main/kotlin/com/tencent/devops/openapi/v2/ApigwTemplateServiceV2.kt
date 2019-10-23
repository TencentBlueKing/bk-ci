package com.tencent.devops.openapi.service.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OrganizationUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.openapi.exception.MicroServiceInvokeFailure
import com.tencent.devops.process.api.v2.template.ServiceProjectTemplateResource
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateType
import com.tencent.devops.project.api.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * @Description
 * @Date 2019/9/1
 * @Version 1.0
 */
@Service
class ApigwTemplateServiceV2(
    private val client: Client
) {
    fun listTemplateByOrganization(
        userId: String,
        organizationType: String,
        organizationName: String,
        deptName: String?,
        centerName: String?,
        templateType: TemplateType?,
        storeFlag: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<TemplateModel>> {
        logger.info("listTemplateByOrganization|$userId,$organizationType,$organizationName,$deptName,$centerName,$page,$pageSize")
        val organization = OrganizationUtil.fillOrganization(
            organizationType = organizationType,
            organizationName = organizationName,
            deptName = deptName,
            centerName = centerName
        )
        val projectsResult = client.get(ServiceProjectResource::class).getProjectByGroup(
            userId = userId,
            bgName = organization.bgName,
            deptName = organization.deptName,
            centerName = organization.centerName
        )
        // 项目接口内容判空
        if (projectsResult.isNotOk()) {
            val resultStr = JsonUtil.toJson(projectsResult)
            throw MicroServiceInvokeFailure(
                "project:ServiceProjectResource:getProjectByGroup",
                "projectsResult=$resultStr"
            )
        }
        // 2.根据所有项目Id获取对应模板
        val projectIds = projectsResult.data!!.map { it.english_name }.toSet()
        val templatesResult = client.getWithoutRetry(ServiceProjectTemplateResource::class).listTemplateByProjectIds(
            userId = userId,
            templateType = templateType,
            storeFlag = storeFlag,
            page = if (page == null || page <= 0) 1 else page,
            pageSize = if (pageSize == null || pageSize <= 0) 20 else pageSize,
            channelCode = ChannelCode.BS,
            checkPermission = false,
            projectIds = projectIds
        )
        val resultStr = JsonUtil.toJson(templatesResult)
        if (templatesResult.isNotOk()) {
            throw MicroServiceInvokeFailure(
                "process:ServiceProjectTemplateResource:listTemplateByProjectIds",
                "templatesResult=$resultStr"
            )
        }
        return templatesResult
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTemplateServiceV2::class.java)
    }
}