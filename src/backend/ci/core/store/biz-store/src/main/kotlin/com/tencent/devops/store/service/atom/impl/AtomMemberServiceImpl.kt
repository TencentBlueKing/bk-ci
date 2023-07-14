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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.impl.StoreMemberServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Suppress("ALL")
abstract class AtomMemberServiceImpl : StoreMemberServiceImpl() {

    @Autowired
    lateinit var marketAtomDao: MarketAtomDao

    private val logger = LoggerFactory.getLogger(AtomMemberServiceImpl::class.java)

    /**
     * 添加插件成员
     */
    override fun add(
        userId: String,
        storeMemberReq: StoreMemberReq,
        storeType: StoreTypeEnum,
        collaborationFlag: Boolean?,
        sendNotify: Boolean,
        checkPermissionFlag: Boolean,
        testProjectCode: String?
    ): Result<Boolean> {
        logger.info("addAtomMember params:$userId|$storeMemberReq|$storeType|$collaborationFlag|$sendNotify")
        val atomCode = storeMemberReq.storeCode
        val atomRecord = marketAtomDao.getLatestAtomByCode(dslContext, atomCode)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(atomCode)
            )
        if (checkPermissionFlag && !storeMemberDao.isStoreAdmin(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = storeType.type.toByte()
            )
        ) {
            return I18nUtil.generateResponseDataObject(
                messageCode = NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(atomCode)
            )
        }
        val repositoryHashId = atomRecord.repositoryHashId
        val addRepoMemberResult = addRepoMember(storeMemberReq, userId, repositoryHashId)
        logger.info("addRepoMemberResult is:$addRepoMemberResult")
        if (addRepoMemberResult.isNotOk()) {
            return Result(status = addRepoMemberResult.status, message = addRepoMemberResult.message, data = false)
        }
        return super.add(
            userId = userId,
            storeMemberReq = storeMemberReq,
            storeType = storeType,
            collaborationFlag = collaborationFlag,
            sendNotify = sendNotify,
            checkPermissionFlag = checkPermissionFlag,
            testProjectCode = testProjectCode
        )
    }

    abstract fun addRepoMember(
        storeMemberReq: StoreMemberReq,
        userId: String,
        repositoryHashId: String
    ): Result<Boolean>

    /**
     * 删除插件成员
     */
    override fun delete(
        userId: String,
        id: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        checkPermissionFlag: Boolean
    ): Result<Boolean> {
        logger.info("deleteAtomMember params:[$userId|$id|$storeCode|$storeType|$checkPermissionFlag]")
        val atomRecord = marketAtomDao.getLatestAtomByCode(dslContext, storeCode)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(storeCode)
            )
        val memberRecord = storeMemberDao.getById(dslContext, id)
            ?: return I18nUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(id)
            )
        // 如果删除的是管理员，只剩一个管理员则不允许删除
        if ((memberRecord.type).toInt() == 0) {
            val validateAdminResult = isStoreHasAdmins(storeCode, storeType)
            if (validateAdminResult.isNotOk()) {
                return Result(status = validateAdminResult.status, message = validateAdminResult.message, data = false)
            }
        }
        val username = memberRecord.username
        val repositoryHashId = atomRecord.repositoryHashId
        val deleteRepoMemberResult = deleteRepoMember(userId, username, repositoryHashId)
        logger.info("deleteRepoMemberResult is:$deleteRepoMemberResult")
        if (deleteRepoMemberResult.isNotOk()) {
            return Result(
                status = deleteRepoMemberResult.status,
                message = deleteRepoMemberResult.message,
                data = false
            )
        }
        return super.delete(
            userId = userId,
            id = id,
            storeCode = storeCode,
            storeType = storeType,
            checkPermissionFlag = checkPermissionFlag
        )
    }

    abstract fun deleteRepoMember(userId: String, username: String, repositoryHashId: String): Result<Boolean>

    override fun getStoreName(storeCode: String): String {
        return marketAtomDao.getLatestAtomByCode(dslContext, storeCode)?.name ?: ""
    }
}
