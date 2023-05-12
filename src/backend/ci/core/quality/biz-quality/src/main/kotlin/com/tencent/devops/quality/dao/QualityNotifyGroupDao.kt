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

package com.tencent.devops.quality.dao

import com.tencent.devops.model.quality.tables.TGroup
import com.tencent.devops.model.quality.tables.records.TGroupRecord
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import javax.ws.rs.NotFoundException

@Repository
@Suppress("ALL")
class QualityNotifyGroupDao {

    fun list(dslContext: DSLContext, projectId: String, offset: Int, limit: Int): Result<TGroupRecord> {
        with(TGroup.T_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATE_TIME.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun list(dslContext: DSLContext, groupIds: Collection<Long>): Result<TGroupRecord> {
        with(TGroup.T_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.`in`(groupIds))
                .fetch()
        }
    }

    fun listByIds(
        dslContext: DSLContext,
        projectId: String,
        groupIds: List<Long>,
        offset: Int,
        limit: Int
    ): Result<TGroupRecord> {
        with(TGroup.T_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(ID.`in`(groupIds))
                .orderBy(CREATE_TIME.desc())
                .offset(offset)
                .limit(limit)
                .fetch()
        }
    }

    fun listIds(
        dslContext: DSLContext,
        projectId: String
    ): Result<Record1<Long>> {
        with(TGroup.T_GROUP) {
            return dslContext.select(ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATE_TIME.desc())
                .fetch()
        }
    }

    fun count(dslContext: DSLContext, projectId: String): Long {
        with(TGroup.T_GROUP) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .fetchOne(0, Long::class.java)!!
        }
    }

    fun has(dslContext: DSLContext, projectId: String, name: String): Boolean {
        with(TGroup.T_GROUP) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NAME.eq(name))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    fun has(dslContext: DSLContext, projectId: String, name: String, excludeId: Long): Boolean {
        with(TGroup.T_GROUP) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NAME.eq(name))
                .and(ID.notEqual(excludeId))
                .fetchOne(0, Long::class.java) != 0L
        }
    }

    fun getOrNull(dslContext: DSLContext, groupId: Long): TGroupRecord? {
        with(TGroup.T_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.eq(groupId))
                .fetchOne()
        }
    }

    fun get(dslContext: DSLContext, groupId: Long): TGroupRecord {
        with(TGroup.T_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.eq(groupId))
                .fetchOne() ?: throw NotFoundException("GroupId: $groupId not found")
        }
    }

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        innerUsers: String,
        innerUsersCount: Int,
        outerUsers: String,
        outerUsersCount: Int,
        remark: String?,
        creator: String,
        updator: String
    ): Long {
        val now = LocalDateTime.now()
        with(TGroup.T_GROUP) {
            val record = dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                INNER_USERS,
                INNER_USERS_COUNT,
                OUTER_USERS,
                OUTER_USERS_COUNT,
                REMARK,
                CREATOR,
                UPDATOR,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                name,
                innerUsers,
                innerUsersCount,
                outerUsers,
                outerUsersCount,
                remark,
                creator,
                updator,
                now,
                now
            )
                .returning(ID)
                .fetchOne()
            return record!!.id
        }
    }

    fun update(
        dslContext: DSLContext,
        id: Long,
        name: String,
        innerUsers: String,
        innerUsersCount: Int,
        outerUsers: String,
        outerUsersCount: Int,
        remark: String?,
        updator: String
    ) {
        val now = LocalDateTime.now()
        with(TGroup.T_GROUP) {
            dslContext.update(this)
                .set(NAME, name)
                .set(INNER_USERS, innerUsers)
                .set(INNER_USERS_COUNT, innerUsersCount)
                .set(OUTER_USERS_COUNT, outerUsersCount)
                .set(OUTER_USERS, outerUsers)
                .set(REMARK, remark)
                .set(UPDATOR, updator)
                .set(UPDATE_TIME, now)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        id: Long
    ) {
        with(TGroup.T_GROUP) {
            dslContext.deleteFrom(this)
                .where(ID.eq(id))
                .execute()
        }
    }

    fun searchByNameLike(
        dslContext: DSLContext,
        projectId: String,
        offset: Int,
        limit: Int,
        name: String
    ): List<TGroupRecord>? {
        return with(TGroup.T_GROUP) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(NAME.like("%$name%")))
                .orderBy(CREATE_TIME.desc())
                .limit(offset, limit)
                .fetch()
        }
    }

    fun countByIdLike(
        dslContext: DSLContext,
        projectId: String,
        name: String
    ): Long {
        with(TGroup.T_GROUP) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(NAME.like("%$name%"))
                .fetchOne(0, kotlin.Long::class.java)!!
        }
    }
}
