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

package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.model.remotedev.tables.TRemoteDevSettings
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceDetail
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceDetailRecord
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceRecordInf
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithDetail
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import java.sql.Timestamp
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.DatePart
import org.jooq.Field
import org.jooq.Record
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.RecordMapper
import org.jooq.Result
import org.jooq.SelectConditionStep
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class WorkspaceDao {

    fun createWorkspace(
        workspace: Workspace,
        workspaceStatus: WorkspaceStatus,
        bgName: String?,
        deptName: String?,
        centerName: String?,
        groupName: String?,
        dslContext: DSLContext
    ): Long {
        if (workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
                dslContext.insertInto(
                    this,
                    WORKSPACE_NAME,
                    WIN_CONFIG_ID
                ).values(
                    workspace.workspaceName,
                    workspace.winConfigId
                ).execute()
            }
        }

        return with(TWorkspace.T_WORKSPACE) {
            dslContext.insertInto(
                /* into = */
                this,
                /* ...fields = */
                PROJECT_ID,
                NAME,
                TEMPLATE_ID,
                URL,
                BRANCH,
                YAML_PATH,
                IMAGE_PATH,
                WORK_PATH,
                WORKSPACE_FOLDER,
                HOST_NAME,
                GPU,
                CPU,
                MEMORY,
                DISK,
                STATUS,
                LAST_STATUS_UPDATE_TIME,
                YAML,
                DOCKERFILE,
                CREATOR,
                CREATOR_BG_NAME,
                CREATOR_DEPT_NAME,
                CREATOR_CENTER_NAME,
                CREATOR_GROUP_NAME,
                WORKSPACE_MOUNT_TYPE,
                SYSTEM_TYPE,
                OWNER_TYPE
            )
                .values(
                    workspace.projectId,
                    workspace.workspaceName,
                    workspace.wsTemplateId,
                    workspace.repositoryUrl,
                    workspace.branch,
                    workspace.devFilePath,
                    "",
                    workspace.workPath,
                    workspace.workspaceFolder,
                    workspace.hostName,
                    workspace.gpu,
                    workspace.cpu,
                    workspace.memory,
                    workspace.disk,
                    workspaceStatus.ordinal,
                    LocalDateTime.now(),
                    workspace.yaml,
                    "",
                    workspace.createUserId,
                    bgName ?: "",
                    deptName ?: "",
                    centerName ?: "",
                    groupName ?: "",
                    workspace.workspaceMountType.name,
                    workspace.workspaceSystemType.name,
                    workspace.ownerType.name
                )
                .returning(ID)
                .fetchOne()!!.id
        }
    }

    fun limitFetchWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit,
        userId: String? = null,
        workspaceName: String? = null
    ): List<WorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(userId, workspaceName)
            val query = dslContext.selectFrom(this)

            if (condition.isNotEmpty()) {
                query.where(condition)
            }
            return query.orderBy(CREATE_TIME.desc(), ID.desc())
                .limit(limit.limit).offset(limit.offset)
                .fetch(workspaceMapper)
        }
    }

    /**
     * 获得用户所拥有工作空间列表（计数）
     */
    fun countUserWorkspace(
        dslContext: DSLContext,
        userId: String? = null,
        projectId: String? = null,
        unionShared: Boolean = true,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL,
        status: Set<WorkspaceStatus>? = null,
        systemType: WorkspaceSystemType? = null
    ): Long {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectCount().from(this)
                .let {
                    when (ownerType) {
                        WorkspaceOwnerType.PERSONAL -> it.where(CREATOR.eq(userId!!))
                        WorkspaceOwnerType.PROJECT -> it.where(PROJECT_ID.eq(projectId!!))
                    }.and(OWNER_TYPE.eq(ownerType.name))
                }
                .let {
                    if (status.isNullOrEmpty()) {
                        it.and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                    } else {
                        it.and(STATUS.`in`(status.map { s -> s.ordinal }))
                    }
                }
                .let { if (systemType != null) it.and(SYSTEM_TYPE.eq(systemType.name)) else it }
                .let {
                    if (unionShared && userId != null) {
                        it.unionAll(
                            unionSelect(shared, userId, status, systemType)
                        )
                    } else {
                        it
                    }
                }
                .fetch(0, Long::class.java).sum()
        }
    }

    fun countProjectWorkspace(
        dslContext: DSLContext,
        projectId: String?,
        workspaceName: String?,
        systemType: WorkspaceSystemType?,
        queryType: QueryType? = QueryType.WEB,
        ips: List<String>?
    ): Long {
        return dslContext.fetchCount(
            genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                projectId = projectId,
                workspaceName = workspaceName,
                systemType = systemType,
                queryType = queryType,
                ips = ips
            )
        ).toLong()
    }

    private fun TWorkspace.unionSelect(
        shared: TWorkspaceShared,
        creator: String,
        status: Set<WorkspaceStatus>?,
        systemType: WorkspaceSystemType?
    ) = DSL.selectCount().from(this).where(
        NAME.`in`(
            DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                shared.SHARED_USER.eq(
                    creator
                )
            )
        )
    ).let { i ->
        if (status.isNullOrEmpty()) {
            i.and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
        } else {
            i.and(STATUS.`in`(status.map { s -> s.ordinal }))
        }
    }.let { i -> if (systemType != null) i.and(SYSTEM_TYPE.eq(systemType.name)) else i }

    /**
     * 获得用户所拥有工作空间列表
     */
    fun limitFetchUserWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit,
        userId: String? = null,
        projectId: String? = null,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): List<WorkspaceRecord>? {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this)
                .let {
                    when (ownerType) {
                        WorkspaceOwnerType.PERSONAL -> it.where(CREATOR.eq(userId!!))
                        WorkspaceOwnerType.PROJECT -> it.where(PROJECT_ID.eq(projectId!!))
                    }.and(OWNER_TYPE.eq(ownerType.name))
                }
                .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .unionAll(
                    DSL.selectFrom(this).where(
                        NAME.`in`(
                            DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                                shared.SHARED_USER.eq(
                                    userId
                                )
                            )
                        )
                    ).and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                ).orderBy(CREATE_TIME.desc(), ID.desc())
                .limit(limit.limit).offset(limit.offset)
                .fetch(workspaceMapper)
        }
    }

    /**
     * 获取项目下工作空间列表
     */
    fun limitFetchProjectWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit,
        projectId: String?,
        workspaceName: String?,
        systemType: WorkspaceSystemType?,
        queryType: QueryType? = QueryType.WEB,
        ips: List<String>?
    ): List<WorkspaceRecordInf>? {
        with(TWorkspace.T_WORKSPACE) {
            return if (ips.isNullOrEmpty()) {
                (genFetchProjectWorkspaceCond(
                    dslContext = dslContext,
                    projectId = projectId,
                    workspaceName = workspaceName,
                    systemType = systemType,
                    queryType = queryType,
                    ips = ips
                ) as SelectConditionStep<TWorkspaceRecord>).orderBy(CREATE_TIME.desc(), ID.desc())
                    .limit(limit.limit).offset(limit.offset)
                    .fetch(workspaceMapper)
            } else {
                genFetchProjectWorkspaceCond(
                    dslContext = dslContext,
                    projectId = projectId,
                    workspaceName = workspaceName,
                    systemType = systemType,
                    queryType = queryType,
                    ips = ips
                ).orderBy(CREATE_TIME.desc(), ID.desc())
                    .limit(limit.limit).offset(limit.offset)
                    .fetch(workspaceWithDetailMapper)
            }
        }
    }

    private fun genFetchProjectWorkspaceCond(
        dslContext: DSLContext,
        projectId: String?,
        workspaceName: String?,
        systemType: WorkspaceSystemType?,
        queryType: QueryType? = QueryType.WEB,
        ips: List<String>?
    ): SelectConditionStep<*> {
        val conditions = mutableListOf<Condition>()
        with(TWorkspace.T_WORKSPACE) {
            projectId?.let {
                if (queryType == QueryType.OP) {
                    conditions.add(PROJECT_ID.like("%$it%"))
                } else {
                    conditions.add(PROJECT_ID.eq(it))
                }
            }

            workspaceName?.let {
                if (queryType == QueryType.OP) {
                    conditions.add(NAME.like("%$it%"))
                } else {
                    conditions.add(NAME.eq(it))
                }
            }

            conditions.add(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))

            if (systemType != null) {
                conditions.add(SYSTEM_TYPE.eq(systemType.name))
            }

            if (queryType == QueryType.WEB) {
                conditions.add(OWNER_TYPE.eq(WorkspaceOwnerType.PROJECT.name))
            }
        }

        // 没有 ip 就不需要连表查询
        if (ips.isNullOrEmpty()) {
            return dslContext.selectFrom(TWorkspace.T_WORKSPACE).where(conditions)
        }

        conditions.add(0, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceDetail.T_WORKSPACE_DETAIL.WORKSPACE_NAME))

        var ipsCond = JooqUtils.jsonExtract(
            t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
            t2 = "\$.hostIP",
            lower = false,
            removeDoubleQuotes = true
        ).like("%${ips.first()}%") as Condition
        ips.drop(1).forEach { ip ->
            ipsCond = ipsCond.or(
                JooqUtils.jsonExtract(
                    t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                    t2 = "\$.hostIP",
                    lower = false,
                    removeDoubleQuotes = true
                ).like("%$ip%")
            )
        }
        conditions.add(ipsCond)

        val fields = TWorkspace.T_WORKSPACE.fields().toMutableList()
        fields.add(TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL)
        return dslContext.select(fields).from(TWorkspace.T_WORKSPACE, TWorkspaceDetail.T_WORKSPACE_DETAIL)
            .where(conditions)
    }

    /**
     * 获得拥有该工作空间的用户
     */
    fun fetchWorkspaceUser(
        dslContext: DSLContext,
        workspaceName: String
    ): List<String> {
        val shared = TWorkspaceShared.T_WORKSPACE_SHARED
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.select(CREATOR).from(this)
                .where(NAME.eq(workspaceName)).unionAll(
                    DSL.select(shared.SHARED_USER).from(shared).where(
                        shared.WORKSPACE_NAME.eq(workspaceName)
                    )
                ).fetch(0, String::class.java)
        }
    }

    fun fetchCreators(
        dslContext: DSLContext,
        status: WorkspaceStatus,
        systemType: WorkspaceSystemType = WorkspaceSystemType.WINDOWS_GPU,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): List<Record2<String, String>> {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.select(CREATOR, NAME).from(this)
                .where(STATUS.eq(status.ordinal))
                .and(SYSTEM_TYPE.eq(systemType.name))
                .and(OWNER_TYPE.eq(ownerType.name))
                .fetch()
        }
    }

    fun fetchAnyWorkspace(
        dslContext: DSLContext,
        userId: String? = null,
        workspaceName: String? = null,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null
    ): WorkspaceRecord? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(
                userId = userId,
                workspaceName = workspaceName,
                status = status,
                mountType = mountType
            )

            if (condition.isEmpty()) {
                return null
            }

            return dslContext.selectFrom(this)
                .where(condition)
                .fetchAny(workspaceMapper)
        }
    }

    fun fetchWorkspace(
        dslContext: DSLContext,
        userId: String? = null,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null,
        projectId: String? = null
    ): List<WorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mixCondition(
                userId = userId,
                status = status,
                mountType = mountType,
                projectId = projectId
            )

            if (condition.isEmpty()) {
                return null
            }

            return dslContext.selectFrom(this)
                .where(condition)
                .fetch(workspaceMapper)
        }
    }

    fun getWorkspaceProject(
        dslContext: DSLContext,
        mountType: WorkspaceMountType? = null
    ): Result<Record1<String>>? {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectDistinct(PROJECT_ID).from(this)
                .where(PROJECT_ID.ne(""))
                .let { i ->
                    if (mountType != null) {
                        i.and(WORKSPACE_MOUNT_TYPE.eq(mountType.name))
                    } else i
                }
                .fetch()
        }
    }

    /**
     * 获取没有使用时长的工作空间
     */
    fun fetchNotUsageTimeWinWorkspace(
        dslContext: DSLContext,
        status: WorkspaceStatus,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): List<WorkspaceRecord>? {
        val setting = TRemoteDevSettings.T_REMOTE_DEV_SETTINGS
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this).where(
                CREATOR.`in`(
                    DSL.select(setting.USER_ID).from(setting).where(setting.WIN_USAGE_REMAINING_TIME.le(0))
                )
            ).and(STATUS.eq(status.ordinal)).and(WORKSPACE_MOUNT_TYPE.eq(WorkspaceMountType.START.name))
                .and(OWNER_TYPE.eq(ownerType.name))
                .fetch(workspaceMapper)
        }
    }

    fun fetchSharedWorkspace(
        dslContext: DSLContext,
        workspaceName: String? = null
    ): Result<out Record>? {
        val t1 = TWorkspace.T_WORKSPACE.`as`("t1")
        val t2 = TWorkspaceShared.T_WORKSPACE_SHARED.`as`("t2")
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.STATUS.ne(WorkspaceStatus.DELETED.ordinal))
        conditions.add(t2.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.VIEWER.name))
        if (!workspaceName.isNullOrBlank()) {
            conditions.add(t2.WORKSPACE_NAME.like("%$workspaceName%"))
        }
        return dslContext.select(t2.ID, t2.WORKSPACE_NAME, t2.OPERATOR, t2.SHARED_USER, t2.ASSIGN_TYPE, t2.RESOURCE_ID)
            .from(t1).innerJoin(t2).on(t1.NAME.eq(t2.WORKSPACE_NAME))
            .where(conditions)
            .fetch()
    }

    fun fetchWorkspaceWithOwner(
        dslContext: DSLContext,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null,
        projectId: String? = null,
        ip: String? = null,
        assignType: WorkspaceShared.AssignType? = null
    ): Result<out Record>? {
        val t1 = TWorkspace.T_WORKSPACE.`as`("t1")
        val t2 = TWorkspaceShared.T_WORKSPACE_SHARED.`as`("t2")
        val t3 = TWorkspaceWindows.T_WORKSPACE_WINDOWS.`as`("t3")
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
        status?.let {
            conditions.add(t1.STATUS.eq(it.ordinal))
        }
        mountType?.let {
            conditions.add(t1.WORKSPACE_MOUNT_TYPE.eq(mountType.name))
        }
        projectId?.let {
            conditions.add(t1.PROJECT_ID.eq(projectId))
        }

        ip?.let {
            conditions.add(
                t1.NAME.`in`(
                    DSL.selectDistinct(t3.WORKSPACE_NAME).from(t3).where(
                        t3.HOST_IP.endsWith(".$ip")
                    )
                )
            )
        }

        return dslContext.select(t1.NAME, t1.PROJECT_ID, t1.CREATOR, t1.STATUS, t1.CREATE_TIME, t2.SHARED_USER)
            .from(t1).leftOuterJoin(t2).on(t1.NAME.eq(t2.WORKSPACE_NAME))
            .where(conditions)
            .let {
                if (assignType != null) {
                    it.and(t2.ASSIGN_TYPE.eq(assignType.name))
                } else it
            }
            .and(t1.OWNER_TYPE.eq(WorkspaceOwnerType.PROJECT.name))
            .unionAll(
                dslContext.select(t1.NAME, t1.PROJECT_ID, t1.CREATOR, t1.STATUS, t1.CREATE_TIME, t1.CREATOR.`as`("SHARED_USER"))
                    .from(t1)
                    .where(conditions)
                    .and(t1.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name))
            )
            .fetch()
    }

    private fun mixCondition(
        userId: String? = null,
        workspaceName: String? = null,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null,
        projectId: String? = null
    ): List<Condition> {
        val condition = mutableListOf<Condition>()
        with(TWorkspace.T_WORKSPACE) {
            if (!userId.isNullOrBlank()) {
                condition.add(CREATOR.eq(userId))
            }
            if (!workspaceName.isNullOrBlank()) {
                condition.add(NAME.eq(workspaceName))
            }
            if (status != null) {
                condition.add(STATUS.eq(status.ordinal))
            }
            if (mountType != null) {
                condition.add(WORKSPACE_MOUNT_TYPE.eq(mountType.name))
            }
            if (projectId != null) {
                condition.add(PROJECT_ID.eq(projectId))
            }
        }
        return condition
    }

    fun updateWorkspaceStatus(
        dslContext: DSLContext,
        workspaceName: String,
        status: WorkspaceStatus,
        hostName: String? = ""
    ) {
        with(TWorkspace.T_WORKSPACE) {
            if (hostName.isNullOrBlank()) {
                dslContext.update(this)
                    .set(STATUS, status.ordinal)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(LAST_STATUS_UPDATE_TIME, LocalDateTime.now())
                    .where(NAME.eq(workspaceName))
                    .execute()
            } else {
                dslContext.update(this)
                    .set(STATUS, status.ordinal)
                    .set(HOST_NAME, hostName)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(LAST_STATUS_UPDATE_TIME, LocalDateTime.now())
                    .where(NAME.eq(workspaceName))
                    .execute()
            }
        }
    }

    fun updateWorkspaceDisplayName(
        dslContext: DSLContext,
        workspaceName: String,
        displayName: String? = ""
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(DISPLAY_NAME, displayName)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(NAME.eq(workspaceName))
                .execute()
        }
    }

    fun updateWorkspaceCreatorInfo(
        dslContext: DSLContext,
        workspaceName: String,
        creator: String,
        bgName: String,
        deptName: String,
        centerName: String,
        groupName: String
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(CREATOR_BG_NAME, bgName)
                .set(CREATOR_DEPT_NAME, deptName)
                .set(CREATOR_CENTER_NAME, centerName)
                .set(CREATOR_GROUP_NAME, groupName)
                .where(NAME.eq(workspaceName).and(CREATOR.eq(creator)))
                .execute()
        }
    }

    fun updateWorkspaceUsageTime(
        workspaceName: String,
        usageTime: Int,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(USAGE_TIME, USAGE_TIME + usageTime)
                .where(NAME.eq(workspaceName))
                .execute()
        }
    }

    fun updateWorkspaceSleepingTime(
        workspaceName: String,
        sleepTime: Int,
        dslContext: DSLContext
    ) {
        with(TWorkspace.T_WORKSPACE) {
            dslContext.update(this)
                .set(SLEEPING_TIME, SLEEPING_TIME + sleepTime)
                .where(NAME.eq(workspaceName))
                .execute()
        }
    }

    fun deleteWorkspace(
        workspaceName: String,
        dslContext: DSLContext
    ): Int {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.delete(this)
                .where(NAME.eq(workspaceName))
                .limit(1)
                .execute()
        }
    }

    // 获取已休眠(status:3)且过期14天的工作空间
    fun getTimeOutInactivityWorkspace(
        dslContext: DSLContext,
        timeOutDays: Int,
        systemType: WorkspaceSystemType?,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL
    ): List<WorkspaceRecord> {
        with(TWorkspace.T_WORKSPACE) {
            val condition = mutableListOf<Condition>()
            condition.add(
                timestampDiff(DatePart.DAY, LAST_STATUS_UPDATE_TIME.cast(java.sql.Timestamp::class.java))
                    .greaterOrEqual(timeOutDays)
            )

            condition.add(STATUS.eq(WorkspaceStatus.SLEEP.ordinal))
            condition.add(OWNER_TYPE.eq(ownerType.name))

            if (systemType != null) {
                condition.add(SYSTEM_TYPE.eq(systemType.name))
            }
            return dslContext.selectFrom(this)
                .where(condition)
                .limit(1000)
                .fetch(workspaceMapper)
        }
    }

    fun updatePreCiAgentId(
        dslContext: DSLContext,
        agentId: String,
        workspaceName: String
    ): Boolean {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.update(this)
                .set(PRECI_AGENT_ID, agentId)
                .where(NAME.eq(workspaceName))
                .execute() == 1
        }
    }

    fun timestampDiff(part: DatePart, t1: Field<Timestamp>): Field<Int> {
        return DSL.field(
            "timestampdiff({0}, {1}, NOW())",
            Int::class.java, DSL.keyword(part.toSQL()), t1
        )
    }

    // 持久化存储workspace detail数据
    fun saveOrUpdateWorkspaceDetail(
        dslContext: DSLContext,
        workspaceName: String,
        detail: String
    ) {
        with(TWorkspaceDetail.T_WORKSPACE_DETAIL) {
            dslContext.insertInto(
                this,
                WORKSPACE_NAME,
                DETAIL,
                CREATE_TIME
            ).values(
                workspaceName,
                detail,
                LocalDateTime.now()
            ).onDuplicateKeyUpdate()
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(DETAIL, detail)
                .execute()
        }
    }

    // 获取workspace detail
    fun getWorkspaceDetail(
        dslContext: DSLContext,
        workspaceName: String
    ): TWorkspaceDetailRecord? {
        return with(TWorkspaceDetail.T_WORKSPACE_DETAIL) {
            dslContext.selectFrom(this)
                .where(WORKSPACE_NAME.eq(workspaceName))
                .fetchAny()
        }
    }

    class TWorkspaceRecordJooqMapper : RecordMapper<TWorkspaceRecord, WorkspaceRecord> {
        override fun map(record: TWorkspaceRecord?): WorkspaceRecord? {
            return record?.run {
                WorkspaceRecord(
                    workspaceId = id,
                    projectId = projectId,
                    workspaceName = name,
                    displayName = displayName,
                    templateId = templateId,
                    repositoryUrl = url,
                    branch = branch,
                    yaml = yaml,
                    devFilePath = yamlPath,
                    dockerFile = dockerfile,
                    imagePath = imagePath,
                    workPath = workPath,
                    workspaceFolder = workspaceFolder,
                    hostName = hostName,
                    gpu = gpu,
                    cpu = cpu,
                    memory = memory,
                    usageTime = usageTime,
                    sleepingTime = sleepingTime,
                    disk = disk,
                    createUserId = creator,
                    creatorBgName = creatorBgName,
                    creatorDeptName = creatorDeptName,
                    creatorCenterName = creatorCenterName,
                    creatorGroupName = creatorGroupName,
                    status = WorkspaceStatus.values()[status],
                    createTime = createTime,
                    updateTime = updateTime,
                    lastStatusUpdateTime = lastStatusUpdateTime,
                    preciAgentId = preciAgentId,
                    workspaceMountType = WorkspaceMountType.valueOf(workspaceMountType),
                    workspaceSystemType = WorkspaceSystemType.valueOf(systemType),
                    ownerType = WorkspaceOwnerType.valueOf(ownerType)
                )
            }
        }
    }

    class TWorkspaceRecordWithDetailJooqMapper : RecordMapper<Record, WorkspaceRecordWithDetail> {
        override fun map(record: Record?): WorkspaceRecordWithDetail? {

            if (record == null) {
                return null
            }
            return WorkspaceRecordWithDetail(
                workspaceId = record["ID"] as Long,
                projectId = record["PROJECT_ID"] as String,
                workspaceName = record["NAME"] as String,
                displayName = record["DISPLAY_NAME"] as String,
                templateId = record["TEMPLATE_ID"] as Int?,
                repositoryUrl = record["URL"] as String?,
                branch = record["BRANCH"] as String?,
                yaml = record["YAML"] as String?,
                devFilePath = record["YAML_PATH"] as String?,
                dockerFile = record["DOCKERFILE"] as String,
                imagePath = record["IMAGE_PATH"] as String,
                workPath = record["WORK_PATH"] as String?,
                workspaceFolder = record["WORKSPACE_FOLDER"] as String?,
                hostName = record["HOST_NAME"] as String,
                gpu = record["GPU"] as Int,
                cpu = record["CPU"] as Int,
                memory = record["MEMORY"] as Int,
                usageTime = record["USAGE_TIME"] as Int,
                sleepingTime = record["SLEEPING_TIME"] as Int,
                disk = record["DISK"] as Int,
                createUserId = record["CREATOR"] as String,
                creatorBgName = record["CREATOR_BG_NAME"] as String,
                creatorDeptName = record["CREATOR_DEPT_NAME"] as String,
                creatorCenterName = record["CREATOR_CENTER_NAME"] as String,
                creatorGroupName = record["CREATOR_GROUP_NAME"] as String,
                status = WorkspaceStatus.values()[record["STATUS"] as Int],
                createTime = record["CREATE_TIME"] as LocalDateTime,
                updateTime = record["UPDATE_TIME"] as LocalDateTime,
                lastStatusUpdateTime = record["LAST_STATUS_UPDATE_TIME"] as LocalDateTime?,
                preciAgentId = record["PRECI_AGENT_ID"] as String?,
                workspaceMountType = WorkspaceMountType.valueOf(record["WORKSPACE_MOUNT_TYPE"] as String),
                workspaceSystemType = WorkspaceSystemType.valueOf(record["SYSTEM_TYPE"] as String),
                ownerType = WorkspaceOwnerType.valueOf(record["OWNER_TYPE"] as String),
                workSpaceDetail = record["DETAIL"] as String
            )
        }
    }

    companion object {
        val workspaceMapper = TWorkspaceRecordJooqMapper()
        val workspaceWithDetailMapper = TWorkspaceRecordWithDetailJooqMapper()
    }
}
