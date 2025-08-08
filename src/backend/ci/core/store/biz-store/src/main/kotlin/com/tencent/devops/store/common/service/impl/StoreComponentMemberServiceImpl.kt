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
 */

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.constant.RepositoryConstants.KEY_REPOSITORY_ID
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.StoreManagementExtraService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.member.StoreMemberReq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import jakarta.ws.rs.NotFoundException

@Primary
@Service
class StoreComponentMemberServiceImpl @Autowired constructor(
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao
) : StoreMemberServiceImpl() {

    override fun getStoreName(storeCode: String, storeType: StoreTypeEnum): String {
        return storeBaseQueryDao.getLatestComponentByCode(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType
        )?.name ?: ""
    }

    override fun add(
        userId: String,
        storeMemberReq: StoreMemberReq,
        storeType: StoreTypeEnum,
        collaborationFlag: Boolean?,
        sendNotify: Boolean,
        checkPermissionFlag: Boolean,
        testProjectCode: String?
    ): Result<Boolean> {
        val storeCode = storeMemberReq.storeCode
        // 检查用户是否有权限操作
        super.checkUserPermission(
            checkPermissionFlag = checkPermissionFlag,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
        val repositoryId = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
            dslContext = dslContext, storeCode = storeCode, storeType = storeType, fieldName = KEY_REPOSITORY_ID
        )?.fieldValue
        if (!repositoryId.isNullOrBlank()) {
            val gitToken = client.get(ServiceOauthResource::class).gitGet(userId).data
                ?: throw NotFoundException("cannot found access token for user($userId)")
            getStoreManagementExtraService(storeType).addComponentRepositoryUser(
                memberType = storeMemberReq.type,
                members = storeMemberReq.member,
                repositoryId = repositoryId,
                token = gitToken.accessToken,
                tokenType = TokenTypeEnum.OAUTH
            )
        }
        return super.add(
            userId = userId,
            storeMemberReq = storeMemberReq,
            storeType = storeType,
            collaborationFlag = collaborationFlag,
            sendNotify = sendNotify,
            checkPermissionFlag = false,
            testProjectCode = testProjectCode
        )
    }

    override fun delete(
        userId: String,
        id: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        checkPermissionFlag: Boolean
    ): Result<Boolean> {
        // 检查用户是否有权限操作
        super.checkUserPermission(
            checkPermissionFlag = checkPermissionFlag,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
        val memberRecord = storeMemberDao.getById(dslContext, id) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(id)
        )
        // 如果删除的是管理员，只剩一个管理员则不允许删除
        if ((memberRecord.type).toInt() == 0) {
            val validateAdminResult = isStoreHasAdmins(storeCode, storeType)
            if (validateAdminResult.isNotOk()) {
                return Result(status = validateAdminResult.status, message = validateAdminResult.message, data = false)
            }
        }
        val repositoryId = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
            dslContext = dslContext, storeCode = storeCode, storeType = storeType, fieldName = KEY_REPOSITORY_ID
        )?.fieldValue
        if (!repositoryId.isNullOrBlank()) {
            val gitToken = client.get(ServiceOauthResource::class).gitGet(userId).data
                ?: throw NotFoundException("cannot found access token for user($userId)")
            getStoreManagementExtraService(storeType).deleteComponentRepositoryUser(
                member = memberRecord.username,
                repositoryId = repositoryId,
                token = gitToken.accessToken,
                tokenType = TokenTypeEnum.OAUTH
            )
        }
        return super.delete(
            userId = userId,
            id = id,
            storeCode = storeCode,
            storeType = storeType,
            checkPermissionFlag = false
        )
    }

    private fun getStoreManagementExtraService(storeType: StoreTypeEnum): StoreManagementExtraService {
        return SpringContextUtil.getBean(
            StoreManagementExtraService::class.java,
            "${storeType}_MANAGEMENT_EXTRA_SERVICE"
        )
    }
}
