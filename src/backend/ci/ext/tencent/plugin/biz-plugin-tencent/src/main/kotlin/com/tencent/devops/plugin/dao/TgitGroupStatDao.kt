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

package com.tencent.devops.plugin.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.plugin.tables.TPluginTgitGroupStat
import com.tencent.devops.model.plugin.tables.records.TPluginTgitGroupStatRecord
import com.tencent.devops.plugin.api.pojo.GitGroupStatRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class TgitGroupStatDao {

    /**
     * 不存在则新增，否则更新
     */
    fun createOrUpdate(
        dslContext: DSLContext,
        group: String,
        gitGroupStatRequest: GitGroupStatRequest
    ) {
        with(TPluginTgitGroupStat.T_PLUGIN_TGIT_GROUP_STAT) {
            dslContext.insertInto(this,
                    ID,
                    GROUP,
                    STAT_DATE,
                    PROJECT_COUNT,
                    PROJECT_COUNT_OPEN,
                    PROJECT_INCRE,
                    PROJECT_INCRE_OPEN,
                    COMMIT_COUNT,
                    COMMIT_COUNT_OPEN,
                    COMMIT_INCRE,
                    COMMIT_INCRE_OPEN,
                    USER_COUNT,
                    USER_COUNT_OPEN,
                    USER_INCRE,
                    USER_INCRE_OPEN
            ).values(
                    UUIDUtil.generate(),
                    group,
                    LocalDate.parse(gitGroupStatRequest.statDate, DateTimeFormatter.ISO_DATE),
                    gitGroupStatRequest.projectCount,
                    gitGroupStatRequest.projectCountOpen,
                    gitGroupStatRequest.projectIncre,
                    gitGroupStatRequest.projectIncreOpen,
                    gitGroupStatRequest.commitCount,
                    gitGroupStatRequest.commitCountOpen,
                    gitGroupStatRequest.commitIncre,
                    gitGroupStatRequest.commitIncreOpen,
                    gitGroupStatRequest.userCount,
                    gitGroupStatRequest.userCountOpen,
                    gitGroupStatRequest.userIncre,
                    gitGroupStatRequest.userIncreOpen
            )
                    .onDuplicateKeyUpdate()
                    .set(PROJECT_COUNT, gitGroupStatRequest.projectCount)
                    .set(PROJECT_COUNT_OPEN, gitGroupStatRequest.projectCountOpen)
                    .set(PROJECT_INCRE, gitGroupStatRequest.projectIncre)
                    .set(PROJECT_INCRE_OPEN, gitGroupStatRequest.projectIncreOpen)
                    .set(COMMIT_COUNT, gitGroupStatRequest.commitCount)
                    .set(COMMIT_COUNT_OPEN, gitGroupStatRequest.commitCountOpen)
                    .set(COMMIT_INCRE, gitGroupStatRequest.commitIncre)
                    .set(COMMIT_INCRE_OPEN, gitGroupStatRequest.commitIncreOpen)
                    .set(USER_COUNT, gitGroupStatRequest.userCount)
                    .set(USER_COUNT_OPEN, gitGroupStatRequest.userCountOpen)
                    .set(USER_INCRE, gitGroupStatRequest.userIncre)
                    .set(USER_INCRE_OPEN, gitGroupStatRequest.userIncreOpen)
                    .set(UPDATE_TIME, java.time.LocalDateTime.now())
                    .execute()
        }
    }

    fun getLatestRecord(dslContext: DSLContext, group: String, stat_date: String): TPluginTgitGroupStatRecord? {
        with(TPluginTgitGroupStat.T_PLUGIN_TGIT_GROUP_STAT) {
            return dslContext.selectFrom(this)
                    .where(GROUP.eq(group))
                    .and(STAT_DATE.ne(LocalDate.parse(stat_date)))
                    .orderBy(CREATE_TIME.desc())
                    .limit(1)
                    .fetchOne()
        }
    }
}
