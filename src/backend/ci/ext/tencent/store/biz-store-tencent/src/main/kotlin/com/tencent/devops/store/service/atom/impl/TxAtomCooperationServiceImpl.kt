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

import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.pojo.atom.AtomCollaboratorCreateReq
import com.tencent.devops.store.pojo.common.ATOM_COLLABORATOR_APPLY_MOA_TEMPLATE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.support.api.service.ServiceMessageApproveResource
import com.tencent.devops.support.model.approval.CreateMoaApproveRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.util.concurrent.Executors

@Service
class TxAtomCooperationServiceImpl @Autowired constructor() : AtomCooperationServiceImpl() {

    @Value("\${store.moaApproveCallBackUrl}")
    private lateinit var moaApproveCallBackUrl: String

    @Autowired
    private lateinit var marketAtomDao: MarketAtomDao

    private val executorService = Executors.newFixedThreadPool(2)

    private val logger = LoggerFactory.getLogger(TxAtomCooperationServiceImpl::class.java)

    override fun sendMoaMessage(
        atomCode: String,
        atomCollaboratorCreateReq: AtomCollaboratorCreateReq,
        approveId: String,
        userId: String,
        token: String
    ) {
        logger.info("sendMoaMessage params:[$atomCode|$atomCollaboratorCreateReq|$approveId|$userId")
        executorService.submit<Unit> {
            val adminRecords = storeMemberDao.getAdmins(dslContext, atomCode, StoreTypeEnum.ATOM.type.toByte())
            val verifierSb = StringBuilder()
            adminRecords.forEach {
                verifierSb.append(it.username).append(",")
            }
            val atomRecord = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
            val applyReason = atomCollaboratorCreateReq.applyReason
            val createMoaApproveRequest = CreateMoaApproveRequest(
                verifier = verifierSb.toString(),
                title = MessageCodeUtil.getCodeMessage(
                    messageCode = ATOM_COLLABORATOR_APPLY_MOA_TEMPLATE,
                    params = arrayOf(userId, atomRecord!!.name, applyReason)
                ) ?: applyReason,
                taskId = approveId,
                backUrl = MessageFormat.format(moaApproveCallBackUrl, token)
            )
            val createMoaMessageApprovalResult = client.get(ServiceMessageApproveResource::class)
                .createMoaMessageApproval(userId = userId, createMoaApproveRequest = createMoaApproveRequest)
            logger.info("createMoaMessageApprovalResult is :$createMoaMessageApprovalResult")
        }
    }
}
