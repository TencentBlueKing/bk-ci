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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.common.StoreDeptRelDao
import com.tencent.devops.store.pojo.common.DeptInfo
import com.tencent.devops.store.pojo.common.PASS
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.template.TemplateVisibleDeptService
import com.tencent.devops.store.service.template.TxOpTemplateService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxOpTemplateServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateVisibleDeptService: TemplateVisibleDeptService,
    private val storeDeptRelDao: StoreDeptRelDao
) : TxOpTemplateService {

    private val logger = LoggerFactory.getLogger(TxOpTemplateServiceImpl::class.java)

    /**
     * 审核可见范围
     */
    override fun approveVisibleDept(
        userId: String,
        storeCode: String,
        visibleApproveReq:
        VisibleApproveReq
    ): Result<Boolean> {
        val deptIdIntList = visibleApproveReq.deptIdList
        val storeType = StoreTypeEnum.TEMPLATE.type.toByte()
        logger.info("approveVisibleDept params:[$userId|$storeCode|$deptIdIntList|$storeType]")
        val status =
            if (visibleApproveReq.result == PASS) {
                DeptStatusEnum.APPROVED.status.toByte()
            } else {
                DeptStatusEnum.REJECT.status.toByte()
            }

        if (status == DeptStatusEnum.APPROVED.status.toByte()) {
            val deptInfos = mutableListOf<DeptInfo>()
            val records = storeDeptRelDao.getDeptInfosByStoreCode(
                dslContext = dslContext,
                storeCode = storeCode,
                storeType = storeType,
                deptStatus = null,
                deptIdList = deptIdIntList
            )
            records?.forEach {
                deptInfos.add(DeptInfo(it.deptId, it.deptName, DeptStatusEnum.getStatus(it.status.toInt()), it.comment))
            }
            if (records != null) {
                val checkRet = templateVisibleDeptService.validateTemplateVisibleDept(storeCode, deptInfos)
                if (checkRet.isNotOk()) {
                    return checkRet
                }
            }
        }
        storeDeptRelDao.batchUpdate(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            deptIdList = deptIdIntList,
            status = status,
            comment = visibleApproveReq.message,
            storeType = storeType
        )
        return Result(true)
    }
}
