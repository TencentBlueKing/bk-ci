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
package com.tencent.devops.store.common.dao

import com.tencent.devops.model.store.tables.TStoreDeptRel
import com.tencent.devops.model.store.tables.records.TStoreDeptRelRecord
import com.tencent.devops.store.pojo.common.StoreApproveRequest
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class StoreAuditConfDao {
    /**
     * 获取对应ID的审核记录条数，判断这条审核记录是否存在
     * @param id 审核记录ID
     */
    fun countDeptRel(dslContext: DSLContext, id: String): Int {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            return dslContext.selectCount()
                .from(this)
                .where(ID.eq(id))
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 审核可见范围，根据ID修改对应记录的状态和驳回信息
     * @param userId 审核人ID
     * @param id 审核记录的ID
     * @param storeApproveRequest 审核信息
     */
    fun approveVisibleDept(
        dslContext: DSLContext,
        userId: String,
        id: String,
        storeApproveRequest: StoreApproveRequest
    ): Int {
        val status = when (storeApproveRequest.approveStatus) {
            ApproveStatusEnum.WAIT -> 0
            ApproveStatusEnum.PASS -> 1
            ApproveStatusEnum.REFUSE -> 2
        }
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            return dslContext.update(this)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(STATUS, status.toByte())
                .set(COMMENT, storeApproveRequest.approveMsg)
                .where(ID.eq(id))
                .execute()
        }
    }

    /**
     * 根据给定条件获取相应的组件审核记录、组件名称
     * @param storeCodeList 组件代码
     * @param storeType 组件类型
     * @param status 组件可见范围的审核状态
     */
    fun getDeptRel(
        dslContext: DSLContext,
        storeCodeList: List<String>?,
        storeType: StoreTypeEnum?,
        status: DeptStatusEnum?
    ): Result<TStoreDeptRelRecord>? {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            val condition = getCondition(storeCodeList, storeType, status)
            return dslContext.selectFrom(this)
                .where(condition)
                .orderBy(STATUS.asc(), UPDATE_TIME.desc())
                .fetch()
        }
    }

    /**
     * 删除相应的可见范围审核信息
     */
    fun deleteDeptRel(dslContext: DSLContext, id: String): Int {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    private fun TStoreDeptRel.getCondition(
        storeCodeList: List<String>?,
        storeType: StoreTypeEnum?,
        status: DeptStatusEnum?
    ): MutableList<Condition> {
        val condition = mutableListOf<Condition>()
        if (storeType != null) {
            condition.add(STORE_TYPE.eq(storeType.type.toByte()))
        }
        if (status != null) {
            condition.add(STATUS.eq(status.status.toByte()))
        }
        if (storeCodeList != null) {
            condition.add(STORE_CODE.`in`(storeCodeList))
        }
        return condition
    }
}
