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

package com.tencent.devops.store.common.resources

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.common.UserStoreMemberResource
import com.tencent.devops.store.common.service.StoreMemberService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.member.StoreMemberItem
import com.tencent.devops.store.pojo.common.member.StoreMemberReq

@RestResource
class UserStoreMemberResourceImpl(val storeMemberService: StoreMemberService) : UserStoreMemberResource {

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
        userId: String,
        storeMember: String,
        projectCode: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Boolean> {
        return getStoreMemberService(storeType).changeMemberTestProjectCode(
            userId = userId,
            storeMember = storeMember,
            projectCode = projectCode,
            storeCode = storeCode,
            storeType = storeType
        )
    }

    override fun isStoreMember(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
    ): Result<Boolean> {
        val check = storeMemberService.isStoreMember(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        )
        if (!check) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.GET_INFO_NO_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        return Result(true)
    }

    private fun getStoreMemberService(storeType: StoreTypeEnum): StoreMemberService {
        val beanName = "${storeType.name.lowercase()}MemberService"
        return if (SpringContextUtil.isBeanExist(beanName)) {
            SpringContextUtil.getBean(StoreMemberService::class.java, beanName)
        } else {
            // 获取默认的成员bean对象
            SpringContextUtil.getBean(StoreMemberService::class.java)
        }
    }
}
