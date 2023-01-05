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

package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.UserStoreDeptInfoRequest
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

/**
 * store组件可见范围逻辑类
 * since: 2019-01-08
 */
@Suppress("ALL")
interface StoreVisibleDeptService {

    /**
     * 查看store组件可见范围
     */
    fun getVisibleDept(
        storeCode: String,
        storeType: StoreTypeEnum,
        deptStatus: DeptStatusEnum?
    ): Result<StoreVisibleDeptResp?>

    /**
     * 批量获取已经审核通过的可见范围
     */
    fun batchGetVisibleDept(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<Int>>>

    /**
     * 设置store组件可见范围
     */
    fun addVisibleDept(
        userId: String,
        storeCode: String,
        deptInfos: List<DeptInfo>,
        storeType: StoreTypeEnum
    ): Result<Boolean>

    /**
     * 删除store组件可见范围
     */
    fun deleteVisibleDept(
        userId: String,
        storeCode: String,
        deptIds: String,
        storeType: StoreTypeEnum
    ): Result<Boolean>

    /**
     * 审核可见范围
     */
    fun approveVisibleDept(
        userId: String,
        storeCode: String,
        visibleApproveReq: VisibleApproveReq,
        storeType: StoreTypeEnum
    ): Result<Boolean>

    /**
     * 判断用户是否有组件的权限
     */
    fun checkUserInvalidVisibleStoreInfo(
        userStoreDeptInfoRequest: UserStoreDeptInfoRequest
    ): Boolean
}
