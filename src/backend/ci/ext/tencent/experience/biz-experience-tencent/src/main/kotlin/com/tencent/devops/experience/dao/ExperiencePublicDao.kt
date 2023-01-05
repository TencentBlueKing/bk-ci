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

import com.tencent.devops.experience.constant.ExperiencePublicType
import com.tencent.devops.model.experience.tables.TExperiencePublic
import com.tencent.devops.model.experience.tables.TExperienceSubscribe
import com.tencent.devops.model.experience.tables.records.TExperiencePublicRecord
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperiencePublicDao {

    @SuppressWarnings("ALL")
    fun listHot(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        category: Int? = null,
        platform: String?,
        types: List<Int>
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

    @SuppressWarnings("ALL")
    fun listNew(
        dslContext: DSLContext,
        offset: Int,
        limit: Int,
        category: Int? = null,
        platform: String?,
        types: List<Int>
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
        platform: String?,
        types: List<Int>
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
        platform: String?,
        projectId: String?
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
                .let {
                    if (projectId == null) it else it.and(PROJECT_ID.eq(projectId))
                }
                .orderBy(UPDATE_TIME.desc())
                .limit(100)
                .fetch()
        }
    }

    @SuppressWarnings("LongParameterList")
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
        logoUrl: String,
        scheme: String,
        type: Int = ExperiencePublicType.FROM_BKCI.id,
        externalUrl: String = "",
        version: String
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
                LOGO_URL,
                SCHEME,
                TYPE,
                EXTERNAL_LINK,
                VERSION
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
                logoUrl,
                scheme,
                type,
                externalUrl,
                version
            ).onDuplicateKeyUpdate()
                .set(RECORD_ID, recordId)
                .set(EXPERIENCE_NAME, experienceName)
                .set(CATEGORY, category)
                .set(END_DATE, endDate)
                .set(ONLINE, true)
                .set(UPDATE_TIME, now)
                .set(SIZE, size)
                .set(LOGO_URL, logoUrl)
                .set(SCHEME, scheme)
                .set(VERSION, version)
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
        recordId: Long,
        online: Boolean = true,
        expireTime: LocalDateTime? = null
    ): Int {
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectCount()
                .from(this)
                .where(RECORD_ID.eq(recordId))
                .and(ONLINE.eq(online))
                .let { if (expireTime == null) it else it.and(END_DATE.gt(expireTime)) }
                .fetchOne()?.get(0, Int::class.java) ?: 0
        }
    }

    fun getByRecordId(
        dslContext: DSLContext,
        recordId: Long
    ): TExperiencePublicRecord? {
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(RECORD_ID.eq(recordId))
                .fetchOne()
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
        bannerIndex: Int? = null,
        downloadTime: Int? = null,
        updateTime: LocalDateTime? = LocalDateTime.now(),
        version: String? = null
    ) {
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.update(this)
                .set(UPDATE_TIME, updateTime)
                .let { if (null == online) it else it.set(ONLINE, online) }
                .let { if (null == necessary) it else it.set(NECESSARY, necessary) }
                .let { if (null == bannerUrl) it else it.set(BANNER_URL, bannerUrl) }
                .let { if (null == necessaryIndex) it else it.set(NECESSARY_INDEX, necessaryIndex) }
                .let { if (null == bannerIndex) it else it.set(BANNER_INDEX, bannerIndex) }
                .let { if (null == downloadTime) it else it.set(DOWNLOAD_TIME, downloadTime) }
                .let { if (null == version) it else it.set(VERSION, version) }
                .where(ID.eq(id))
                .execute()
        }
    }

    fun getByBundleId(
        dslContext: DSLContext,
        projectId: String,
        platform: String,
        bundleIdentifier: String
    ): TExperiencePublicRecord? {
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PLATFORM.eq(platform))
                .and(BUNDLE_IDENTIFIER.eq(bundleIdentifier))
                .fetchAny()
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
    ): Int {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
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
                .fetchOne()!!.value1()
        }
    }

    fun filterRecordId(dslContext: DSLContext, records: Set<Long>): Result<Record1<Long>>? {
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.select(RECORD_ID)
                .from(this)
                .where(RECORD_ID.`in`(records))
                .fetch()
        }
    }

    fun listAllUnique(dslContext: DSLContext): Result<TExperiencePublicRecord> {
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            return dslContext.selectFrom(this)
                .where(END_DATE.gt(LocalDateTime.now()))
                .and(ONLINE.eq(true))
                .orderBy(UPDATE_TIME.desc())
                .limit(10000)
                .fetch()
        }
    }

    fun listAllRecordId(dslContext: DSLContext): Result<Record1<Long>>? {
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            return dslContext.select(RECORD_ID)
                .from(this)
                .where(END_DATE.gt(LocalDateTime.now()))
                .and(ONLINE.eq(true))
                .orderBy(UPDATE_TIME.desc())
                .limit(10000)
                .fetch()
        }
    }

    fun getNewestRecord(
        dslContext: DSLContext,
        projectId: String,
        bundleIdentifier: String,
        platform: String
    ): TExperiencePublicRecord? {
        with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(END_DATE.gt(LocalDateTime.now()))
                .and(ONLINE.eq(true))
                .and(BUNDLE_IDENTIFIER.eq(bundleIdentifier))
                .and(PLATFORM.eq(platform))
                .fetchOne()
        }
    }

    fun listSubscribeRecordIds(
        dslContext: DSLContext,
        userId: String,
        platform: String?,
        limit: Int
    ): List<Long> {
        val p = TExperiencePublic.T_EXPERIENCE_PUBLIC
        val s = TExperienceSubscribe.T_EXPERIENCE_SUBSCRIBE
        val join = p.leftJoin(s).on(
            p.BUNDLE_IDENTIFIER.eq(s.BUNDLE_IDENTIFIER)
                .and(p.PLATFORM.eq(s.PLATFORM))
                .and(p.PROJECT_ID.eq(s.PROJECT_ID))
        )
        return dslContext.select(p.RECORD_ID)
            .from(join)
            .where(s.USER_ID.eq(userId))
            .let { if (platform == null) it else it.and(s.PLATFORM.eq(platform)) }
            .orderBy(p.UPDATE_TIME.desc())
            .limit(limit)
            .fetch(p.RECORD_ID)
    }

    fun listExperienceByProjectId(
        dslContext: DSLContext,
        platform: String?,
        projectId: String
    ): Result<TExperiencePublicRecord> {
        val now = LocalDateTime.now()
        return with(TExperiencePublic.T_EXPERIENCE_PUBLIC) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .and(PROJECT_ID.eq(projectId))
                .let {
                    if (null == platform) it else it.and(PLATFORM.eq(platform))
                }
                .orderBy(DOWNLOAD_TIME.desc())
                .fetch()
        }
    }
}
