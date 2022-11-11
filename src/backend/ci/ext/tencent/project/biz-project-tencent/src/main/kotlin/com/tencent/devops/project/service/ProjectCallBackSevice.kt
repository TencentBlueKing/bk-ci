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

import com.tencent.bk.sdk.iam.dto.CallbackApplicationDTO
import com.tencent.bk.sdk.iam.service.ManagerService
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.pojo.ItsmCallBackInfo
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectApprovalCallbackDao
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.enums.ApproveStatus
import com.tencent.devops.project.service.iam.IamV3Service
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProjectCallBackSevice @Autowired constructor(
    private val dslContext: DSLContext,
    private val iamManagerService: ManagerService,
    private val projectDao: ProjectDao,
    private val projectApprovalCallbackDao: ProjectApprovalCallbackDao,
    private val iamV3Service: IamV3Service
) {
    fun createProjectCallBack(itsmCallBackInfo: ItsmCallBackInfo) {
        logger.info("createProjectCallBack: itsmCallBackInfo = $itsmCallBackInfo")
        val sn = itsmCallBackInfo.sn
        val approveResult = itsmCallBackInfo.approveResult.toBoolean()
        // 蓝盾数据库存储的回调信息
        val callBackInfo = projectApprovalCallbackDao.getCallbackBySn(dslContext, sn)
            ?: throw OperationException(
                MessageCodeUtil.getCodeLanMessage
                (ProjectMessageCode.QUERY_PROJECT_CALLBACK_APPLICATION_FAIL)
            )
        val englishName = callBackInfo.englishName
        val projectInfo = projectDao.getByEnglishName(dslContext, englishName)
            ?: throw OperationException(MessageCodeUtil.getCodeLanMessage(ProjectMessageCode.QUERY_PROJECT_FAIL))
        val callBackId = callBackInfo.callbackId
        val currentStatus = itsmCallBackInfo.currentStatus
        if (approveResult) {
            // 调起iam处理分级管理员创建申请
            val callbackApplicationDTO = CallbackApplicationDTO
                .builder()
                .sn(sn)
                .currentStatus(currentStatus)
                .approveResult(approveResult).build()
            val iamCallbackApplication = iamManagerService.handleCallbackApplication(callBackId, callbackApplicationDTO)
            val gradeManagerId = iamCallbackApplication.roleId
            // 创建默认组
            iamV3Service.batchCreateDefaultGroups(
                userId = callBackInfo.applicant,
                gradeManagerId = gradeManagerId,
                projectCode = englishName,
                projectName = projectInfo.projectName
            )
            // 项目关联分级管理id
            projectDao.updateRelationByCode(
                dslContext = dslContext,
                projectCode = englishName,
                relationId = gradeManagerId.toString()
            )
            // 修改状态
            projectDao.updateProjectStatusByEnglishName(
                dslContext = dslContext,
                projectCode = englishName,
                statusEnum = ApproveStatus.APPROVED
            )
            // 发成功创建消息给用户
        } else {
            // 修改状态
            projectDao.updateProjectStatusByEnglishName(
                dslContext = dslContext,
                projectCode = englishName,
                statusEnum = ApproveStatus.REJECT
            )
            // 发成功创建消息给用户
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCallBackSevice::class.java)
    }
}
