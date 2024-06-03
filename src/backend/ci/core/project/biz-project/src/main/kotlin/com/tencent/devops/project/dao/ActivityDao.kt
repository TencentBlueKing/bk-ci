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

package com.tencent.devops.project.dao

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.project.tables.TActivity
import com.tencent.devops.model.project.tables.records.TActivityRecord
import com.tencent.devops.project.pojo.ActivityInfo
import com.tencent.devops.project.pojo.ActivityStatus
import com.tencent.devops.project.pojo.OPActivityUpdate
import com.tencent.devops.project.pojo.OPActivityVO
import com.tencent.devops.project.pojo.enums.ActivityType
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ActivityDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        activityInfo: ActivityInfo,
        type: ActivityType
    ) {
        with(TActivity.T_ACTIVITY) {
            dslContext.insertInto(
                this,
                TYPE,
                NAME,
                LINK,
                ENGLISH_NAME,
                CREATE_TIME,
                STATUS,
                CREATOR
            )
                .values(
                    type.name,
                    activityInfo.name,
                    activityInfo.link,
                    activityInfo.englishName,
                    LocalDateTime.now(),
                    ActivityStatus.ACTIVITY.name,
                    userId
                )
                .execute()
        }
    }

    fun list(
        dslContext: DSLContext,
        type: ActivityType,
        status: ActivityStatus
    ): Result<TActivityRecord> {
        with(TActivity.T_ACTIVITY) {
            return dslContext.selectFrom(this)
                .where(TYPE.eq(type.name))
                .orderBy(CREATE_TIME.desc())
                .skipCheck()
                .fetch()
        }
    }

    fun delete(dslContext: DSLContext, activityId: Long) {
        with(TActivity.T_ACTIVITY) {
            dslContext.deleteFrom(this).where(ID.eq(activityId)).execute()
        }
    }

    fun get(dslContext: DSLContext, activityId: Long): TActivityRecord? {
        with(TActivity.T_ACTIVITY) {
            return dslContext.selectFrom(this)
                .where(ID.eq(activityId))
                .fetchOne()
        }
    }

    fun listOPActivity(dslContext: DSLContext): List<OPActivityVO> {
        with(TActivity.T_ACTIVITY) {
            return dslContext.selectFrom(this)
                .fetch().map {
                    OPActivityVO(
                        id = it.id,
                        name = it.name,
                        englishName = it.englishName,
                        link = it.link,
                        type = it.type,
                        status = it.status,
                        creator = it.creator, createTime = DateTimeUtil.toDateTime(it.createTime)
                    )
                } ?: ArrayList<OPActivityVO>()
        }
    }

    fun upDate(dslContext: DSLContext, activityId: Long, opActivityUpdate: OPActivityUpdate): Boolean {
        with(TActivity.T_ACTIVITY) {
            val result = dslContext.update(this)
                .set(NAME, opActivityUpdate.name)
                .set(LINK, opActivityUpdate.link)
                .set(TYPE, opActivityUpdate.type)
                .set(ENGLISH_NAME, opActivityUpdate.englishName)
                .set(STATUS, opActivityUpdate.status)
                .where(ID.eq(activityId))
                .execute()
            return result > 0
        }
    }
}
