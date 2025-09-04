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

import com.tencent.devops.experience.pojo.ExperienceExtendBanner
import com.tencent.devops.model.experience.tables.TExperienceExtendBanner
import com.tencent.devops.model.experience.tables.records.TExperienceExtendBannerRecord
import org.apache.commons.lang3.StringUtils
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExperienceExtendBannerDao {
    fun listWithExtendBanner(
        dslContext: DSLContext
    ): Result<TExperienceExtendBannerRecord>? {
        val now = LocalDateTime.now()
        return with(TExperienceExtendBanner.T_EXPERIENCE_EXTEND_BANNER) {
            dslContext.selectFrom(this)
                .where(END_DATE.gt(now))
                .and(ONLINE.eq(true))
                .and(BANNER_URL.ne(StringUtils.EMPTY))
                .fetch()
        }
    }

    fun create(
        dslContext: DSLContext,
        experienceExtendBanner: ExperienceExtendBanner
    ): Int {
        val now = LocalDateTime.now()
        return with(TExperienceExtendBanner.T_EXPERIENCE_EXTEND_BANNER) {
            dslContext.insertInto(
                this,
                BANNER_URL,
                TYPE,
                LINK,
                ONLINE,
                END_DATE,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                experienceExtendBanner.bannerUrl,
                experienceExtendBanner.type,
                experienceExtendBanner.link,
                experienceExtendBanner.online,
                LocalDateTime.of(2100, 1, 1, 1, 1),
                now,
                now
            ).execute()
        }
    }

    fun update(
        dslContext: DSLContext,
        bannerId: Long,
        experienceExtendBanner: ExperienceExtendBanner
    ): Int {
        val now = LocalDateTime.now()
        return with(TExperienceExtendBanner.T_EXPERIENCE_EXTEND_BANNER) {
            dslContext.update(this)
                .let {
                    if (experienceExtendBanner.bannerUrl == null) it
                    else it.set(BANNER_URL, experienceExtendBanner.bannerUrl)
                }
                .let {
                    if (experienceExtendBanner.type == null) it
                    else it.set(TYPE, experienceExtendBanner.type)
                }
                .let {
                    if (experienceExtendBanner.link == null) it
                    else it.set(LINK, experienceExtendBanner.link)
                }
                .let {
                    if (experienceExtendBanner.online == null) it
                    else it.set(ONLINE, experienceExtendBanner.online)
                }
                .set(UPDATE_TIME, now)
                .where(ID.eq(bannerId))
                .execute()
        }
    }
}
