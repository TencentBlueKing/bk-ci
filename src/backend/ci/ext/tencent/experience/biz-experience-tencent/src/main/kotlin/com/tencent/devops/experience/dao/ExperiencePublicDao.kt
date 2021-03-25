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

package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperiencePublic
import com.tencent.devops.model.experience.tables.records.TExperiencePublicRecord
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperiencePublicDao {
    fun listHot(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        category: Int? = null,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .let {
                    if (null == category) it else it.and(CATEGORY.eq(category))
                }
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(DOWNLOAD_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listNew(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        category: Int? = null,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .let {
                    if (null == category) it else it.and(CATEGORY.eq(category))
                }
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(UPDATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listNecessary(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .and(NECESSARY.eq(true))
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(NECESSARY_INDEX.asc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listWithBanner(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .and(BANNER_URL.ne(StringUtils.EMPTY))
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(BANNER_INDEX.asc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun listLikeExperienceName(
        dslContext: DSLContext,
        experienceName: String,
        platform: String?
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .and(EXPERIENCE_NAME.like("%$experienceName%"))
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(UPDATE_TIME.desc())
                .limit(100)
                .fetch()
        }
    }

    fun create(
        dslContext: DSLContext,
        recordId: Long,
        projectId: String,
        experienceName: String,
        category: Int,
        platform: String,
        bundleIdentifier: String,
        endDate: LocalDateTime,
        size: Long,
        logoUrl: String
    ) {
        val now = LocalDateTime.now()
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.insertInto(
                this,
                RECORD_ID,
                PROJECT_ID,
                EXPERIENCE_NAME,
                CATEGORY,
                PLATFORM,
                BUNDLE_IDENTIFIER,
                END_DATE,
                ONLINE,
                CREATE_TIME,
                UPDATE_TIME,
                DOWNLOAD_TIME,
                SIZE,
                LOGO_URL
            ).values(
                recordId,
                projectId,
                experienceName,
                category,
                platform,
                bundleIdentifier,
                endDate,
                true,
                now,
                now,
                0,
                size,
                logoUrl
            ).onDuplicateKeyUpdate()
                .set(RECORD_ID, recordId)
                .set(EXPERIENCE_NAME, experienceName)
                .set(CATEGORY, category)
                .set(END_DATE, endDate)
                .set(ONLINE, true)
                .set(UPDATE_TIME, now)
                .set(SIZE, size)
                .set(LOGO_URL, logoUrl)
                .execute()
        }
    }

    fun getById(
        dslContext: DSLContext,
        id: Long
    ): TExperiencePublicRecord? {
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this).where(ID.eq(id)).fetchOne()
        }
    }

    fun countByRecordId(
        dslContext: DSLContext,
        recordId: Long
    ): Int {
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectCount().where(RECORD_ID.eq(recordId)).execute()
        }
    }

    @SuppressWarnings("ALL")
    fun updateById(
        dslContext: DSLContext,
        id: Long,
        online: Boolean? = null,
        necessary: Boolean? = null,
        bannerUrl: String? = null,
        necessaryIndex: Int? = null,
        bannerIndex: Int? = null
    ) {
        val now = LocalDateTime.now()
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.update(this)
                .set(UPDATE_TIME, now)
                .let { if (null == online) it else it.set(ONLINE, online) }
                .let { if (null == necessary) it else it.set(NECESSARY, necessary) }
                .let { if (null == bannerUrl) it else it.set(BANNER_URL, bannerUrl) }
                .let { if (null == necessaryIndex) it else it.set(NECESSARY_INDEX, necessaryIndex) }
                .let { if (null == bannerIndex) it else it.set(BANNER_INDEX, bannerIndex) }
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateByBundleId(
        dslContext: DSLContext,
        projectId: String,
        platform: String,
        bundleIdentifier: String,
        online: Boolean?
    ) {
        val now = LocalDateTime.now()
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.update(this)
                .set(UPDATE_TIME, now)
                .let { if (null == online) it else it.set(ONLINE, online) }
                .where(PROJECT_ID.eq(projectId).and(PLATFORM.eq(platform)).and(BUNDLE_IDENTIFIER.eq(bundleIdentifier)))
                .execute()
        }
    }

    fun updateByRecordId(
        dslContext: DSLContext,
        recordId: Long,
        online: Boolean? = null,
        endDate: LocalDateTime? = null
    ) {
        val now = LocalDateTime.now()
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.update(this)
                .set(UPDATE_TIME, now)
                .let { if (null == online) it else it.set(ONLINE, online) }
                .let { if (null == endDate) it else it.set(END_DATE, endDate) }
                .where(RECORD_ID.eq(recordId))
                .execute()
        }
    }

    fun count(
        dslContext: DSLContext,
        category: Int? = null,
        necessary: Boolean? = null,
        withBanner: Boolean? = null,
        platform: String?
    ): Int {
        val now = LocalDateTime.now()

        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectCount().from(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .let {
                    if (null == category) it else it.and(CATEGORY.eq(category))
                }.let {
                    if (null == necessary) it else it.and(NECESSARY.eq(necessary))
                }.let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }.let {
                    if (null == withBanner) it else it.and(
                        if (withBanner) BANNER_URL.ne(StringUtils.EMPTY) else BANNER_URL.eq(StringUtils.EMPTY)
                    )
                }
                .fetchOne().value1()
        }
    }

    fun addDownloadTimeByRecordId(dslContext: DSLContext, recordId: Long) {
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.update(this)
                .set(DOWNLOAD_TIME, DOWNLOAD_TIME.plus(1))
                .where(RECORD_ID.eq(recordId))
                .execute()
        }
    }
}
