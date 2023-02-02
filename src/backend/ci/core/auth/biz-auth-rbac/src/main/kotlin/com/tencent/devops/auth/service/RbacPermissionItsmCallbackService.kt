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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthItsmCallbackDao
import com.tencent.devops.auth.pojo.ItsmCallBackInfo
import com.tencent.devops.auth.service.iam.PermissionItsmCallbackService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class RbacPermissionItsmCallbackService constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val authItsmCallbackDao: AuthItsmCallbackDao,
    private val permissionGradeManagerService: PermissionGradeManagerService
) : PermissionItsmCallbackService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionItsmCallbackService::class.java)
    }

    override fun createProjectCallBack(itsmCallBackInfo: ItsmCallBackInfo) {
        logger.info("itsm callback info:$itsmCallBackInfo")
        val sn = itsmCallBackInfo.sn
        val approveResult = itsmCallBackInfo.approveResult.toBoolean()
        // 蓝盾数据库存储的回调信息
        val callBackInfo = authItsmCallbackDao.getCallbackBySn(dslContext, sn) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ITSM_CALLBACK_APPLICATION_FAIL,
            params = arrayOf(sn),
            defaultMessage = "The itsm callback application does not exist!| sn = $sn"
        )
        val englishName = callBackInfo.englishName
        val projectInfo =
            client.get(ServiceProjectResource::class).get(englishName = englishName).data ?: throw OperationException(
                MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.PROJECT_NOT_EXIST,
                    defaultMessage = "The project does not exist! | englishName = $englishName"
                )
            )
        if (projectInfo.approvalStatus == ProjectApproveStatus.CANCEL_CREATE.status) {
            logger.info("This project has been canceled create! englishName = $englishName")
            return
        }
        val callBackId = callBackInfo.callbackId
        val currentStatus = itsmCallBackInfo.currentStatus
        logger.info("createProjectCallBack: ${itsmCallBackInfo.title}|$sn|$approveResult|$englishName|$callBackId")
        authItsmCallbackDao.updateCallbackBySn(
            dslContext = dslContext,
            sn = sn,
            approver = itsmCallBackInfo.lastApprover,
            approveResult = approveResult
        )
        if (approveResult) {
            permissionGradeManagerService.handleItsmCallback(
                userId = callBackInfo.applicant,
                projectCode = englishName,
                projectName = projectInfo.projectName,
                sn = sn,
                callBackId = callBackId,
                currentStatus = currentStatus
            )
            client.get(ServiceProjectApprovalResource::class).updateApprovalStatus(
                projectId = englishName,
                approver = itsmCallBackInfo.lastApprover,
                approvalStatus = ProjectApproveStatus.CREATE_APPROVED.status
            )
        } else {
            client.get(ServiceProjectApprovalResource::class).updateApprovalStatus(
                projectId = englishName,
                approver = itsmCallBackInfo.lastApprover,
                approvalStatus = ProjectApproveStatus.CREATE_REJECT.status
            )
        }
    }
}
