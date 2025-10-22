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
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface StoreManagementExtraService {

    /**
     * 检查组件是否可以删除
     */
    fun doComponentDeleteCheck(storeCode: String): Result<Boolean>

    /**
     * 删除组件仓库文件
     */
    fun deleteComponentRepoFile(userId: String, storeCode: String, storeType: StoreTypeEnum): Result<Boolean>

    /**
     * 删除组件代码库
     */
    fun deleteComponentCodeRepository(
        userId: String,
        repositoryId: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    /**
     * 添加组件代码库成员
     */
    fun addComponentRepositoryUser(
        memberType: StoreMemberTypeEnum,
        members: List<String>,
        repositoryId: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    /**
     * 删除组件代码库成员
     */
    fun deleteComponentRepositoryUser(
        member: String,
        repositoryId: String,
        token: String,
        tokenType: TokenTypeEnum
    ): Result<Boolean>

    /**
     * 检查卸载组件请求合法性
     */
    fun uninstallComponentCheck(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String
    ): Result<Boolean>
}
