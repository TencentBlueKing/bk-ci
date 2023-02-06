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
 *
 */

package com.tencent.devops.project.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectApprovalDao
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dispatch.ProjectDispatcher
import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectApprovalService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectApprovalDao: ProjectApprovalDao,
    private val projectDao: ProjectDao,
    private val projectExtService: ProjectExtService,
    private val projectDispatcher: ProjectDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectApprovalService::class.java)
    }

    fun create(
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        approvalStatus: Int,
        subjectScopes: List<SubjectScopeInfo>
    ): Int {
        return projectApprovalDao.create(
            dslContext = dslContext,
            userId = userId,
            projectCreateInfo = projectCreateInfo,
            approvalStatus = approvalStatus,
            subjectScopes = subjectScopes
        )
    }

    fun update(
        userId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        approvalStatus: Int,
        subjectScopes: List<SubjectScopeInfo>
    ): Int {
        return projectApprovalDao.update(
            dslContext = dslContext,
            userId = userId,
            projectUpdateInfo = projectUpdateInfo,
            approvalStatus = approvalStatus,
            subjectScopes = subjectScopes
        )
    }

    fun get(projectId: String): ProjectApprovalInfo? {
        return projectApprovalDao.getByEnglishName(dslContext = dslContext, englishName = projectId)
    }

    fun createApproved(projectId: String, applicant: String, approver: String) {
        logger.info("project create approved|$projectId|$applicant|$approver")
        val projectInfo =
            projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectId),
                defaultMessage = "project $projectId is not exist"
            )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatus(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.CREATE_APPROVED.status
            )
            projectDao.updateApprovalStatus(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.CREATE_APPROVED.status
            )
            val projectCreateInfo = with(projectInfo) {
                ProjectCreateInfo(
                    projectName = projectName,
                    englishName = englishName,
                    projectType = projectType ?: 0,
                    description = description ?: "",
                    bgId = bgId?.toLong() ?: 0L,
                    bgName = bgName ?: "",
                    deptId = deptId?.toLong() ?: 0L,
                    deptName = deptName ?: "",
                    centerId = centerId?.toLong() ?: 0L,
                    centerName = centerName ?: "",
                    kind = kind ?: 0,
                    logoAddress = logoAddr
                )
            }
            try {
                projectExtService.createExtProjectInfo(
                    userId = applicant,
                    projectId = projectId,
                    accessToken = null,
                    projectCreateInfo = projectCreateInfo,
                    createExtInfo = ProjectCreateExtInfo(needValidate = true, needAuth = true),
                    logoAddress = projectInfo.logoAddr
                )
            } catch (ignore: Exception) {
                logger.warn("fail to create the project[$projectId] ext info $projectInfo", ignore)
                projectDao.delete(dslContext, projectId)
                throw ignore
            }
        }
    }

    fun createReject(projectId: String, applicant: String, approver: String) {
        logger.info("project create reject|$projectId|$applicant|$approver")
        projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(projectId),
            defaultMessage = "project $projectId is not exist"
        )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatus(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.CREATE_REJECT.status
            )
            projectDao.updateApprovalStatus(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.CREATE_REJECT.status
            )
        }
    }

    @Suppress("LongMethod")
    fun updateApproved(projectId: String, applicant: String, approver: String) {
        logger.info("project update approved|$projectId|$applicant|$approver")
        val projectInfo =
            projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectId),
                defaultMessage = "project $projectId is not exist"
            )
        val projectApprovalInfo =
            projectApprovalDao.getByEnglishName(dslContext = dslContext, englishName = projectId)
                ?: throw ErrorCodeException(
                    errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                    params = arrayOf(projectId),
                    defaultMessage = "project $projectId is not exist"
                )
        val projectUpdateInfo = with(projectApprovalInfo) {
            ProjectUpdateInfo(
                projectName = projectName,
                englishName = englishName,
                description = description ?: "",
                bgId = bgId?.toLong() ?: 0L,
                bgName = bgName ?: "",
                deptId = deptId?.toLong() ?: 0L,
                deptName = deptName ?: "",
                centerId = centerId?.toLong() ?: 0L,
                centerName = centerName ?: "",
                logoAddress = logoAddr,
                subjectScopes = subjectScopes,
                authSecrecy = authSecrecy,
                ccAppId = projectInfo.ccAppId,
                ccAppName = projectInfo.ccAppName,
                kind = projectInfo.kind
            )
        }
        val logoAddress = projectUpdateInfo.logoAddress
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatus(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.UPDATE_APPROVED.status
            )
            projectDao.update(
                dslContext = context,
                userId = applicant,
                projectId = projectInfo.projectId,
                projectUpdateInfo = projectUpdateInfo,
                subjectScopesStr = JsonUtil.toJson(projectUpdateInfo.subjectScopes!!),
                logoAddress = logoAddress,
                authSecrecy = projectUpdateInfo.authSecrecy
            )
            projectDispatcher.dispatch(
                ProjectUpdateBroadCastEvent(
                    userId = applicant,
                    projectId = projectInfo.projectId,
                    projectInfo = projectUpdateInfo
                )
            )
            if (logoAddress != null) {
                projectDispatcher.dispatch(
                    ProjectUpdateLogoBroadCastEvent(
                        userId = applicant,
                        projectId = projectInfo.projectId,
                        logoAddr = logoAddress
                    )
                )
            }
        }
    }

    fun updateReject(projectId: String, applicant: String, approver: String) {
        logger.info("project update reject|$projectId|$applicant|$approver")
        projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(projectId),
            defaultMessage = "project $projectId is not exist"
        )
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatus(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.UPDATE_REJECT.status
            )
            projectDao.updateApprovalStatus(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.UPDATE_REJECT.status
            )
        }
    }
}
