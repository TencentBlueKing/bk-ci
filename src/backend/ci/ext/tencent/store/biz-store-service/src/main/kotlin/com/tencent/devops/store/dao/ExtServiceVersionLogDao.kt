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

package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TExtensionServiceVersionLog
import com.tencent.devops.model.store.tables.records.TExtensionServiceVersionLogRecord
import com.tencent.devops.store.pojo.ExtServiceVersionLogCreateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogUpdateInfo
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceVersionLogDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        id: String,
        extServiceVersionLogCreateInfo: ExtServiceVersionLogCreateInfo
    ) {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_ID,
                RELEASE_TYPE,
                CONTENT,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    extServiceVersionLogCreateInfo.serviceId,
                    extServiceVersionLogCreateInfo.releaseType,
                    extServiceVersionLogCreateInfo.content,
                    extServiceVersionLogCreateInfo.creatorUser,
                    extServiceVersionLogCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                ).onDuplicateKeyUpdate()
                .set(RELEASE_TYPE, extServiceVersionLogCreateInfo.releaseType)
                .set(CONTENT, extServiceVersionLogCreateInfo.content)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun updateExtServiceFeatureBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        extServiceVersionLogUpdateInfo: ExtServiceVersionLogUpdateInfo
    ) {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            val baseStep = dslContext.update(this)
            val content = extServiceVersionLogUpdateInfo.content
            if (null != content) {
                baseStep.set(CONTENT, content)
            }
            val releaseType = extServiceVersionLogUpdateInfo.releaseType
            if (null != releaseType) {
                baseStep.set(RELEASE_TYPE, releaseType.toByte())
            }
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }

    fun getVersionLogById(
        dslContext: DSLContext,
        logId: String
    ): TExtensionServiceVersionLogRecord {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectFrom(this).where(ID.eq(logId)).fetchOne()!!
        }
    }

    fun getVersionLogsByServiceIds(
        dslContext: DSLContext,
        serviceIds: List<String>
    ): Result<TExtensionServiceVersionLogRecord>? {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectFrom(this).where(SERVICE_ID.`in`(serviceIds)).fetch()
        }
    }

    fun getVersionLogByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): TExtensionServiceVersionLogRecord? {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectFrom(this)
                .where(SERVICE_ID.eq(serviceId))
                .orderBy(CREATE_TIME.desc())
                .limit(0, 1)
                .fetchOne()
        }
    }

    fun listVersionLogByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): Result<TExtensionServiceVersionLogRecord>? {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectFrom(this).where(SERVICE_ID.eq(serviceId)).fetch()
        }
    }

    fun countVersionLogByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): Int {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectCount().from(this).where(SERVICE_ID.eq(serviceId)).fetchOne(0, Int::class.java)!!
        }
    }

    fun deleteByServiceId(dslContext: DSLContext, extServiceIds: List<String>) {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            dslContext.deleteFrom(this)
                .where(SERVICE_ID.`in`(extServiceIds))
                .execute()
        }
    }
}
