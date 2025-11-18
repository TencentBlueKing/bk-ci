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
import com.tencent.devops.model.store.tables.TTemplateVersionInstallHistory
import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class TemplateInstallHistoryDao {
    fun create(
        dslContext: DSLContext,
        record: TemplateVersionInstallHistoryInfo
    ) {
        with(TTemplateVersionInstallHistory.T_TEMPLATE_VERSION_INSTALL_HISTORY) {
            dslContext.insertInto(
                this,
                SRC_MARKET_TEMPLATE_PROJECT_CODE,
                SRC_MARKET_TEMPLATE_CODE,
                PROJECT_CODE,
                TEMPLATE_CODE,
                VERSION,
                VERSION_NAME,
                NUMBER,
                CREATOR,
                CREATE_TIME
            ).values(
                record.srcMarketTemplateProjectCode,
                record.srcMarketTemplateCode,
                record.projectCode,
                record.templateCode,
                record.version,
                record.versionName,
                record.number,
                record.creator,
                record.createTime.toLocalDateTimeOrDefault()
            ).onDuplicateKeyUpdate()
                .set(CREATOR, record.creator)
                .set(VERSION, record.version)
                .set(VERSION_NAME, record.versionName)
                .set(NUMBER, record.number)
                .set(CREATE_TIME, record.createTime.toLocalDateTimeOrDefault())
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        templateCode: String
    ) {
        with(TTemplateVersionInstallHistory.T_TEMPLATE_VERSION_INSTALL_HISTORY) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        srcTemplateCode: String,
        templateCode: String,
        versions: List<Long>
    ) {
        with(TTemplateVersionInstallHistory.T_TEMPLATE_VERSION_INSTALL_HISTORY) {
            dslContext.deleteFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .and(SRC_MARKET_TEMPLATE_CODE.eq(srcTemplateCode))
                .and(VERSION.`in`(versions))
                .execute()
        }
    }

    fun getRecentlyInstalledVersion(
        dslContext: DSLContext,
        templateCode: String
    ): TemplateVersionInstallHistoryInfo? {
        return with(TTemplateVersionInstallHistory.T_TEMPLATE_VERSION_INSTALL_HISTORY) {
            dslContext.selectFrom(this)
                .where(TEMPLATE_CODE.eq(templateCode))
                .orderBy(CREATE_TIME.desc(), NUMBER.desc())
                .limit(1)
                .fetchOne()?.let {
                    TemplateVersionInstallHistoryInfo(
                        srcMarketTemplateProjectCode = it.srcMarketTemplateCode,
                        srcMarketTemplateCode = it.srcMarketTemplateCode,
                        projectCode = it.projectCode,
                        templateCode = it.templateCode,
                        version = it.version,
                        versionName = it.versionName,
                        number = it.number,
                        creator = it.creator,
                        createTime = it.createTime.timestampmilli()
                    )
                }
        }
    }

    fun listLatestInstalledVersions(
        dslContext: DSLContext,
        templateCodes: List<String>
    ): List<TemplateVersionInstallHistoryInfo> {
        return with(TTemplateVersionInstallHistory.T_TEMPLATE_VERSION_INSTALL_HISTORY) {
            // 子查询获取每个模板的最大NUMBER
            val maxNumbers = dslContext.select(TEMPLATE_CODE, DSL.max(NUMBER).`as`("max_number"))
                .from(this)
                .where(TEMPLATE_CODE.`in`(templateCodes))
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
                    TemplateVersionInstallHistoryInfo(
                        srcMarketTemplateProjectCode = it[SRC_MARKET_TEMPLATE_PROJECT_CODE],
                        srcMarketTemplateCode = it[SRC_MARKET_TEMPLATE_CODE],
                        projectCode = it[PROJECT_CODE],
                        templateCode = it[TEMPLATE_CODE],
                        version = it[VERSION],
                        versionName = it[VERSION_NAME],
                        number = it[NUMBER],
                        creator = it[CREATOR],
                        createTime = it[CREATE_TIME].timestampmilli()
                    )
                }
        }
    }
}
