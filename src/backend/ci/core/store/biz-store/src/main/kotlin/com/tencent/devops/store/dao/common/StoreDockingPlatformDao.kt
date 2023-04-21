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

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TStoreDockingPlatform
import com.tencent.devops.model.store.tables.TStoreDockingPlatformErrorCode
import com.tencent.devops.model.store.tables.records.TStoreDockingPlatformErrorCodeRecord
import com.tencent.devops.model.store.tables.records.TStoreDockingPlatformRecord
import com.tencent.devops.store.pojo.common.StoreDockingPlatformInfo
import com.tencent.devops.store.pojo.common.StoreDockingPlatformRequest
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class StoreDockingPlatformDao {

    fun add(
        dslContext: DSLContext,
        userId: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ) {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            dslContext.insertInto(
                this,
                ID,
                PLATFORM_CODE,
                PLATFORM_NAME,
                WEBSITE,
                SUMMARY,
                PRINCIPAL,
                LOGO_URL,
                CREATOR,
                MODIFIER,
                OWNER_DEPT_NAME,
                LABELS,
                ERROR_CODE_PREFIX
            )
                .values(
                    UUIDUtil.generate(),
                    storeDockingPlatformRequest.platformCode,
                    storeDockingPlatformRequest.platformName,
                    storeDockingPlatformRequest.website,
                    storeDockingPlatformRequest.summary,
                    storeDockingPlatformRequest.principal,
                    storeDockingPlatformRequest.logoUrl,
                    userId,
                    userId,
                    storeDockingPlatformRequest.ownerDeptName,
                    storeDockingPlatformRequest.labels?.joinToString(","),
                    storeDockingPlatformRequest.errorCodePrefix
                ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        id: String,
        userId: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ) {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            dslContext.update(this)
                .set(PLATFORM_CODE, storeDockingPlatformRequest.platformCode)
                .set(PLATFORM_NAME, storeDockingPlatformRequest.platformName)
                .set(WEBSITE, storeDockingPlatformRequest.website)
                .set(SUMMARY, storeDockingPlatformRequest.summary)
                .set(PRINCIPAL, storeDockingPlatformRequest.principal)
                .set(LOGO_URL, storeDockingPlatformRequest.logoUrl)
                .set(LABELS, storeDockingPlatformRequest.labels?.joinToString(","))
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .set(OWNER_DEPT_NAME, storeDockingPlatformRequest.ownerDeptName)
                .set(ERROR_CODE_PREFIX, storeDockingPlatformRequest.errorCodePrefix)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        userId: String,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ) {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            var step = dslContext.update(this)
                .set(SUMMARY, storeDockingPlatformRequest.summary)
                .set(PRINCIPAL, storeDockingPlatformRequest.principal)
                .set(PLATFORM_NAME, storeDockingPlatformRequest.platformName)
                .set(WEBSITE, storeDockingPlatformRequest.website)
                .set(LABELS, storeDockingPlatformRequest.labels?.joinToString(","))
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .set(OWNER_DEPT_NAME, storeDockingPlatformRequest.ownerDeptName)
            if (!storeDockingPlatformRequest.logoUrl.isNullOrBlank()) {
                step = step.set(LOGO_URL, storeDockingPlatformRequest.logoUrl)
            }
            step.where(PLATFORM_CODE.eq(storeDockingPlatformRequest.platformCode)).execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        storeDockingPlatformRequest: StoreDockingPlatformRequest
    ) {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            dslContext.deleteFrom(this)
                .where(PLATFORM_CODE.eq(storeDockingPlatformRequest.platformCode))
                .and(OWNER_DEPT_NAME.eq(storeDockingPlatformRequest.ownerDeptName))
                .execute()
        }
    }

    fun countByName(dslContext: DSLContext, platformName: String): Int {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.selectCount().from(this)
                .where(PLATFORM_NAME.eq(platformName))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByErrorCodePrefix(dslContext: DSLContext, errorCodePrefix: Int): Int {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.selectCount().from(this)
                .where(ERROR_CODE_PREFIX.eq(errorCodePrefix))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun countByCode(dslContext: DSLContext, platformCode: String): Int {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.selectCount().from(this)
                .where(PLATFORM_CODE.eq(platformCode))
                .fetchOne(0, Int::class.java)!!
        }
    }

    fun getStoreDockingPlatform(dslContext: DSLContext, id: String): TStoreDockingPlatformRecord? {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun getStoreDockingPlatformByErrorCode(dslContext: DSLContext, prefix: Int): TStoreDockingPlatformRecord? {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.selectFrom(this)
                .where(ERROR_CODE_PREFIX.eq(prefix))
                .fetchOne()
        }
    }

    fun getStoreDockingPlatformByCode(dslContext: DSLContext, platformCode: String): String? {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.select(ID)
                .from(this)
                .where(PLATFORM_CODE.eq(platformCode))
                .fetchOne(0, String::class.java)
        }
    }

    fun updateStoreDockingPlatformLogoUrl(dslContext: DSLContext, id: String, logoUrl: String): Int {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.update(this)
                .set(LOGO_URL, logoUrl)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getStoreDockingPlatforms(
        dslContext: DSLContext,
        platformName: String?,
        id: String?,
        page: Int,
        pageSize: Int
    ): List<StoreDockingPlatformInfo>? {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            val conditions = getStoreDockingPlatformsCondition(platformName, id)
            val storeDockingPlatformRecords = dslContext
                .selectFrom(this)
                .where(conditions).orderBy(CREATE_TIME.desc())
                .offset((page - 1) * pageSize).limit(pageSize)
                .fetch()
            return generateStoreDockingPlatformInfos(storeDockingPlatformRecords)
        }
    }

    fun getStoreDockingPlatforms(
        dslContext: DSLContext,
        platformCodes: List<String>
    ): List<StoreDockingPlatformInfo>? {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            val storeDockingPlatformRecords = dslContext
                .selectFrom(this)
                .where(PLATFORM_CODE.`in`(platformCodes))
                .fetch()
            return generateStoreDockingPlatformInfos(storeDockingPlatformRecords)
        }
    }

    private fun generateStoreDockingPlatformInfos(
        storeDockingPlatformRecords: Result<TStoreDockingPlatformRecord>
    ): MutableList<StoreDockingPlatformInfo>? {
        var storeDockingPlatformInfos: MutableList<StoreDockingPlatformInfo>? = null
        if (!storeDockingPlatformRecords.isNullOrEmpty()) {
            storeDockingPlatformInfos = mutableListOf()
            storeDockingPlatformRecords.forEach { storeDockingPlatformRecord ->
                val storeDockingPlatformInfo = convert(storeDockingPlatformRecord)
                storeDockingPlatformInfos.add(storeDockingPlatformInfo)
            }
        }
        return storeDockingPlatformInfos
    }

    fun getStoreDockingPlatformCount(
        dslContext: DSLContext,
        platformName: String?,
        id: String?
    ): Long {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            val conditions = getStoreDockingPlatformsCondition(platformName, id)
            return dslContext
                .selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun TStoreDockingPlatform.getStoreDockingPlatformsCondition(
        platformName: String?,
        id: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!platformName.isNullOrBlank()) {
            conditions.add(PLATFORM_NAME.contains(platformName))
        }
        if (!id.isNullOrBlank()) {
            conditions.add(ID.eq(id))
        }
        return conditions
    }

    fun convert(record: TStoreDockingPlatformRecord): StoreDockingPlatformInfo {
        with(record) {
            return StoreDockingPlatformInfo(
                id = id,
                platformCode = platformCode,
                platformName = platformName,
                website = website,
                summary = summary,
                principal = principal,
                logoUrl = logoUrl,
                creator = creator,
                labels = labels?.split(","),
                modifier = modifier,
                createTime = DateTimeUtil.toDateTime(createTime),
                updateTime = DateTimeUtil.toDateTime(updateTime),
                ownerDeptName = record.ownerDeptName
            )
        }
    }

    fun isPlatformCodeRegistered(dslContext: DSLContext, platformCode: String): Boolean {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            return dslContext.selectFrom(this).where(PLATFORM_CODE.eq(platformCode)).fetchOne() != null
        }
    }

    fun getPlatformErrorCode(
        dslContext: DSLContext,
        platformCode: String,
        errorCode: Int
    ): TStoreDockingPlatformErrorCodeRecord? {
        with(TStoreDockingPlatformErrorCode.T_STORE_DOCKING_PLATFORM_ERROR_CODE) {
            return dslContext.selectFrom(this)
                .where(PLATFORM_CODE.eq(platformCode))
                .and(ERROR_CODE.eq(errorCode))
                .fetchOne()
        }
    }
}
