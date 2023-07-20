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
package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.store.constant.StoreMessageCode.GET_INFO_NO_PERMISSION
import com.tencent.devops.store.dao.common.StoreDockingPlatformDao
import com.tencent.devops.store.dao.common.StoreDockingPlatformRelDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.StoreDockingPlatformInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreDockingPlatformRelService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreDockingPlatformRelServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storeDockingPlatformDao: StoreDockingPlatformDao,
    private val storeDockingPlatformRelDao: StoreDockingPlatformRelDao
) : StoreDockingPlatformRelService {

    override fun create(
        userId: String?,
        storeCode: String,
        storeType: StoreTypeEnum,
        platformCodes: Set<String>
    ): Boolean {
        // 判断用户是否有权限
        validateUserPermission(userId, storeCode, storeType)
        storeDockingPlatformRelDao.batchAdd(
            dslContext = dslContext,
            userId = userId ?: SYSTEM,
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            platformCodes = platformCodes
        )
        return true
    }

    override fun getStoreDockingPlatforms(
        userId: String?,
        storeCode: String,
        storeType: StoreTypeEnum
    ): List<StoreDockingPlatformInfo>? {
        // 判断用户是否有权限
        validateUserPermission(userId, storeCode, storeType)
        // 查出组件下的平台标识列表
        val platformCodes = storeDockingPlatformRelDao.getStoreDockingPlatformRelations(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        )?.map { it.platformCode }
        if (platformCodes.isNullOrEmpty()) {
            return emptyList()
        }
        return storeDockingPlatformDao.getStoreDockingPlatforms(dslContext, platformCodes)
    }

    private fun validateUserPermission(userId: String?, storeCode: String, storeType: StoreTypeEnum) {
        if (userId.isNullOrBlank()) {
            return
        }
        if (!storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte())
        ) {
            throw ErrorCodeException(
                errorCode = GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
    }
}
