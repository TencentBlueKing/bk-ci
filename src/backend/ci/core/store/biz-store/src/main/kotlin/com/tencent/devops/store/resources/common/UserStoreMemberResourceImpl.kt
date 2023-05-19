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

package com.tencent.devops.store.resources.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.UserStoreMemberResource
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMemberService

@RestResource
class UserStoreMemberResourceImpl : UserStoreMemberResource {

    override fun list(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<List<StoreMemberItem?>> {
        return getStoreMemberService(storeType).list(userId, storeCode, storeType)
    }

    override fun add(userId: String, storeMemberReq: StoreMemberReq): Result<Boolean> {
        val storeType = storeMemberReq.storeType
        return getStoreMemberService(storeType).add(userId, storeMemberReq, storeType)
    }

    override fun delete(userId: String, id: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        return getStoreMemberService(storeType).delete(userId, id, storeCode, storeType)
    }

    override fun view(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<StoreMemberItem?> {
        return getStoreMemberService(storeType).viewMemberInfo(userId, storeCode, storeType)
    }

    override fun changeMemberTestProjectCode(
        accessToken: String,
        userId: String,
        storeMember: String,
        projectCode: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        return getStoreMemberService(storeType).changeMemberTestProjectCode(
            accessToken = accessToken,
            userId = userId,
            storeMember = storeMember,
            projectCode = projectCode,
            storeCode = storeCode,
            storeType = storeType
        )
    }

    private fun getStoreMemberService(storeType: StoreTypeEnum): StoreMemberService {
        return SpringContextUtil.getBean(StoreMemberService::class.java,
            "${storeType.name.lowercase()}MemberService")
    }
}
