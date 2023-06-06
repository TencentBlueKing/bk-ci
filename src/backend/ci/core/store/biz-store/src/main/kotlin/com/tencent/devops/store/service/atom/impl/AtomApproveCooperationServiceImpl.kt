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
package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.dao.atom.AtomApproveRelDao
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.common.StoreApproveDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.pojo.common.ATOM_COLLABORATOR_APPLY_REFUSE_TEMPLATE
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.AbstractStoreApproveSpecifyBusInfoService
import com.tencent.devops.store.service.common.StoreNotifyService
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 插件协作逻辑处理
 * since: 2019-08-05
 */
@Suppress("ALL")
@Service("ATOM_COLLABORATOR_APPLY_APPROVE_SERVICE")
class AtomApproveCooperationServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketAtomDao: MarketAtomDao,
    private val atomApproveRelDao: AtomApproveRelDao,
    private val storeApproveDao: StoreApproveDao,
    private val storeProjectRelDao: StoreProjectRelDao,
    private val atomMemberService: AtomMemberServiceImpl,
    private val storeNotifyService: StoreNotifyService
) : AbstractStoreApproveSpecifyBusInfoService() {

    private val executorService = Executors.newFixedThreadPool(2)

    private val logger = LoggerFactory.getLogger(AtomApproveCooperationServiceImpl::class.java)

    override fun approveStoreSpecifyBusInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveId: String,
        storeApproveRequest: StoreApproveRequest
    ): Result<Boolean> {
        logger.info("approveStoreSpecifyBusInfo params: [$userId|$storeType|$storeCode|$storeApproveRequest]")
        val atomApproveRelRecord = atomApproveRelDao.getByApproveId(dslContext, approveId)
        if (null == atomApproveRelRecord) {
            return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(approveId),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val atomApproveRecord = storeApproveDao.getStoreApproveInfo(dslContext, approveId)
        if (storeApproveRequest.approveStatus == ApproveStatusEnum.PASS) {
            // 为用户添加插件代码库的权限和插件开发人员的权限
            val storeMemberReq = StoreMemberReq(
                member = listOf(atomApproveRecord!!.applicant),
                type = StoreMemberTypeEnum.DEVELOPER,
                storeCode = storeCode,
                storeType = storeType
            )
            val addAtomMemberResult = atomMemberService.add(userId, storeMemberReq, storeType, true)
            if (addAtomMemberResult.isNotOk()) {
                return Result(status = addAtomMemberResult.status, message = addAtomMemberResult.message, data = false)
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeApproveDao.updateStoreApproveInfo(
                dslContext = context,
                userId = userId,
                approveId = approveId,
                approveMsg = storeApproveRequest.approveMsg,
                approveStatus = storeApproveRequest.approveStatus
            )
            // 如果审批通过，需要为用户添加插件代码库的开发权限和保存调试项目
            if (storeApproveRequest.approveStatus == ApproveStatusEnum.PASS) {
                // 如果申请者已经是插件的成员则直接更新该成员的调试项目为协作申请时录入的项目
                storeProjectRelDao.updateUserStoreTestProject(
                        dslContext = context,
                        userId = atomApproveRecord!!.applicant,
                        storeCode = storeCode,
                        storeType = storeType,
                        projectCode = atomApproveRelRecord.testProjectCode,
                        storeProjectType = StoreProjectTypeEnum.TEST
                )
            }
        }
        if (storeApproveRequest.approveStatus == ApproveStatusEnum.REFUSE) {
            // 给用户发送驳回通知
            executorService.submit<Unit> {
                val receivers = mutableSetOf(atomApproveRecord!!.applicant)
                val atomName = marketAtomDao.getLatestAtomByCode(dslContext, storeCode)?.name ?: ""
                val bodyParams = mapOf(
                    "atomAdmin" to userId,
                    "atomName" to atomName,
                    "approveMsg" to storeApproveRequest.approveMsg
                )
                storeNotifyService.sendNotifyMessage(
                    templateCode = ATOM_COLLABORATOR_APPLY_REFUSE_TEMPLATE,
                    sender = DEVOPS,
                    receivers = receivers,
                    bodyParams = bodyParams
                )
            }
        }
        return Result(true)
    }

    override fun getBusAdditionalParams(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveId: String
    ): Map<String, String>? {
        val testProjectCode = storeProjectRelDao.getUserStoreTestProjectCode(dslContext, userId, storeCode, storeType)
        return if (null != testProjectCode) {
            val additionalParams = mapOf("testProjectCode" to testProjectCode)
            logger.info("getBusAdditionalParams storeCode=$storeCode|additionalParams=$additionalParams")
            additionalParams
        } else {
            null
        }
    }
}
