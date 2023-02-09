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

package com.tencent.devops.store.service.atom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.AtomOfflineReq
import com.tencent.devops.store.pojo.atom.AtomReleaseRequest
import com.tencent.devops.store.pojo.atom.MarketAtomCreateRequest
import com.tencent.devops.store.pojo.atom.MarketAtomUpdateRequest
import com.tencent.devops.store.pojo.common.StoreProcessInfo

@Suppress("ALL")
interface AtomReleaseService {

    /**
     * 添加插件
     */
    fun addMarketAtom(userId: String, marketAtomCreateRequest: MarketAtomCreateRequest): Result<String>

    /**
     * 升级插件
     */
    fun updateMarketAtom(
        userId: String,
        projectCode: String,
        marketAtomUpdateRequest: MarketAtomUpdateRequest
    ): Result<String?>

    /**
     * 获取插件版本发布进度
     */
    fun getProcessInfo(userId: String, atomId: String): Result<StoreProcessInfo>

    /**
     * 取消发布
     */
    fun cancelRelease(userId: String, atomId: String): Result<Boolean>

    /**
     * 确认通过测试，继续发布
     */
    fun passTest(userId: String, atomId: String): Result<Boolean>

    /**
     * 处理用户提交的下架插件请求
     */
    fun offlineAtom(
        userId: String,
        atomCode: String,
        atomOfflineReq: AtomOfflineReq,
        checkPermissionFlag: Boolean = true
    ): Result<Boolean>

    /**
     * 处理插件发布逻辑
     */
    fun handleAtomRelease(
        userId: String,
        releaseFlag: Boolean,
        atomReleaseRequest: AtomReleaseRequest
    ): Result<Boolean>

    /**
     * error.json数据入库
     */
    fun syncAtomErrorCodeConfig(
        atomCode: String,
        atomVersion: String,
        userId: String,
        repositoryHashId: String?,
        branch: String?
    )
}
