package com.tencent.devops.project.service.impl

import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.enums.OrganizationType
import com.tencent.devops.project.service.ProjectOrganizationService
import com.tencent.devops.project.service.tof.TOFService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectOrganizationServiceImpl @Autowired constructor(private val tofService: TOFService) : ProjectOrganizationService {

	override fun getDeptInfo(userId: String, id: Int): DeptInfo {
		return tofService.getDeptInfo(userId, id)
	}

	override fun getOrganizations(userId: String, type: OrganizationType, id: Int): List<OrganizationInfo> {
		return tofService.getOrganizationInfo(userId, type, id)
	}

	override fun getParentDeptInfos(deptId: String, level: Int): List<DeptInfo> {
		return tofService.getParentDeptInfo(deptId, level)
	}
}