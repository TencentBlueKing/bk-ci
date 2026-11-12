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
import com.tencent.devops.model.store.tables.TStoreComponentContract
import com.tencent.devops.model.store.tables.records.TStoreComponentContractRecord
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractCoverageEnum
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractWriteReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record3
import org.jooq.Record5
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreComponentContractDao {

    fun getContract(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCode: String,
        forUpdate: Boolean = false
    ): TStoreComponentContractRecord? {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            val query = dslContext.selectFrom(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
            return if (forUpdate) query.forUpdate().fetchOne() else query.fetchOne()
        }
    }

    /**
     * 读取全部契约的选型字段，不读契约正文
     */
    fun listSelectionFields(
        dslContext: DSLContext,
        storeType: StoreTypeEnum
    ): Result<Record5<String, String, String, String, Boolean>> {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            return dslContext.select(
                STORE_CODE,
                SELECTION_SUMMARY,
                SELECTION_KEYWORDS,
                COVERAGE,
                STALE_FLAG
            ).from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .fetch()
        }
    }

    fun listContractBodies(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCodes: Collection<String>
    ): Map<String, String> {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            return dslContext.select(STORE_CODE, CONTRACT)
                .from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.`in`(storeCodes))
                .fetch()
                .associate { it.value1() to it.value2() }
        }
    }

    /**
     * 按过期时间先后列出待重新抽取的契约：组件标识、最近看到的版本、连续失败次数
     */
    fun listStaleForExtract(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        staleReason: String,
        maxFailCount: Int,
        limit: Int
    ): Result<Record3<String, String, Int>> {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            return dslContext.select(STORE_CODE, LAST_SEEN_VERSION, EXTRACT_FAIL_COUNT)
                .from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STALE_FLAG.eq(true))
                .and(STALE_REASON.eq(staleReason))
                .and(EXTRACT_FAIL_COUNT.lt(maxFailCount))
                .orderBy(LAST_SEEN_TIME.asc())
                .limit(limit)
                .fetch()
        }
    }

    fun listStoreCodes(dslContext: DSLContext, storeType: StoreTypeEnum): Set<String> {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            return dslContext.select(STORE_CODE)
                .from(this)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .fetch()
                .map { it.value1() }
                .toSet()
        }
    }

    fun increaseExtractFailCount(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Int {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            return dslContext.update(this)
                .set(EXTRACT_FAIL_COUNT, EXTRACT_FAIL_COUNT.plus(1))
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .execute()
        }
    }

    fun createContract(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        writeReq: StoreComponentContractWriteReq
    ) {
        val now = LocalDateTime.now()
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            dslContext.insertInto(this)
                .set(ID, UUIDUtil.generate())
                .set(STORE_CODE, storeCode)
                .set(STORE_TYPE, storeType.type.toByte())
                .set(SCHEMA_VERSION, writeReq.schemaVersion)
                .set(BASED_ON_VERSION, writeReq.basedOnVersion)
                .set(SOURCE_COMMIT, writeReq.sourceCommit)
                .set(PROPS_HASH, writeReq.propsHash)
                .set(COVERAGE, writeReq.coverage.name)
                .set(STALE_FLAG, false)
                .set(LAST_SEEN_VERSION, writeReq.basedOnVersion)
                .set(LAST_SEEN_TIME, now)
                .set(EXTRACT_FAIL_COUNT, 0)
                .set(SELECTION_SUMMARY, writeReq.selectionSummary)
                .set(SELECTION_KEYWORDS, writeReq.selectionKeywords?.joinToString(","))
                .set(CONTRACT, writeReq.contract)
                .set(GENERATED_DESCRIPTION, writeReq.generatedDescription)
                .set(REPORT_PATH, writeReq.reportPath)
                .set(EXTRACT_BUILD_ID, writeReq.extractBuildId)
                .set(CREATOR, userId)
                .set(MODIFIER, userId)
                .set(CREATE_TIME, now)
                .set(UPDATE_TIME, now)
                .execute()
        }
    }

    @Suppress("LongParameterList")
    fun overwriteContract(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        writeReq: StoreComponentContractWriteReq,
        staleFlag: Boolean,
        lastSeenVersion: String
    ) {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            val baseStep = dslContext.update(this)
                .set(SCHEMA_VERSION, writeReq.schemaVersion)
                .set(BASED_ON_VERSION, writeReq.basedOnVersion)
                .set(SOURCE_COMMIT, writeReq.sourceCommit)
                .set(PROPS_HASH, writeReq.propsHash)
                .set(COVERAGE, writeReq.coverage.name)
                .set(STALE_FLAG, staleFlag)
                .set(LAST_SEEN_VERSION, lastSeenVersion)
                .set(EXTRACT_FAIL_COUNT, 0)
                .set(SELECTION_SUMMARY, writeReq.selectionSummary)
                .set(SELECTION_KEYWORDS, writeReq.selectionKeywords?.joinToString(","))
                .set(CONTRACT, writeReq.contract)
                .set(GENERATED_DESCRIPTION, writeReq.generatedDescription)
                .set(REPORT_PATH, writeReq.reportPath)
                .set(EXTRACT_BUILD_ID, writeReq.extractBuildId)
                .set(REVIEWER, null as String?)
                .set(REVIEW_TIME, null as LocalDateTime?)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
            val step = if (staleFlag) baseStep else baseStep.set(STALE_REASON, null as String?)
            step.where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .execute()
        }
    }

    fun markPendingCompare(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        staleReason: String
    ) {
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            dslContext.update(this)
                .set(STALE_FLAG, true)
                .set(STALE_REASON, staleReason)
                .set(EXTRACT_FAIL_COUNT, 0)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .execute()
        }
    }

    fun markStaleOnRelease(
        dslContext: DSLContext,
        storeType: StoreTypeEnum,
        storeCode: String,
        staleReason: String,
        version: String
    ): Int {
        val now = LocalDateTime.now()
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            return dslContext.update(this)
                .set(STALE_FLAG, true)
                .set(STALE_REASON, staleReason)
                .set(LAST_SEEN_VERSION, version)
                .set(LAST_SEEN_TIME, now)
                .set(UPDATE_TIME, now)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .execute()
        }
    }

    fun verifyContract(
        dslContext: DSLContext,
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Int {
        val now = LocalDateTime.now()
        with(TStoreComponentContract.T_STORE_COMPONENT_CONTRACT) {
            return dslContext.update(this)
                .set(COVERAGE, StoreComponentContractCoverageEnum.VERIFIED.name)
                .set(REVIEWER, userId)
                .set(REVIEW_TIME, now)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, now)
                .where(STORE_TYPE.eq(storeType.type.toByte()))
                .and(STORE_CODE.eq(storeCode))
                .execute()
        }
    }
}
