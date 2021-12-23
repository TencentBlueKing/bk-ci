/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.api.pojo.ProjectOrganization
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectFreshDao
import com.tencent.devops.project.pojo.ProjectDeptInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectTxInfoService @Autowired constructor(
    val projectDao: ProjectDao,
    val projectFreshDao: ProjectFreshDao,
    val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectTxInfoService::class.java)
        private const val MAX_PROJECT_NAME_LENGTH = 64
    }

    fun getProjectOrganizations(projectCode: String): ProjectOrganization? {
        val projectInfo = projectDao.getByEnglishName(dslContext, projectCode) ?: return null
        return ProjectOrganization(
                projectId = projectInfo.id.toString(),
                projectEnglishName = projectInfo.englishName,
                projectName = projectInfo.projectName,
                projectType = projectInfo.projectType,
                bgId = projectInfo.bgId,
                bgName = projectInfo.bgName,
                centerId = projectInfo.centerId,
                centerName = projectInfo.centerName,
                deptId = projectInfo.deptId,
                deptName = projectInfo.deptName
        )
    }

    fun updateProjectName(
        userId: String,
        projectCode: String,
        projectName: String
    ): Boolean {
        // projectName表字段长度调整到64位限制
        if (projectName.isEmpty() || projectName.length > MAX_PROJECT_NAME_LENGTH) {
            throw ErrorCodeException(
                defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_TOO_LONG),
                errorCode = ProjectMessageCode.NAME_TOO_LONG
            )
        }
        if (projectDao.existByProjectName(dslContext, projectName, projectCode)) {
            throw ErrorCodeException(
                defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.PROJECT_NAME_EXIST),
                errorCode = ProjectMessageCode.PROJECT_NAME_EXIST
            )
        }
        return projectFreshDao.updateProjectName(
            dslContext = dslContext,
            userId = userId,
            englishName = projectCode,
            projectName = projectName
        ) > 0
    }

    fun getProjectInfoByProjectName(
        userId: String,
        projectName: String
    ): TProjectRecord? {
        logger.info("PROJECT|userId|$userId|projectName|$projectName")
        if (projectName.isEmpty()) {
            throw ErrorCodeException(
                defaultMessage = MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.NAME_EMPTY),
                errorCode = ProjectMessageCode.NAME_EMPTY
            )
        }

        return projectFreshDao.getProjectInfoByProjectName(
            dslContext = dslContext,
            userId = userId,
            projectName = projectName
        )
    }

    fun bindProjectDept(
        userId: String,
        projectCode: String,
        projectDeptInfo: ProjectDeptInfo
    ): Boolean {
        projectFreshDao.bindProjectDept(
            dslContext = dslContext,
            projectId = projectCode,
            projectDeptInfo = projectDeptInfo,
            updateUser = userId
        )
        return true
    }
}
