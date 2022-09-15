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

package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TStoreApprove
import com.tencent.devops.model.store.tables.records.TStoreApproveRecord
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.ApproveTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class StoreApproveDao {

    fun getStoreApproveInfoCount(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        applicant: String?,
        approveType: ApproveTypeEnum?,
        approveStatus: ApproveStatusEnum?
    ): Long {
        with(TStoreApprove.T_STORE_APPROVE) {
            val conditions = getStoreApproveInfoConditions(storeCode, storeType, applicant, approveType, approveStatus)
            return dslContext.selectCount().from(this).where(conditions).fetchOne(0, Long::class.java)!!
        }
    }

    fun getStoreApproveInfos(
        dslContext: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        applicant: String?,
        approveType: ApproveTypeEnum?,
        approveStatus: ApproveStatusEnum?,
        page: Int,
        pageSize: Int
    ): Result<TStoreApproveRecord>? {
        with(TStoreApprove.T_STORE_APPROVE) {
            val conditions = getStoreApproveInfoConditions(storeCode, storeType, applicant, approveType, approveStatus)
            return dslContext.selectFrom(this)
                .where(conditions)
                .orderBy(CREATE_TIME.desc())
                .limit((page - 1) * pageSize, pageSize)
                .fetch()
        }
    }

    private fun TStoreApprove.getStoreApproveInfoConditions(
        storeCode: String,
        storeType: StoreTypeEnum,
        applicant: String?,
        approveType: ApproveTypeEnum?,
        approveStatus: ApproveStatusEnum?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        conditions.add(STORE_CODE.eq(storeCode))
        conditions.add(STORE_TYPE.eq(storeType.type.toByte()))
        if (!applicant.isNullOrEmpty()) {
            conditions.add(APPLICANT.like("%$applicant%"))
        }
        if (null != approveType) {
            conditions.add(TYPE.eq(approveType.name))
        }
        if (null != approveStatus) {
            conditions.add(STATUS.eq(approveStatus.name))
        }
        return conditions
    }

    fun getStoreApproveInfo(dslContext: DSLContext, approveId: String): TStoreApproveRecord? {
        return with(TStoreApprove.T_STORE_APPROVE) {
            dslContext.selectFrom(this)
                .where(ID.eq(approveId))
                .fetchOne()
        }
    }

    fun addStoreApproveInfo(
        dslContext: DSLContext,
        userId: String,
        approveId: String,
        content: String,
        applicant: String,
        approveType: ApproveTypeEnum,
        approveStatus: ApproveStatusEnum,
        storeCode: String,
        storeType: StoreTypeEnum,
        token: String
    ) {
        with(TStoreApprove.T_STORE_APPROVE) {
            dslContext.insertInto(
                this,
                ID,
                CONTENT,
                APPLICANT,
                TYPE,
                STATUS,
                STORE_CODE,
                STORE_TYPE,
                CREATOR,
                MODIFIER,
                TOKEN
            ).values(
                approveId,
                content,
                applicant,
                approveType.name,
                approveStatus.name,
                storeCode,
                storeType.type.toByte(),
                userId,
                userId,
                token
            ).execute()
        }
    }

    fun updateStoreApproveInfo(
        dslContext: DSLContext,
        approveId: String,
        userId: String,
        approveStatus: ApproveStatusEnum,
        approveMsg: String
    ) {
        with(TStoreApprove.T_STORE_APPROVE) {
            dslContext.update(this)
                .set(STATUS, approveStatus.name)
                .set(APPROVER, userId)
                .set(APPROVE_MSG, approveMsg)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(ID.eq(approveId))
                .execute()
        }
    }

    /**
     * 判断用户是否能提交store某项业务的申请单
     */
    fun isAllowApply(
        dslContext: DSLContext,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        approveType: ApproveTypeEnum
    ): Boolean {
        with(TStoreApprove.T_STORE_APPROVE) {
            return dslContext.selectCount()
                .from(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(APPLICANT.eq(userId))
                .and(TYPE.eq(approveType.name))
                .and(STATUS.eq(ApproveStatusEnum.WAIT.name))
                .fetchOne(0, Long::class.java) == 0L
        }
    }

    fun getUserStoreApproveInfo(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        approveType: ApproveTypeEnum
    ): TStoreApproveRecord? {
        with(TStoreApprove.T_STORE_APPROVE) {
            return dslContext.selectFrom(this)
                .where(STORE_CODE.eq(storeCode))
                .and(STORE_TYPE.eq(storeType.type.toByte()))
                .and(APPLICANT.eq(userId))
                .and(TYPE.eq(approveType.name))
                .orderBy(CREATE_TIME.desc())
                .limit(1)
                .fetchOne()
        }
    }

    fun deleteApproveInfo(dslContext: DSLContext, storeCode: String, storeType: Byte) {
        with(TStoreApprove.T_STORE_APPROVE) {
            dslContext.deleteFrom(this)
                .where(STORE_CODE.eq(storeCode).and(STORE_TYPE.eq(storeType)))
                .execute()
        }
    }
}
