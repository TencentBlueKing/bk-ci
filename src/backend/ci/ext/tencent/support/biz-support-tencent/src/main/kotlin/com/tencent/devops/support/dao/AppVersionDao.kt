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

package com.tencent.devops.support.dao

import com.tencent.devops.model.support.tables.TAppVersion
import com.tencent.devops.model.support.tables.records.TAppVersionRecord
import com.tencent.devops.support.model.app.AppVersionRequest
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class AppVersionDao {
    fun getAllAppVersion(dslContext: DSLContext): Result<TAppVersionRecord>? {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext
                .selectFrom(this)
                .orderBy(RELEASE_DATE.desc(), ID.desc())
                .fetch()
        }
    }

    fun getAllAppVersionByChannelType(dslContext: DSLContext, channelType: Byte): Result<TAppVersionRecord>? {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext
                .selectFrom(this)
                .where(CHANNEL_TYPE.eq(channelType))
                .orderBy(RELEASE_DATE.desc(), ID.desc())
                .fetch()
        }
    }

    fun getAppVersion(dslContext: DSLContext, id: Long): TAppVersionRecord? {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
                .fetchOne()
        }
    }

    fun deleteAppVersion(dslContext: DSLContext, id: Long): Int {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun setAppVersion(dslContext: DSLContext, versionId: Long?, appVersionRequest: AppVersionRequest): Int {
        with(TAppVersion.T_APP_VERSION) {
            val releaseDate = Timestamp(appVersionRequest.releaseDate).toLocalDateTime()
            return if (versionId != null && exist(dslContext, versionId)) {
                // 存在的时候，更新
                dslContext.update(this)
                    .set(VERSION_ID, appVersionRequest.versionId)
                    .set(RELEASE_DATE, releaseDate)
                    .set(RELEASE_CONTENT, appVersionRequest.releaseContent)
                    .set(CHANNEL_TYPE, appVersionRequest.channelType)
                    .set(UPDATE_TYPE, appVersionRequest.updateType)
                    .where(ID.eq(versionId))
                    .execute()
            } else {
                // 不存在的时候,插入
                dslContext.insertInto(
                    this,
                    VERSION_ID,
                    RELEASE_DATE,
                    RELEASE_CONTENT,
                    CHANNEL_TYPE,
                    UPDATE_TYPE
                ).values(
                    appVersionRequest.versionId,
                    releaseDate,
                    appVersionRequest.releaseContent,
                    appVersionRequest.channelType,
                    appVersionRequest.updateType
                ).execute()
            }
        }
    }

    fun exist(dslContext: DSLContext, versionId: Long): Boolean {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.selectFrom(this).where(ID.eq(versionId)).fetchOne() != null
        }
    }

    fun getLastAppVersion(dslContext: DSLContext, channelType: Byte): TAppVersionRecord? {
        with(TAppVersion.T_APP_VERSION) {
            val records = dslContext.selectFrom(this)
                .where(CHANNEL_TYPE.eq(channelType))
                .orderBy(RELEASE_DATE.desc(), ID.desc())
                .fetch()

            return if (records.size > 0) records[0] else null
        }
    }

    fun listByChannelType(dslContext: DSLContext, channelType: Byte): Result<TAppVersionRecord> {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.selectFrom(this)
                .where(CHANNEL_TYPE.eq(channelType))
                .fetch()
        }
    }
}
