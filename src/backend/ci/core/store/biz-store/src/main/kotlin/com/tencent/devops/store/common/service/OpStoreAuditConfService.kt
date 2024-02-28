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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.VisibleAuditInfo
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

interface OpStoreAuditConfService {

    /**
     * 查询给定条件的审核范围记录
     * @param storeName 审核组件名称
     * @param storeType 审核组件类型（0：插件 1：模板）
     * @param status 审核状态
     * @param page 分页页数
     * @param pageSize 分页每页记录条数
     */
    fun getAllAuditConf(
        storeName: String?,
        storeType: StoreTypeEnum?,
        status: DeptStatusEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<VisibleAuditInfo>>

    /**
     * 审核可见范围，根据记录ID修改相应的审核状态
     * @param userId 审核人ID
     * @param id 审核记录ID
     * @param storeApproveRequest 审核信息对象，半酣审核状态和驳回原因
     */
    fun approveVisibleDept(userId: String, id: String, storeApproveRequest: StoreApproveRequest): Result<Boolean>

    /**
     * 根据审核记录的ID删除一条审核记录
     * @param id 审核记录的ID
     */
    fun deleteAuditConf(id: String): Result<Boolean>
}
