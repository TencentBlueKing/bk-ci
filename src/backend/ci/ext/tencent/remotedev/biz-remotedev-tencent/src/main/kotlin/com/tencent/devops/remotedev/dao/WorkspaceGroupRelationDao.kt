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

import com.tencent.devops.model.remotedev.tables.TWorkspaceGroupRelation
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class WorkspaceGroupRelationDao {

    fun add(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        workspaceName: String,
        operator: String
    ): Int {
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(
                this,
                PROJECT_ID,
                GROUP_ID,
                WORKSPACE_NAME,
                OPERATOR,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                projectId,
                groupId,
                workspaceName,
                operator,
                now,
                now
            ).onDuplicateKeyIgnore().execute()
        }
    }

    fun batchAdd(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        workspaceNames: Collection<String>,
        operator: String
    ) {
        if (workspaceNames.isEmpty()) return
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            val now = LocalDateTime.now()
            dslContext.batch(
                workspaceNames.map { name ->
                    dslContext.insertInto(
                        this,
                        PROJECT_ID,
                        GROUP_ID,
                        WORKSPACE_NAME,
                        OPERATOR,
                        CREATE_TIME,
                        UPDATE_TIME
                    ).values(projectId, groupId, name, operator, now, now).onDuplicateKeyIgnore()
                }
            ).execute()
        }
    }

    fun remove(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        workspaceName: String
    ): Int {
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.deleteFrom(this)
                .where(
                    PROJECT_ID.eq(projectId)
                        .and(GROUP_ID.eq(groupId))
                        .and(WORKSPACE_NAME.eq(workspaceName))
                ).execute()
        }
    }

    fun removeByGroup(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long
    ): Int {
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.deleteFrom(this)
                .where(PROJECT_ID.eq(projectId).and(GROUP_ID.eq(groupId)))
                .execute()
        }
    }

    fun listWorkspaceNames(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long
    ): Set<String> {
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.select(WORKSPACE_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(GROUP_ID.eq(groupId)))
                .fetch(WORKSPACE_NAME, String::class.java)
                .toSet()
        }
    }

    fun listWorkspaceNamesWithPage(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long,
        offset: Int,
        limit: Int
    ): List<Pair<String, LocalDateTime>> {
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.select(WORKSPACE_NAME, CREATE_TIME)
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(GROUP_ID.eq(groupId)))
                .orderBy(CREATE_TIME.desc())
                .limit(limit)
                .offset(offset)
                .fetch()
                .map { record ->
                    Pair(
                        record.get(WORKSPACE_NAME, String::class.java),
                        record.get(CREATE_TIME, LocalDateTime::class.java)
                    )
                }
        }
    }

    fun countWorkspaces(
        dslContext: DSLContext,
        projectId: String,
        groupId: Long
    ): Long {
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.selectCount()
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(GROUP_ID.eq(groupId)))
                .fetchOne(0, Long::class.java) ?: 0L
        }
    }

    fun countByGroups(
        dslContext: DSLContext,
        projectId: String,
        groupIds: Collection<Long>
    ): Map<Long, Int> {
        if (groupIds.isEmpty()) return emptyMap()
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.select(GROUP_ID, DSL.count(ID))
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(GROUP_ID.`in`(groupIds)))
                .groupBy(GROUP_ID)
                .fetch()
                .associate { r -> (r.get(GROUP_ID) ?: 0L) to (r.get(DSL.count(ID))?.toInt() ?: 0) }
        }
    }

    fun listWorkspaceNamesByGroups(
        dslContext: DSLContext,
        projectId: String,
        groupIds: List<Long>
    ): List<String> {
        if (groupIds.isEmpty()) return emptyList()
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.select(WORKSPACE_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(GROUP_ID.`in`(groupIds)))
                .fetch(WORKSPACE_NAME, String::class.java)
        }
    }

    fun listGroupIdsByWorkspace(
        dslContext: DSLContext,
        projectId: String,
        workspaceName: String
    ): List<Long> {
        with(TWorkspaceGroupRelation.T_WORKSPACE_GROUP_RELATION) {
            return dslContext.select(GROUP_ID)
                .from(this)
                .where(PROJECT_ID.eq(projectId).and(WORKSPACE_NAME.eq(workspaceName)))
                .fetch(GROUP_ID, Long::class.java)
        }
    }
}


