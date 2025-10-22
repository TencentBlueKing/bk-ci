/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service

import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrgInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.StaffInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import org.springframework.stereotype.Service

@Service
class SampleProjectOrganizationServiceImpl : ProjectOrganizationService {
    override fun getDeptInfo(userId: String?, id: Int): DeptInfo {
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
                id = id.toString(), name = "mock Corp"
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

    override fun isOrgProject(projectId: String, orgInfos: OrgInfo): Boolean {
        return true
    }

    override fun getDeptStaffsWithLevel(deptId: String, level: Int): List<StaffInfo> {
        val mock = mutableListOf<StaffInfo>()
        mock.add(
            StaffInfo(
                loginName = "mock",
                departmentName = "mock Dept",
                fullName = "mock full name",
                chineseName = "模拟",
                groupId = "0",
                groupName = "mock group",
                statusId = "0"
            )
        )
        return mock
    }
}
