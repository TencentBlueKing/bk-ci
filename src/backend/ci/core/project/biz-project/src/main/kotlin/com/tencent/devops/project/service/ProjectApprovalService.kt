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
 *
 */

package com.tencent.devops.project.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.pojo.SubjectScopeInfo
import com.tencent.devops.common.auth.callback.AuthConstants
import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectApprovalDao
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectUpdateHistoryDao
import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.ProjectCreateExtInfo
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectProperties
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.pojo.enums.ProjectTipsStatus
import com.tencent.devops.project.pojo.mq.ProjectUpdateBroadCastEvent
import com.tencent.devops.project.pojo.mq.ProjectUpdateLogoBroadCastEvent
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("TooManyFunctions")
class ProjectApprovalService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectApprovalDao: ProjectApprovalDao,
    private val projectDao: ProjectDao,
    private val projectExtService: ProjectExtService,
    private val projectUpdateHistoryDao: ProjectUpdateHistoryDao,
    private val projectDispatcher: SampleEventDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectApprovalService::class.java)
        private const val REVOKE_ITSM_APPLICATION = "REVOKED"
    }

    fun create(
        userId: String,
        projectCreateInfo: ProjectCreateInfo,
        approvalStatus: Int,
        subjectScopes: List<SubjectScopeInfo>,
        tipsStatus: Int
    ): Int {
        return projectApprovalDao.create(
            dslContext = dslContext,
            userId = userId,
            projectCreateInfo = projectCreateInfo,
            approvalStatus = approvalStatus,
            subjectScopes = subjectScopes,
            tipsStatus = tipsStatus
        )
    }

    fun update(
        userId: String,
        projectUpdateInfo: ProjectUpdateInfo,
        approvalStatus: Int,
        subjectScopes: List<SubjectScopeInfo>
    ): Int {
        val tipsStatus = when (approvalStatus) {
            ProjectApproveStatus.CREATE_PENDING.status -> ProjectTipsStatus.SHOW_CREATE_PENDING.status
            ProjectApproveStatus.UPDATE_PENDING.status -> ProjectTipsStatus.SHOW_UPDATE_PENDING.status
            else -> ProjectTipsStatus.SHOW_SUCCESSFUL_UPDATE.status
        }
        return projectApprovalDao.update(
            dslContext = dslContext,
            userId = userId,
            projectUpdateInfo = projectUpdateInfo,
            approvalStatus = approvalStatus,
            subjectScopes = subjectScopes,
            tipsStatus = tipsStatus
        )
    }

    fun rollBack(
        projectApprovalInfo: ProjectApprovalInfo
    ) {
        projectApprovalDao.update(
            dslContext = dslContext,
            projectApprovalInfo = projectApprovalInfo
        )
    }

    /**
     * 用户主动更新审批状态，如取消
     */
    fun cancelUpdate(
        userId: String,
        projectId: String,
        approvalStatus: Int
    ): Int {
        return projectApprovalDao.updateApprovalStatusByUser(
            dslContext = dslContext,
            projectId = projectId,
            userId = userId,
            approvalStatus = approvalStatus,
            tipsStatus = ProjectTipsStatus.NOT_SHOW.status
        )
    }

    fun delete(projectId: String) {
        logger.info("delete project approval info:$projectId")
        projectApprovalDao.delete(dslContext = dslContext, projectId = projectId)
    }

    fun get(projectId: String): ProjectApprovalInfo? {
        return projectApprovalDao.getByEnglishName(dslContext = dslContext, englishName = projectId)
    }

    @Suppress("ComplexMethod")
    fun createApproved(projectId: String, applicant: String, approver: String) {
        logger.info("project create approved|$projectId|$applicant|$approver")
        val projectInfo =
            projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectId),
                defaultMessage = "project $projectId is not exist"
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
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatusByCallback(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.APPROVED.status,
                tipsStatus = ProjectTipsStatus.SHOW_SUCCESSFUL_CREATE.status
            )
            projectDao.updateApprovalStatus(
                dslContext = context,
                englishName = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.APPROVED.status
            )
            createExtProjectInfo(applicant, projectInfo.projectId, projectCreateInfo, projectInfo, projectId)
        }
    }

    private fun createExtProjectInfo(
        applicant: String,
        authProjectId: String,
        projectCreateInfo: ProjectCreateInfo,
        projectInfo: TProjectRecord,
        projectId: String
    ) {
        try {
            projectExtService.createExtProjectInfo(
                userId = applicant,
                authProjectId = authProjectId,
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

    fun createRejectOrRevoke(
        projectId: String,
        itsmTicketStatus: String,
        applicant: String,
        approver: String
    ) {
        logger.info("create project reject or revoke|$projectId|$itsmTicketStatus|$applicant|$approver")
        projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(projectId),
            defaultMessage = "project $projectId is not exist"
        )
        val tipsStatus =
            if (itsmTicketStatus == REVOKE_ITSM_APPLICATION) ProjectTipsStatus.SHOW_CREATE_REVOKE.status
            else ProjectTipsStatus.SHOW_CREATE_REJECT.status
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatusByCallback(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.CREATE_REJECT.status,
                tipsStatus = tipsStatus
            )
            projectDao.updateApprovalStatus(
                dslContext = context,
                englishName = projectId,
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
        // 属性只能变更前端展示的,其他的字段由op变更
        val projectProperties = projectInfo.properties?.let { JsonUtil.to(it, ProjectProperties::class.java) }
        val updateProjectProperties = projectApprovalInfo.properties?.let { projectProperties?.userCopy(it) }
        val projectUpdateInfo = with(projectApprovalInfo) {
            ProjectUpdateInfo(
                projectName = projectName,
                englishName = englishName,
                description = description ?: "",
                bgId = bgId?.toLong() ?: 0L,
                bgName = bgName ?: "",
                businessLineId = businessLineId,
                businessLineName = businessLineName,
                deptId = deptId?.toLong() ?: 0L,
                deptName = deptName ?: "",
                centerId = centerId?.toLong() ?: 0L,
                centerName = centerName ?: "",
                logoAddress = logoAddr,
                subjectScopes = subjectScopes,
                authSecrecy = authSecrecy,
                ccAppId = projectInfo.ccAppId,
                ccAppName = projectInfo.ccAppName,
                kind = projectInfo.kind,
                projectType = projectType ?: 0,
                productId = projectApprovalInfo.productId,
                properties = updateProjectProperties
            )
        }
        val logoAddress = projectUpdateInfo.logoAddress
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatusByCallback(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.APPROVED.status,
                tipsStatus = ProjectTipsStatus.SHOW_SUCCESSFUL_UPDATE.status
            )
            projectDao.update(
                dslContext = context,
                userId = applicant,
                projectId = projectInfo.projectId,
                projectUpdateInfo = projectUpdateInfo,
                subjectScopesStr = JsonUtil.toJson(projectUpdateInfo.subjectScopes!!),
                logoAddress = logoAddress
            )
            projectUpdateHistoryDao.updateProjectHistoryStatus(
                dslContext = context,
                englishName = projectId,
                approvalStatus = ProjectApproveStatus.APPROVED.status
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

    fun updateRejectOrRevoke(
        projectId: String,
        itsmTicketStatus: String,
        applicant: String,
        approver: String
    ) {
        logger.info("project update reject|$projectId|$applicant|$approver")
        projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
            errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
            params = arrayOf(projectId),
            defaultMessage = "project $projectId is not exist"
        )
        val tipsStatus =
            if (itsmTicketStatus == REVOKE_ITSM_APPLICATION) ProjectTipsStatus.NOT_SHOW.status
            else ProjectTipsStatus.SHOW_UPDATE_REJECT.status
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            projectApprovalDao.updateApprovalStatusByCallback(
                dslContext = context,
                projectCode = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.APPROVED.status,
                tipsStatus = tipsStatus
            )
            projectDao.updateApprovalStatus(
                dslContext = context,
                englishName = projectId,
                approver = approver,
                approvalStatus = ProjectApproveStatus.APPROVED.status
            )
            projectUpdateHistoryDao.updateProjectHistoryStatus(
                dslContext = context,
                englishName = projectId,
                approvalStatus = ProjectApproveStatus.UPDATE_REJECT_OR_REVOKE.status
            )
        }
    }

    fun updateTipsStatus(projectId: String, tipsStatus: Int) {
        projectApprovalDao.updateTipsStatus(
            dslContext = dslContext,
            projectId = projectId,
            tipsStatus = tipsStatus
        )
    }

    /**
     * 升级权限时,创建项目审批单
     */
    fun createMigration(projectId: String) {
        logger.info("project create migration|$projectId")
        val projectInfo =
            projectDao.getByEnglishName(dslContext = dslContext, englishName = projectId) ?: throw ErrorCodeException(
                errorCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                params = arrayOf(projectId),
                defaultMessage = "project $projectId is not exist"
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
        create(
            userId = projectInfo.creator,
            projectCreateInfo = projectCreateInfo,
            approvalStatus = ProjectApproveStatus.APPROVED.status,
            subjectScopes = listOf(
                SubjectScopeInfo(
                    id = AuthConstants.ALL_MEMBERS,
                    type = AuthConstants.ALL_MEMBERS,
                    name = AuthConstants.ALL_MEMBERS_NAME
                )
            ),
            tipsStatus = ProjectTipsStatus.NOT_SHOW.status
        )
    }
}
