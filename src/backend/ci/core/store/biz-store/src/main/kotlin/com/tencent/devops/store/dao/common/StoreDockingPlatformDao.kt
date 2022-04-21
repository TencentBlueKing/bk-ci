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
                MODIFIER
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
                    userId
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
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, id: String) {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
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

    fun getStoreDockingPlatforms(
        dslContext: DSLContext,
        platformName: String?,
        page: Int,
        pageSize: Int
    ): Result<TStoreDockingPlatformRecord>? {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            val conditions = getStoreDockingPlatformsCondition(platformName)
            return dslContext
                .selectFrom(this)
                .where(conditions).orderBy(CREATE_TIME.desc())
                .offset((page - 1) * pageSize).limit(pageSize)
                .fetch()
        }
    }

    fun getStoreDockingPlatformCount(
        dslContext: DSLContext,
        platformName: String?
    ): Long {
        with(TStoreDockingPlatform.T_STORE_DOCKING_PLATFORM) {
            val conditions = getStoreDockingPlatformsCondition(platformName)
            return dslContext
                .selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun TStoreDockingPlatform.getStoreDockingPlatformsCondition(
        platformName: String?
    ): MutableList<Condition> {
        val conditions = mutableListOf<Condition>()
        if (!platformName.isNullOrBlank()) {
            conditions.add(PLATFORM_NAME.contains(platformName))
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
                modifier = modifier,
                createTime = DateTimeUtil.toDateTime(createTime),
                updateTime = DateTimeUtil.toDateTime(updateTime)
            )
        }
    }
}
