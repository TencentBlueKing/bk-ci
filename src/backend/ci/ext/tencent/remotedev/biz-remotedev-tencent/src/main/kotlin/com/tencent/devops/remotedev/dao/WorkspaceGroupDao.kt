/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 */

package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.remotedev.tables.TWorkspaceGroup
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceGroupRecord
import com.tencent.devops.remotedev.pojo.WorkspaceGroup
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WorkspaceGroupDao {

    fun create(
        dslContext: DSLContext,
        projectId: String,
        name: String,
        description: String?,
        userId: String
    ): Long {
        with(TWorkspaceGroup.T_WORKSPACE_GROUP) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                NAME,
                DESCRIPTION,
                CREATE_USER,
                UPDATE_USER,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                name,
                description ?: "",
                userId,
                userId,
                now,
                now
            ).returning(ID).fetchOne()!!.id
        }
    }

    fun update(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        name: String?,
        description: String?,
        userId: String
    ): Int {
        with(TWorkspaceGroup.T_WORKSPACE_GROUP) {
            val now = LocalDateTime.now()
            return dslContext.update(this)
                .set(UPDATE_USER, userId)
                .set(UPDATE_TIME, now)
                .let { s -> if (name != null) s.set(NAME, name) else s }
                .let { s -> if (description != null) s.set(DESCRIPTION, description ?: "") else s }
                .where(ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun delete(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long
    ): Int {
        with(TWorkspaceGroup.T_WORKSPACE_GROUP) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .execute()
        }
    }

    fun list(dslContext: DSLContext, projectId: String): List<WorkspaceGroup> {
        with(TWorkspaceGroup.T_WORKSPACE_GROUP) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .orderBy(CREATE_TIME.asc(), ID.asc())
                .fetch().map { convert(it) }
        }
    }

    fun get(dslContext: DSLContext, projectId: String, groupId: Long): WorkspaceGroup? {
        with(TWorkspaceGroup.T_WORKSPACE_GROUP) {
            return dslContext.selectFrom(this)
                .where(ID.eq(groupId).and(PROJECT_ID.eq(projectId)))
                .fetchOne()?.let { convert(it) }
        }
    }

    fun countByName(dslContext: DSLContext, projectId: String, name: String): Long {
        with(TWorkspaceGroup.T_WORKSPACE_GROUP) {
            return dslContext.selectCount().from(this)
                .where(PROJECT_ID.eq(projectId).and(NAME.eq(name)))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun convert(record: TWorkspaceGroupRecord): WorkspaceGroup {
        with(record) {
            return WorkspaceGroup(
                id = id,
                projectId = projectId,
                name = name,
                description = description,
                createTime = createTime.timestampmilli(),
                updateTime = updateTime.timestampmilli(),
                createUser = createUser,
                updateUser = updateUser,
                workspaceCount = 0
            )
        }
    }
}
