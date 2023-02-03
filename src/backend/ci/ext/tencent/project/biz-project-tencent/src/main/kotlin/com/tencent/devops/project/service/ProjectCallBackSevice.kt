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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.bk.sdk.iam.dto.CallbackApplicationDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.project.api.pojo.ItsmCallBackInfo
import com.tencent.devops.project.constant.ProjectMessageCode
import com.tencent.devops.project.dao.ProjectApprovalCallbackDao
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.pojo.enums.ApproveType
import com.tencent.devops.project.pojo.enums.ProjectApproveStatus
import com.tencent.devops.project.service.iam.IamRbacService
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProjectCallBackSevice @Autowired constructor(
    private val dslContext: DSLContext,
    private val iamManagerService: V2ManagerService,
    private val projectDao: ProjectDao,
    private val projectApprovalCallbackDao: ProjectApprovalCallbackDao,
    private val iamRbacService: IamRbacService,
    private val objectMapper: ObjectMapper
) {
    @Value("\${esb.code:#{null}}")
    val appCode: String? = null

    @Value("\${esb.secret:#{null}}")
    val appSecret: String? = null

    @Value("\${itsm.verify.token.url:#{null}}")
    private val verifyItsmTokenUrl: String = ""

    fun createProjectCallBack(itsmCallBackInfo: ItsmCallBackInfo) {
        val sn = itsmCallBackInfo.sn
        // 校验token
        // checkItsmToken(itsmCallBackInfo.token, sn)
        val approveResult = itsmCallBackInfo.approveResult.toBoolean()
        // 蓝盾数据库存储的回调信息
        val callBackInfo = projectApprovalCallbackDao.getCallbackBySn(dslContext, sn)
            ?: throw OperationException(
                MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_PROJECT_CALLBACK_APPLICATION_FAIL,
                    defaultMessage = "The itsm callback application does not exist!| sn = $sn"
                )
            )
        val englishName = callBackInfo.englishName
        val projectInfo = projectDao.getByEnglishName(dslContext, englishName) ?: throw OperationException(
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
        if (approveResult) {
            // 调起iam处理分级管理员创建申请
            val gradeManagerId: Int?
            try {
                val callbackApplicationDTO = CallbackApplicationDTO
                    .builder()
                    .sn(sn)
                    .currentStatus(currentStatus)
                    .approveResult(approveResult).build()
                val iamCallbackApplication =
                    iamManagerService.handleCallbackApplication(callBackId, callbackApplicationDTO)
                gradeManagerId = iamCallbackApplication.roleId
                // 创建默认组
                iamRbacService.batchCreateDefaultGroups(
                    userId = callBackInfo.applicant,
                    gradeManagerId = gradeManagerId,
                    projectCode = englishName,
                    projectName = projectInfo.projectName
                )
            } catch (e: Exception) {
                logger.warn("Failed to create a grade manager in the permission center:$englishName|$sn|$callBackId", e)
                throw OperationException("Failed to create a grade manager in the permission center!")
            }
            try {
                dslContext.transaction { configuration ->
                    val context = DSL.using(configuration)
                    // 项目关联分级管理id
                    projectDao.updateRelationByCode(
                        dslContext = context,
                        projectCode = englishName,
                        relationId = gradeManagerId.toString()
                    )
                    // 修改状态
                    projectDao.updateProjectStatusByEnglishName(
                        dslContext = context,
                        englishName = englishName,
                        approvalStatus = ProjectApproveStatus.CREATE_APPROVED.status
                    )
                    projectApprovalCallbackDao.updateCallbackBySn(
                        dslContext = context,
                        sn = sn,
                        lastApprover = itsmCallBackInfo.lastApprover,
                        approveResult = approveResult
                    )
                }
            } catch (e: Exception) {
                logger.warn("Failed to update project:$englishName", e)
                throw OperationException("Failed to update project!")
            }
        } else {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                // 修改状态
                projectDao.updateProjectStatusByEnglishName(
                    dslContext = dslContext,
                    englishName = englishName,
                    approvalStatus = ProjectApproveStatus.CREATE_REJECT.status
                )
                projectApprovalCallbackDao.updateCallbackBySn(
                    dslContext = context,
                    sn = sn,
                    lastApprover = itsmCallBackInfo.lastApprover,
                    approveResult = approveResult
                )
            }
        }
    }

    fun updateProjectCallBack(itsmCallBackInfo: ItsmCallBackInfo) {
        val sn = itsmCallBackInfo.sn
        // checkItsmToken(itsmCallBackInfo.token, sn)
        val approveResult = itsmCallBackInfo.approveResult.toBoolean()
        // 蓝盾数据库存储的回调信息
        val dbCallBackInfo = projectApprovalCallbackDao.getCallbackBySn(dslContext, sn)
            ?: throw OperationException(
                MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_PROJECT_CALLBACK_APPLICATION_FAIL,
                    defaultMessage = "The itsm callback application does not exist!| sn = $sn"
                )
            )
        val englishName = dbCallBackInfo.englishName
        val projectInfo = projectDao.getByEnglishName(dslContext, englishName)
            ?: throw OperationException(
                MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.QUERY_PROJECT_FAIL,
                    defaultMessage = "The project does not exist! | englishName = $englishName"
                )
            )
        val callBackId = dbCallBackInfo.callbackId
        val currentStatus = itsmCallBackInfo.currentStatus
        var authSecrecy = projectInfo.authSecrecy
        logger.info(
            "updateProjectCallBack: ${itsmCallBackInfo.title}" +
                "|$sn|$approveResult|$englishName|$callBackId|$authSecrecy"
        )
        try {
            if (approveResult) {
                // 若最大可授权人员范围被修改，则需要调起Iam处理单据
                if (dbCallBackInfo.approveType == ApproveType.SUBJECT_SCOPES_APPROVE.type
                    || dbCallBackInfo.approveType == ApproveType.ALL_CHANGE_APPROVE.type) {
                    // 调起iam处理分级管理员修改申请
                    val callbackApplicationDTO = CallbackApplicationDTO
                        .builder()
                        .sn(sn)
                        .currentStatus(currentStatus)
                        .approveResult(approveResult).build()
                    iamManagerService.handleCallbackApplication(callBackId, callbackApplicationDTO)
                }
                if (dbCallBackInfo.approveType == ApproveType.AUTH_SECRECY_APPROVE.type
                    || dbCallBackInfo.approveType == ApproveType.ALL_CHANGE_APPROVE.type) {
                    // 修改项目的权限保密字段
                    authSecrecy = !authSecrecy
                }
                projectDao.updateProjectByEnglishName(
                    dslContext = dslContext,
                    subjectScopesStr = dbCallBackInfo.subjectScopes,
                    authSecrecy = authSecrecy,
                    projectCode = englishName,
                    statusEnum = ProjectApproveStatus.UPDATE_APPROVED
                )
            } else {
                // 修改状态
                projectDao.updateProjectStatusByEnglishName(
                    dslContext = dslContext,
                    englishName = englishName,
                    approvalStatus = ProjectApproveStatus.UPDATE_REJECT.status
                )
            }
            projectApprovalCallbackDao.updateCallbackBySn(
                dslContext = dslContext,
                sn = sn,
                lastApprover = itsmCallBackInfo.lastApprover,
                approveResult = approveResult
            )
        } catch (e: Exception) {
            logger.warn("Failed to update project:$englishName|$callBackId|$sn", e)
            throw OperationException("Failed to update project!")
        }
    }

    private fun checkItsmToken(token: String, sn: String) {
        val param: MutableMap<String, String?> = mutableMapOf()
        param["bk_app_secret"] = appSecret
        param["token"] = token
        param["bk_app_code"] = appCode
        val content = objectMapper.writeValueAsString(param)
        val mediaType = MediaType.parse("application/json; charset=utf-8")
        val requestBody = RequestBody.create(mediaType, content)
        val request = Request.Builder().url(verifyItsmTokenUrl)
            .post(requestBody)
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("Itsm request failed url:$verifyItsmTokenUrl response $it")
                // 请求错误
                throw RemoteServiceException("Itsm request failed, response: ($it)")
            }
            val responseStr = it.body()!!.string()
            logger.info("Itsm request responseStr[$responseStr]")
            val itsmApiRes = objectMapper.readValue<Map<String, Any>>(responseStr)
            if (itsmApiRes["code"] != 0 || itsmApiRes["result"] == false) {
                // 请求错误
                throw RemoteServiceException(
                    "request itsm failed|message[${itsmApiRes["message"]}]"
                )
            }
            val itsmApiResData = itsmApiRes["data"] as Map<String, Boolean>
            if (!itsmApiResData["is_passed"]!!) {
                logger.warn("verify itsm token failed!!| sn = $sn | response = $it")
                MessageCodeUtil.getCodeLanMessage(
                    messageCode = ProjectMessageCode.VERIFY_ITSM_TOKEN_FAIL,
                    defaultMessage = "verify itsm token failed!! | sn = $sn"
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCallBackSevice::class.java)
    }
}
