package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.enums.OrganizationType

interface ProjectOrganizationService {
	fun getDeptInfo(userId: String, id: Int): DeptInfo

	fun getOrganizations(userId: String, type: OrganizationType, id: Int): List<OrganizationInfo>

	fun getParentDeptInfos(deptId: String, level: Int): List<DeptInfo>
}