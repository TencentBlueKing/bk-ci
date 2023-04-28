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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.atom.AtomApproveRelDao
import com.tencent.devops.store.dao.common.StoreApproveDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.atom.AtomCollaboratorCreateReq
import com.tencent.devops.store.pojo.atom.AtomCollaboratorCreateResp
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.ApproveTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomCooperationService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * 插件协作逻辑处理
 * since: 2019-08-05
 */
abstract class AtomCooperationServiceImpl @Autowired constructor() : AtomCooperationService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var atomApproveRelDao: AtomApproveRelDao
    @Autowired
    lateinit var storeApproveDao: StoreApproveDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(AtomCooperationServiceImpl::class.java)

    override fun addAtomCollaborator(
        userId: String,
        atomCollaboratorCreateReq: AtomCollaboratorCreateReq
    ): Result<AtomCollaboratorCreateResp> {
        logger.info("addAtomCollaborator userId is :$userId,atomCollaboratorCreateReq is :$atomCollaboratorCreateReq")
        // 判断用户提交的插件协作者申请是否处于待审批的状态，防止重复提交申请
        val atomCode = atomCollaboratorCreateReq.atomCode
        val storeMemberFlag = storeMemberDao.isStoreMember(
            dslContext = dslContext,
            userId = userId,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM.type.toByte()
        )
        val flag = storeApproveDao.isAllowApply(
            dslContext = dslContext,
            userId = userId,
            storeCode = atomCode,
            storeType = StoreTypeEnum.ATOM,
            approveType = ApproveTypeEnum.ATOM_COLLABORATOR_APPLY
        )
        if (storeMemberFlag || !flag) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_APPROVAL_IS_NOT_ALLOW_REPEAT_APPLY,
                language = I18nUtil.getLanguage(userId)
            )
        }
        val approveId = UUIDUtil.generate()
        val token = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            atomApproveRelDao.add(
                dslContext = context,
                userId = userId,
                atomCode = atomCode,
                testProjectCode = atomCollaboratorCreateReq.testProjectCode,
                approveId = approveId
            )
            storeApproveDao.addStoreApproveInfo(
                dslContext = context,
                userId = userId,
                approveId = approveId,
                content = atomCollaboratorCreateReq.applyReason,
                applicant = userId,
                approveType = ApproveTypeEnum.ATOM_COLLABORATOR_APPLY,
                approveStatus = ApproveStatusEnum.WAIT,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM,
                token = token
            )
        }
        sendMoaMessage(
            atomCode = atomCode,
            atomCollaboratorCreateReq = atomCollaboratorCreateReq,
            approveId = approveId,
            userId = userId,
            token = token
        )
        return Result(AtomCollaboratorCreateResp(userId, ApproveStatusEnum.WAIT.name))
    }

    abstract fun sendMoaMessage(
        atomCode: String,
        atomCollaboratorCreateReq: AtomCollaboratorCreateReq,
        approveId: String,
        userId: String,
        token: String
    )
}
