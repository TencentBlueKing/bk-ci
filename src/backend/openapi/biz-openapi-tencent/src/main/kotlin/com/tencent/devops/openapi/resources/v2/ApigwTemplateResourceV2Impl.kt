package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.v2.ApigwTemplateResourceV2
import com.tencent.devops.openapi.service.v2.ApigwTemplateServiceV2
import com.tencent.devops.process.pojo.template.TemplateModel
import com.tencent.devops.process.pojo.template.TemplateType
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTemplateResourceV2Impl @Autowired constructor(
    private val apigwTemplateService: ApigwTemplateServiceV2
) : ApigwTemplateResourceV2 {

    override fun listTemplateByOrganization(userId: String, organizationType: String, organizationName: String, deptName: String?, centerName: String?, templateType: TemplateType?, storeFlag: Boolean?, page: Int?, pageSize: Int?): Result<Page<TemplateModel>> {
        return apigwTemplateService.listTemplateByOrganization(
            userId = userId,
            organizationType = organizationType,
            organizationName = organizationName,
            deptName = deptName,
            centerName = centerName,
            templateType = templateType,
            storeFlag = storeFlag,
            page = page,
            pageSize = pageSize
        )
    }
}