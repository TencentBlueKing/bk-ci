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

package com.tencent.devops.statistics.dao.project

import com.tencent.devops.model.project.tables.TProject
import com.tencent.devops.model.project.tables.records.TProjectRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.net.URLDecoder

@Repository
class ProjectDao {

    fun list(dslContext: DSLContext, projectIdList: Set<String>): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this).where(PROJECT_ID.`in`(projectIdList)).fetch()
        }
    }

    fun list(dslContext: DSLContext, limit: Int, offset: Int): Result<TProjectRecord> {
        return with(TProject.T_PROJECT) {
            dslContext.selectFrom(this).where(ENABLED.eq(true)).limit(limit).offset(offset).fetch()
        }
    }

    /**
     * 根据组织架构来查询name
     */
    fun listByGroup(
        dslContext: DSLContext,
        bgName: String?,
        deptName: String?,
        centerName: String?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (!bgName.isNullOrBlank()) {
                conditions.add(BG_NAME.like("%${URLDecoder.decode(bgName, "UTF-8")}%"))
            }
            if (!deptName.isNullOrBlank()) {
                conditions.add(DEPT_NAME.like("%${URLDecoder.decode(deptName, "UTF-8")}%"))
            }
            if (!centerName.isNullOrBlank()) {
                conditions.add(CENTER_NAME.like("%${URLDecoder.decode(centerName, "UTF-8")}%"))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    /**
     * 根据组织架构来查询name
     */
    fun listByGroupId(
        dslContext: DSLContext,
        bgId: Long?,
        deptId: Long?,
        centerId: Long?
    ): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (bgId != null) {
                conditions.add(BG_ID.eq(bgId))
            }
            if (deptId != null) {
                conditions.add(DEPT_ID.eq(deptId))
            }
            if (centerId != null) {
                conditions.add(CENTER_ID.eq(centerId))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    /**
     * 根据deptId+centerName来查询
     */
    fun listByOrganization(dslContext: DSLContext, deptId: Long?, centerName: String?): Result<TProjectRecord>? {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (deptId != null) {
                conditions.add(DEPT_ID.eq(deptId))
            }
            if (!centerName.isNullOrBlank()) {
                conditions.add(CENTER_NAME.like("%${URLDecoder.decode(centerName, "UTF-8")}%"))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    /**
     * 根据bgId+deptName+centerName来查询
     */
    fun listByOrganization(
        dslContext: DSLContext,
        bgId: Long?,
        deptName: String?,
        centerName: String?
    ): Result<TProjectRecord>? {
        with(TProject.T_PROJECT) {
            val conditions = mutableListOf<Condition>()
            if (bgId != null) {
                conditions.add(BG_ID.eq(bgId))
            }
            if (!deptName.isNullOrBlank()) {
                conditions.add(DEPT_NAME.like("%${URLDecoder.decode(deptName, "UTF-8")}%"))
            }
            if (!centerName.isNullOrBlank()) {
                conditions.add(CENTER_NAME.like("%${URLDecoder.decode(centerName, "UTF-8")}%"))
            }
            return dslContext.selectFrom(this).where(conditions).fetch()
        }
    }

    fun listByEnglishName(dslContext: DSLContext, englishNameList: List<String>): Result<TProjectRecord> {
        with(TProject.T_PROJECT) {
            return dslContext.selectFrom(this)
                .where(APPROVAL_STATUS.eq(2)).and(ENGLISH_NAME.`in`(englishNameList))
                .and(IS_OFFLINED.eq(false)).fetch()
        }
    }
}
