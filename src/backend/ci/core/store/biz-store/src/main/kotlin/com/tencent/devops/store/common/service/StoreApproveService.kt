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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.approval.StoreApproveDetail
import com.tencent.devops.store.pojo.common.approval.StoreApproveInfo
import com.tencent.devops.store.pojo.common.approval.StoreApproveRequest
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.ApproveTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

/**
 * store审批业务逻辑类
 * since: 2019-08-05
 */
@Suppress("ALL")
interface StoreApproveService {

    /**
     * 审批store组件
     */
    fun approveStoreInfo(
        userId: String,
        approveId: String,
        storeApproveRequest: StoreApproveRequest
    ): Result<Boolean>

    fun getStoreApproveInfos(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        applicant: String?,
        approveType: ApproveTypeEnum?,
        approveStatus: ApproveStatusEnum?,
        page: Int,
        pageSize: Int
    ): Result<Page<StoreApproveInfo>?>

    fun getUserStoreApproveInfo(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveType: ApproveTypeEnum
    ): Result<StoreApproveInfo?>

    fun getStoreApproveDetail(userId: String, approveId: String): Result<StoreApproveDetail?>
}
