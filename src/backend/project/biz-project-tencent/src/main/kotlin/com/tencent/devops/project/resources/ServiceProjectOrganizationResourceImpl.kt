package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.OrganizationType
import com.tencent.devops.project.service.tof.TOFService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceProjectOrganizationResourceImpl @Autowired constructor(private val tofService: TOFService) : ServiceProjectOrganizationResource {

    override fun getDeptInfo(userId: String, id: Int): Result<DeptInfo> {
        return Result(tofService.getDeptInfo(userId, id))
    }

    override fun getOrganizations(userId: String, type: OrganizationType, id: Int): Result<List<OrganizationInfo>> {
        return Result(tofService.getOrganizationInfo(userId, type, id))
    }

    override fun getParentDeptInfos(deptId: String, level: Int): Result<List<DeptInfo>> {
        return Result(tofService.getParentDeptInfo(deptId, level))
    }
}
