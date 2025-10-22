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

package com.tencent.devops.store.common.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreDeptRel
import com.tencent.devops.model.store.tables.records.TStoreDeptRelRecord
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.visible.DeptInfo
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class StoreDeptRelDao {

    fun getDeptInfosByStoreCode(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        deptStatusList: List<Byte>? = null,
        deptIdList: List<Int>? = null
    ): Result<TStoreDeptRelRecord>? {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            val conditions = mutableListOf<Condition>()
            conditions.add(STORE_CODE.eq(storeCode))
            conditions.add(STORE_TYPE.eq(storeType))
            if (!deptStatusList.isNullOrEmpty()) conditions.add(STATUS.`in`(deptStatusList))
            if (!deptIdList.isNullOrEmpty()) conditions.add(DEPT_ID.`in`(deptIdList))
            return dslContext.selectFrom(this)
                .where(conditions)
                .fetch()
        }
    }

    fun batchList(
        dslContext: DSLContext,
        storeCodeList: Collection<String?>,
        storeType: Byte
    ): Result<TStoreDeptRelRecord>? {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.`in`(storeCodeList))
                .and(STATUS.eq(DeptStatusEnum.APPROVED.status.toByte()))
                .and(STORE_TYPE.eq(storeType))
                .fetch()
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        deptInfoList: List<DeptInfo>,
        storeType: Byte
    ) {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            val addStep = deptInfoList.map {
                val deptStatus = if (it.status.isNullOrBlank()) {
                    DeptStatusEnum.APPROVED.status.toByte()
                } else {
                    DeptStatusEnum.valueOf(it.status!!).status.toByte()
                }
                dslContext.insertInto(this,
                    ID,
                    STORE_CODE,
                    DEPT_ID,
                    DEPT_NAME,
                    STATUS,
                    COMMENT,
                    STORE_TYPE,
                    CREATOR,
                    MODIFIER
                )
                    .values(
                        UUIDUtil.generate(),
                        storeCode,
                        it.deptId,
                        it.deptName,
                        deptStatus,
                        it.comment,
                        storeType,
                        userId,
                        userId
                    )
                    .onDuplicateKeyUpdate()
                    .set(STATUS, deptStatus)
                    .set(COMMENT, it.comment)
                    .set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
            }
            dslContext.batch(addStep).execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun batchDelete(dslContext: DSLContext, storeCode: String, deptIdList: List<Int>, storeType: Byte) {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode).and(DEPT_ID.`in`(deptIdList)).and(STORE_TYPE.eq(storeType)))
                .execute()
        }
    }

    fun deleteByStoreCode(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .execute()
        }
    }

    fun countByCodeAndDeptId(dslContext: DSLContext, storeCode: String, deptId: Int, storeType: Byte): Int {
        return with(TStoreDeptRel.T_STORE_DEPT_REL) {
            dslContext.selectCount().from(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType))
                    .and(DEPT_ID.eq(deptId))
                    .and(
                        STATUS.`in`(DeptStatusEnum.APPROVING.status.toByte(), DeptStatusEnum.APPROVED.status.toByte())
                    )
                )
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun updateDeptStatus(
        dslContext: DSLContext,
        storeCode: String,
        storeType: Byte,
        originStatus: Byte,
        newStatus: Byte,
        userId: String
    ) {
        with(TStoreDeptRel.T_STORE_DEPT_REL) {
            dslContext.update(this)
                .set(STATUS, newStatus)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType))
                .and(STATUS.eq(originStatus))
                .execute()
        }
    }
}
