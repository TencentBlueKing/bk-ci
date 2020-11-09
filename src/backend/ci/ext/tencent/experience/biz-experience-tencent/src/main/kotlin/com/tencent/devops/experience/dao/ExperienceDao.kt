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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.experience.dao

import com.tencent.devops.model.experience.tables.TExperience
import com.tencent.devops.model.experience.tables.records.TExperienceRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.net.URLDecoder
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
class ExperienceDao {
    fun listIDGroupByProjectIdAndBundleIdentifier(
        dslContext: DSLContext,
        projectIdSet: Set<String>,
        expireTime: LocalDateTime,
        online: Boolean
    ): Result<Record1<Long>> {
        with(TExperience.T_EXPERIENCE) {
            return dslContext.select(DSL.max(ID))
                .from(this)
                .where(PROJECT_ID.`in`(projectIdSet))
                .and(END_DATE.gt(expireTime))
                .and(ONLINE.eq(online))
                .groupBy(PROJECT_ID, BUNDLE_IDENTIFIER, PLATFORM)
                .fetch()
        }
    }

    fun listIDByProjectId(
        dslContext: DSLContext,
        projectIdSet: Set<String>,
        expireTime: LocalDateTime,
        online: Boolean
    ): Result<Record1<Long>> {
        with(TExperience.T_EXPERIENCE) {
            return dslContext.select(ID)
                .from(this)
                .where(PROJECT_ID.`in`(projectIdSet))
                .and(END_DATE.gt(expireTime))
                .and(ONLINE.eq(online))
                .fetch()
        }
    }

    fun getProjectIdByInnerUser(
        dslContext: DSLContext,
        userId: String,
        expireTime: LocalDateTime,
        online: Boolean
    ): Result<Record1<String>>? {
        with(TExperience.T_EXPERIENCE) {
            return dslContext.selectDistinct(PROJECT_ID)
                .from(this)
                .where(END_DATE.gt(expireTime))
                .and(ONLINE.eq(online))
                .and(
                    INNER_USERS.like(
                        "%" + URLDecoder.decode(
                            userId,
                            "UTF-8"
                        ) + "%"
                    )
                )
                .fetch()
        }
    }

    fun list(dslContext: DSLContext, idSet: Set<Long>): Result<TExperienceRecord> {
        with(TExperience.T_EXPERIENCE) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(idSet))
                .fetch()
        }
    }

    fun list(
        dslContext: DSLContext,
        projectId: String,
        expireTime: LocalDateTime?,
        online: Boolean?
    ): Result<TExperienceRecord> {
        with(TExperience.T_EXPERIENCE) {
            val step1 = dslContext.selectFrom(this).where(PROJECT_ID.eq(projectId))
            val step2 = if (expireTime == null) step1 else step1.and(END_DATE.gt(expireTime))
            val step3 = if (online == null) step2 else step2.and(ONLINE.eq(online))
            return step3.orderBy(CREATE_TIME.desc()).fetch()
        }
    }

    fun listByBundleIdentifier(
        dslContext: DSLContext,
        projectId: String,
        bundleIdentifier: String
    ): Result<TExperienceRecord> {
        with(TExperience.T_EXPERIENCE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(BUNDLE_IDENTIFIER.eq(bundleIdentifier))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun getOrNull(dslContext: DSLContext, experienceId: Long): TExperienceRecord? {
        with(TExperience.T_EXPERIENCE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(experienceId))
                .fetchOne()
        }
    }

    fun get(dslContext: DSLContext, experienceId: Long): TExperienceRecord {
        with(TExperience.T_EXPERIENCE) {
            return dslContext.selectFrom(this)
                .where(ID.eq(experienceId))
                .fetchOne() ?: throw NotFoundException("Experience: $experienceId not found")
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        platform: String,
        path: String,
        artifactoryType: String,
        artifactorySha1: String,
        bundleIdentifier: String,
        version: String,
        remark: String?,
        endDate: LocalDateTime,
        experienceGroups: String,
        innerUsers: String,
        outerUsers: String,
        notifyTypes: String,
        enableWechatGroup: Boolean,
        wechatGroups: String,
        online: Boolean,
        source: String,
        creator: String,
        updator: String,
        experienceName: String,
        versionTitle: String,
        category: Int,
        productOwner: String,
        iconUrl: String,
        size: Long
    ): Long {
        val now = LocalDateTime.now()
        with(TExperience.T_EXPERIENCE) {
            val record = dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                PLATFORM,
                ARTIFACTORY_PATH,
                ARTIFACTORY_TYPE,
                ARTIFACTORY_SHA1,
                BUNDLE_IDENTIFIER,
                VERSION,
                REMARK,
                END_DATE,
                EXPERIENCE_GROUPS,
                INNER_USERS,
                OUTER_USERS,
                NOTIFY_TYPES,
                ENABLE_WECHAT_GROUPS,
                WECHAT_GROUPS,
                ONLINE,
                SOURCE,
                CREATOR,
                UPDATOR,
                CREATE_TIME,
                UPDATE_TIME,
                EXPERIENCE_NAME,
                VERSION_TITLE,
                CATEGORY,
                PRODUCT_OWNER,
                ICON_URL,
                SIZE
            ).values(
                projectId,
                name,
                platform,
                path,
                artifactoryType,
                artifactorySha1,
                bundleIdentifier,
                version,
                remark,
                endDate,
                experienceGroups,
                innerUsers,
                outerUsers,
                notifyTypes,
                enableWechatGroup,
                wechatGroups,
                online,
                source,
                creator,
                updator,
                now,
                now,
                experienceName,
                versionTitle,
                category,
                productOwner,
                iconUrl,
                size
            )
                .returning(ID)
                .fetchOne()
            return record.id
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        name: String,
        remark: String?,
        endDate: LocalDateTime,
        experienceGroups: String,
        innerUsers: String,
        outerUsers: String,
        notifyTypes: String,
        enableWechatGroup: Boolean,
        wechatGroups: String,
        updator: String,
        experienceName: String,
        versionTitle: String,
        category: Int,
        productOwner: String
    ) {
        val now = LocalDateTime.now()
        with(TExperience.T_EXPERIENCE) {
            dslContext.update(this)
                .set(NAME, name)
                .set(REMARK, remark)
                .set(END_DATE, endDate)
                .set(EXPERIENCE_GROUPS, experienceGroups)
                .set(INNER_USERS, innerUsers)
                .set(OUTER_USERS, outerUsers)
                .set(NOTIFY_TYPES, notifyTypes)
                .set(ENABLE_WECHAT_GROUPS, enableWechatGroup)
                .set(WECHAT_GROUPS, wechatGroups)
                .set(UPDATOR, updator)
                .set(UPDATE_TIME, now)
                .set(EXPERIENCE_NAME, experienceName)
                .set(VERSION_TITLE, versionTitle)
                .set(CATEGORY, category)
                .set(PRODUCT_OWNER, productOwner)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun updateOnline(
        dslContext: DSLContext,
        id: Long,
        online: Boolean
    ) {
        val now = LocalDateTime.now()
        with(TExperience.T_EXPERIENCE) {
            dslContext.update(this)
                .set(ONLINE, online)
                .set(UPDATE_TIME, now)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun count(dslContext: DSLContext, projectIds: Set<String>, expired: Boolean): Result<Record>? {
        with(TExperience.T_EXPERIENCE) {
            val conditions = mutableListOf<Condition>()
            if (projectIds.isNotEmpty()) conditions.add(PROJECT_ID.`in`(projectIds))

            return dslContext.selectCount()
                .select(PROJECT_ID)
                .from(this)
                .where(conditions)
                .groupBy(PROJECT_ID)
                .fetch()
        }
    }
}