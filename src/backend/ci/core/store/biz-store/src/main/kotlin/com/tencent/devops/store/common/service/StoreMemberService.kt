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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.member.StoreMemberItem
import com.tencent.devops.store.pojo.common.member.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

@Suppress("ALL")
interface StoreMemberService {

    /**
     * store组件成员列表
     */
    fun list(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        checkPermissionFlag: Boolean = true
    ): Result<List<StoreMemberItem?>>

    /**
     * 查看store组件成员信息
     */
    fun viewMemberInfo(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<StoreMemberItem?>

    /***
     * 批量获取store组件成员列表
     */
    fun batchListMember(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<String>>>

    /**
     * 添加store组件成员
     */
    fun add(
        userId: String,
        storeMemberReq: StoreMemberReq,
        storeType: StoreTypeEnum,
        collaborationFlag: Boolean? = false,
        sendNotify: Boolean = true,
        checkPermissionFlag: Boolean = true,
        testProjectCode: String? = null
    ): Result<Boolean>

    /**
     * 删除store组件成员
     */
    fun delete(
        userId: String,
        id: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        checkPermissionFlag: Boolean = true
    ): Result<Boolean>

    /**
     * 更改store组件成员的调试项目
     */
    fun changeMemberTestProjectCode(
        userId: String,
        storeMember: String,
        projectCode: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Result<Boolean>

    /**
     * 判断store组件是否有超过一个管理员
     */
    fun isStoreHasAdmins(storeCode: String, storeType: StoreTypeEnum): Result<Boolean>

    /**
     * 判断是否为成员
     */
    fun isStoreMember(userId: String, storeCode: String, storeType: Byte): Boolean

    /**
     * 判断是否为管理员
     */
    fun isStoreAdmin(userId: String, storeCode: String, storeType: Byte): Boolean
}
