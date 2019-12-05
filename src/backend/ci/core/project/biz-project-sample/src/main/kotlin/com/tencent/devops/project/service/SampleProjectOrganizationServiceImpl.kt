package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import org.springframework.stereotype.Service

@Service
class SampleProjectOrganizationServiceImpl : ProjectOrganizationService {
    override fun getDeptInfo(userId: String, id: Int): DeptInfo {
        return DeptInfo(
            typeId = "0",
            leaderId = "0",
            name = "mock Dept",
            level = "2",
            enabled = "true",
            parentId = "1",
            id = id.toString()
        )
    }

    override fun getOrganizations(userId: String, type: OrganizationType, id: Int): List<OrganizationInfo> {
        val mock = mutableListOf<OrganizationInfo>()
        mock.add(
            OrganizationInfo(
                ID = id.toString(), Name = "mock Corp"
            )
        )
        return mock
    }

    override fun getParentDeptInfos(deptId: String, level: Int): List<DeptInfo> {
        val mock = mutableListOf<DeptInfo>()
        mock.add(
            DeptInfo(
                typeId = "0",
                leaderId = "0",
                name = "mock Dept",
                level = level.toString(),
                enabled = "true",
                parentId = "1",
                id = deptId
            )
        )
        return mock
    }
}