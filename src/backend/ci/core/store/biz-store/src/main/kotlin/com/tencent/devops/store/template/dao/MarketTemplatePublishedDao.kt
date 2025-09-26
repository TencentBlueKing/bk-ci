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

package com.tencent.devops.store.template.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.api.util.toLocalDateTimeOrDefault
import com.tencent.devops.model.store.tables.TTemplateVersionReleasedRel
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class MarketTemplatePublishedDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        record: TemplatePublishedVersionInfo
    ) {
        with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            dslContext.insertInto(
                this,
                PROJECT_CODE,
                TEMPLATE_CODE,
                VERSION,
                NUMBER,
                VERSION_NAME,
                PUBLISHED,
                CREATOR,
                UPDATER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                record.projectCode,
                record.templateCode,
                record.version,
                record.number,
                record.versionName,
                record.published,
                record.creator,
                record.updater,
                record.createTime.toLocalDateTimeOrDefault(),
                record.updateTime.toLocalDateTimeOrDefault(),
            ).onDuplicateKeyUpdate()
                .set(VERSION, record.version)
                .set(CREATOR, record.creator)
                .set(PUBLISHED, record.published)
                .set(NUMBER, record.number)
                .set(VERSION_NAME, record.versionName)
                .set(UPDATER, record.updater)
                .set(CREATE_TIME, record.createTime.toLocalDateTimeOrDefault())
                .set(UPDATE_TIME, record.updateTime.toLocalDateTimeOrDefault())
                .execute()
        }
    }

    fun offlineTemplate(
        dslContext: DSLContext,
        templateCode: String,
        templateVersion: Long?
    ) {
        with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            dslContext.update(this)
                .set(PUBLISHED, false)
                .where(TEMPLATE_CODE.eq(templateCode))
                .let { if (templateVersion != null) it.and(VERSION.eq(templateVersion)) else it }
                .execute()
        }
    }

    fun getLatestMarketPublishedVersion(
        dslContext: DSLContext,
        templateCode: String
    ): TemplatePublishedVersionInfo? {
        return with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .orderBy(NUMBER.desc())
                .limit(1)
                .fetchOne()?.let {
                    TemplatePublishedVersionInfo(
                        projectCode = it.projectCode,
                        templateCode = it.templateCode,
                        version = it.version,
                        number = it.number,
                        versionName = it.versionName,
                        published = it.published,
                        createTime = it.createTime.timestampmilli(),
                        updateTime = it.updateTime.timestampmilli(),
                        creator = it.creator,
                        updater = it.updater
                    )
                }
        }
    }

    fun listPublishedHistory(
        dslContext: DSLContext,
        templateCode: String,
        page: Int,
        pageSize: Int
    ): List<TemplatePublishedVersionInfo> {
        return with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .orderBy(UPDATE_TIME.desc())
                .limit((page - 1) * pageSize, pageSize)
                .fetch().map {
                    TemplatePublishedVersionInfo(
                        projectCode = it.projectCode,
                        templateCode = it.templateCode,
                        version = it.version,
                        number = it.number,
                        versionName = it.versionName,
                        published = it.published,
                        createTime = it.createTime.timestampmilli(),
                        updateTime = it.updateTime.timestampmilli(),
                        creator = it.creator,
                        updater = it.updater
                    )
                }
        }
    }

    fun countPublishedHistory(
        dslContext: DSLContext,
        templateCode: String,
    ): Long {
        return with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            dslContext.selectCount()
                .from(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .orderBy(UPDATE_TIME.desc())
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun listLatestPublishedVersions(
        dslContext: DSLContext,
        templateCodes: List<String>
    ): List<TemplatePublishedVersionInfo> {
        return with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            // 子查询获取每个模板的最大NUMBER
            val maxNumbers = dslContext.select(TEMPLATE_CODE, DSL.max(NUMBER).`as`("max_number"))
                .from(this)
                .where(TEMPLATE_CODE.`in`(templateCodes))
                .and(PUBLISHED.eq(true))
                .groupBy(TEMPLATE_CODE)
                .asTable("m")
            // 主查询关联获取VERSION
            dslContext.select()
                .from(this)
                .join(maxNumbers)
                .on(
                    TEMPLATE_CODE.eq(maxNumbers.field(TEMPLATE_CODE)),
                    NUMBER.eq(maxNumbers.field("max_number", Int::class.java))
                )
                .fetch().map {
                    TemplatePublishedVersionInfo(
                        projectCode = it[PROJECT_CODE],
                        templateCode = it[TEMPLATE_CODE],
                        version = it[VERSION],
                        number = it[NUMBER],
                        versionName = it[VERSION_NAME],
                        published = it[PUBLISHED],
                        createTime = it[CREATE_TIME].timestampmilli(),
                        updateTime = it[UPDATE_TIME]?.timestampmilli(),
                        creator = it[CREATOR],
                        updater = it[UPDATER]
                    )
                }
        }
    }

    fun delete(
        dslContext: DSLContext,
        templateCode: String,
        versions: List<Long>
    ) {
        with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .and(VERSION.`in`(versions))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        templateCode: String
    ) {
        with(TTemplateVersionReleasedRel.T_TEMPLATE_VERSION_RELEASED_REL) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .execute()
        }
    }
}
