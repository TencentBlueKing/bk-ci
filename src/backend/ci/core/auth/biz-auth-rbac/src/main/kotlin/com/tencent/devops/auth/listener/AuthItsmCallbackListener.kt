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

package com.tencent.devops.auth.listener

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthItsmCallbackDao
import com.tencent.devops.auth.pojo.ItsmCallBackInfo
import com.tencent.devops.auth.pojo.enums.AuthItsmApprovalType
import com.tencent.devops.auth.pojo.event.AuthItsmCallbackEvent
import com.tencent.devops.auth.service.PermissionGradeManagerService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.event.listener.trace.BaseTraceListener
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.auth.tables.records.TAuthItsmCallbackRecord
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

class AuthItsmCallbackListener @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val authItsmCallbackDao: AuthItsmCallbackDao,
    private val permissionGradeManagerService: PermissionGradeManagerService,
    traceEventDispatcher: TraceEventDispatcher
) : BaseTraceListener<AuthItsmCallbackEvent>(traceEventDispatcher) {

    companion object {
        private const val REVOKE_ITSM_APPLICATION_ACTION = "REVOKED"
    }

    override fun run(event: AuthItsmCallbackEvent) {
        when (event.approveType) {
            AuthItsmApprovalType.CREATE.name ->
                createProjectCallBack(itsmCallBackInfo = event.itsmCallBackInfo)
            AuthItsmApprovalType.UPDATE.name ->
                updateProjectCallBack(itsmCallBackInfo = event.itsmCallBackInfo)
        }
    }

    private fun createProjectCallBack(itsmCallBackInfo: ItsmCallBackInfo) {
        logger.info("auth itsm create callback info:$itsmCallBackInfo")
        val sn = itsmCallBackInfo.sn
        // 蓝盾数据库存储的回调信息
        val callBackInfo = authItsmCallbackDao.getCallbackBySn(dslContext, sn) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_ITSM_CALLBACK_APPLICATION_FAIL,
            params = arrayOf(sn),
            defaultMessage = "itsm application form $sn does not exist"
        )
        // 审批单不存在或者已经结束
        if (callBackInfo.approveResult != null) {
            logger.warn("itsm application has ended, no need to handle|itsmCallBackInfo|$itsmCallBackInfo")
            return
        }
        when (itsmCallBackInfo.currentStatus) {
            REVOKE_ITSM_APPLICATION_ACTION -> revokeCreateProject(
                projectId = getProjectId(callBackInfo.englishName),
                userId = callBackInfo.applicant
            )
            else -> {
                denyOrApproveCreateProject(
                    callBackInfo = callBackInfo,
                    itsmCallBackInfo = itsmCallBackInfo
                )
            }
        }
    }

    private fun updateProjectCallBack(
        itsmCallBackInfo: ItsmCallBackInfo
    ) {
        logger.info("auth itsm update callback info:$itsmCallBackInfo")
        val sn = itsmCallBackInfo.sn
        // 蓝盾数据库存储的回调信息
        val callBackInfo = authItsmCallbackDao.getCallbackBySn(dslContext, sn) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_ITSM_CALLBACK_APPLICATION_FAIL,
            params = arrayOf(sn),
            defaultMessage = "itsm application form $sn does not exist"
        )
        // 审批单不存在或者已经结束
        if (callBackInfo.approveResult != null) {
            logger.warn("itsm application has ended, no need to handle|itsmCallBackInfo|$itsmCallBackInfo")
            return
        }
        when (itsmCallBackInfo.currentStatus) {
            REVOKE_ITSM_APPLICATION_ACTION -> {
                revokeUpdateProject(
                    projectId = getProjectId(callBackInfo.englishName),
                    userId = callBackInfo.applicant
                )
            }
            else -> {
                denyOrApproveUpdateProject(
                    callBackInfo = callBackInfo,
                    itsmCallBackInfo = itsmCallBackInfo
                )
            }
        }
    }

    private fun revokeCreateProject(
        projectId: String,
        userId: String
    ) {
        logger.info("revoke create project:$projectId|$userId")
        client.get(ServiceProjectResource::class).cancelCreateProject(
            userId = userId,
            projectId = projectId
        )
    }

    private fun denyOrApproveCreateProject(
        callBackInfo: TAuthItsmCallbackRecord,
        itsmCallBackInfo: ItsmCallBackInfo
    ) {
        logger.info("deny or approve create project:$callBackInfo|$itsmCallBackInfo")
        val sn = itsmCallBackInfo.sn
        val approveResult = itsmCallBackInfo.approveResult.toBoolean()
        val englishName = callBackInfo.englishName
        val projectInfo =
            client.get(ServiceProjectResource::class).get(englishName = englishName).data ?: throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                    defaultMessage = "The project does not exist! | englishName = $englishName"
                )
            )
        if (projectInfo.approvalStatus != ProjectApproveStatus.CREATE_PENDING.status) {
            logger.info("This project status not create pending! englishName = $englishName")
            return
        }
        val callBackId = callBackInfo.callbackId
        val currentStatus = itsmCallBackInfo.currentStatus
        authItsmCallbackDao.updateCallbackBySn(
            dslContext = dslContext,
            sn = sn,
            approver = itsmCallBackInfo.lastApprover,
            approveResult = approveResult
        )
        if (approveResult) {
            permissionGradeManagerService.handleItsmCreateCallback(
                userId = callBackInfo.applicant,
                projectCode = englishName,
                projectName = projectInfo.projectName,
                sn = sn,
                callBackId = callBackId,
                currentStatus = currentStatus
            )
            client.get(ServiceProjectApprovalResource::class).createApproved(
                projectId = englishName,
                applicant = callBackInfo.applicant,
                approver = itsmCallBackInfo.lastApprover
            )
        } else {
            permissionGradeManagerService.rejectCancelApplication(callBackId = callBackId)
            client.get(ServiceProjectApprovalResource::class).createReject(
                projectId = englishName,
                applicant = callBackInfo.applicant,
                approver = itsmCallBackInfo.lastApprover
            )
        }
    }

    private fun revokeUpdateProject(
        projectId: String,
        userId: String
    ) {
        logger.info("revoke update project:$projectId|$userId")
        client.get(ServiceProjectResource::class).cancelUpdateProject(
            userId = userId,
            projectId = projectId
        )
    }

    private fun denyOrApproveUpdateProject(
        callBackInfo: TAuthItsmCallbackRecord,
        itsmCallBackInfo: ItsmCallBackInfo
    ) {
        logger.info("deny or approve update project:$callBackInfo|$itsmCallBackInfo")
        val sn = itsmCallBackInfo.sn
        val approveResult = itsmCallBackInfo.approveResult.toBoolean()
        val englishName = callBackInfo.englishName
        val projectInfo =
            client.get(ServiceProjectResource::class).get(englishName = englishName).data ?: throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                    defaultMessage = "The project does not exist! | englishName = $englishName"
                )
            )
        if (projectInfo.approvalStatus == ProjectApproveStatus.APPROVED.status) {
            logger.info("The project is already in normal state and cannot be updated|englishName = $englishName")
            return
        }
        val callBackId = callBackInfo.callbackId
        val currentStatus = itsmCallBackInfo.currentStatus
        authItsmCallbackDao.updateCallbackBySn(
            dslContext = dslContext,
            sn = sn,
            approver = itsmCallBackInfo.lastApprover,
            approveResult = approveResult
        )
        if (approveResult) {
            permissionGradeManagerService.handleItsmUpdateCallback(
                userId = callBackInfo.applicant,
                projectCode = englishName,
                projectName = projectInfo.projectName,
                sn = sn,
                callBackId = callBackId,
                currentStatus = currentStatus
            )
            client.get(ServiceProjectApprovalResource::class).updateApproved(
                projectId = englishName,
                applicant = callBackInfo.applicant,
                approver = itsmCallBackInfo.lastApprover
            )
        } else {
            permissionGradeManagerService.rejectCancelApplication(callBackId = callBackId)
            client.get(ServiceProjectApprovalResource::class).updateReject(
                projectId = englishName,
                applicant = callBackInfo.applicant,
                approver = itsmCallBackInfo.lastApprover
            )
        }
    }

    fun getProjectId(projectCode: String): String {
        return client.get(ServiceProjectResource::class).get(englishName = projectCode).data?.projectId
            ?: throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                    defaultMessage = "The project does not exist! | englishName = $projectCode"
                )
            )
    }
}
