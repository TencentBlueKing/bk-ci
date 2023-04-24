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
import com.tencent.devops.store.api.common.OpStoreMemberResource
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMemberService

@RestResource
class OpStoreMemberResourceImpl : OpStoreMemberResource {

    override fun list(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<List<StoreMemberItem?>> {
        return getStoreMemberService(storeType).list(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType,
            checkPermissionFlag = false
        )
    }

    override fun add(userId: String, testProjectCode: String?, storeMemberReq: StoreMemberReq): Result<Boolean> {
        val storeType = storeMemberReq.storeType
        return getStoreMemberService(storeType).add(
            userId = userId,
            storeMemberReq = storeMemberReq,
            storeType = storeType,
            sendNotify = false,
            checkPermissionFlag = false,
            testProjectCode = testProjectCode
        )
    }

    override fun delete(userId: String, id: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean> {
        return getStoreMemberService(storeType).delete(
            userId = userId,
            id = id,
            storeCode = storeCode,
            storeType = storeType,
            checkPermissionFlag = false
        )
    }

    override fun view(
        userId: String,
        member: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<StoreMemberItem?> {
        return getStoreMemberService(storeType).viewMemberInfo(member, storeCode, storeType)
    }

    private fun getStoreMemberService(storeType: StoreTypeEnum): StoreMemberService {
        return SpringContextUtil.getBean(StoreMemberService::class.java, "${storeType.name.lowercase()}MemberService")
    }
}
