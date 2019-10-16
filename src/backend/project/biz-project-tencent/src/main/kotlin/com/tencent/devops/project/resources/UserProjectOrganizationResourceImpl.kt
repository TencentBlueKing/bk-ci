package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.user.UserProjectOrganizationResource
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.OrganizationType
import com.tencent.devops.project.service.tof.TOFService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectOrganizationResourceImpl @Autowired constructor(private val tofService: TOFService
) : UserProjectOrganizationResource {

    override fun getOrganizations(
            userId: String,
            type: OrganizationType,
            id: Int
    ): Result<List<OrganizationInfo>> {
        return Result(tofService.getOrganizationInfo(userId, type, id))
    }
}