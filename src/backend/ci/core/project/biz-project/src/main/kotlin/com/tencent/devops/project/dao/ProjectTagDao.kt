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
 *
 */

package com.tencent.devops.project.dao

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.model.project.Tables
import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class ProjectTagDao {

    fun updateProjectTags(dslContext: DSLContext, englishNames: List<String>, routerTag: String) {
        with(Tables.T_PROJECT) {
            dslContext.update(this).set(ROUTER_TAG, routerTag).where(ENGLISH_NAME.`in`(englishNames)).execute()
        }
    }

    fun updateExtSystemProjectTags(dslContext: DSLContext, englishName: String, otherRouterTag: String) {
        with(Tables.T_PROJECT) {
            dslContext.update(this).set(OTHER_ROUTER_TAGS, otherRouterTag)
                .where(ENGLISH_NAME.eq(englishName)).execute()
        }
    }

    fun updateChannelTags(dslContext: DSLContext, routerTag: String, channel: String) {
        with(Tables.T_PROJECT) {
            dslContext.update(this).set(ROUTER_TAG, routerTag).where(CHANNEL.eq(channel)).execute()
        }
    }

    fun updateOrgTags(dslContext: DSLContext, routerTag: String, bgId: Long?, centerId: Long?, deptId: Long?) {
        if (bgId == null && centerId == null && deptId == null) {
            throw ParamBlankException("Invalid project org")
        }

        with(Tables.T_PROJECT) {
            val conditions = mutableListOf<Condition>()

            if (bgId != null) {
                conditions.add(BG_ID.eq(bgId))
            }

            if (centerId != null) {
                conditions.add(CENTER_ID.eq(centerId))
            }

            if (deptId != null) {
                conditions.add(DEPT_ID.eq(deptId))
            }
            dslContext.update(this).set(ROUTER_TAG, routerTag).where(conditions).execute()
        }
    }

    fun listByChannel(dslContext: DSLContext, channel: String, limit: Int, offset: Int): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(CHANNEL.eq(channel)).limit(limit).offset(offset).fetch()
        }
    }

    fun getExtSystemRouterTag(dslContext: DSLContext, projectIds: List<String>): Result<TProjectRecord>? {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this).where(ENGLISH_NAME.`in`(projectIds)).fetch()
        }
    }
}
