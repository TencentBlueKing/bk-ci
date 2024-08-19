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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceDetail
import com.tencent.devops.model.remotedev.tables.TWorkspaceLabels
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceDetailRecord
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOrganization
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.project.WorkspaceProperty
import java.time.LocalDateTime
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record1
import org.jooq.RecordMapper
import org.jooq.Result
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Suppress("ALL")
@Repository
class WorkspaceDao {

    fun createWorkspace(
        workspace: Workspace,
        workspaceStatus: WorkspaceStatus,
        organization: WorkspaceOrganization,
        dslContext: DSLContext
    ): Long {
        if (workspace.workspaceSystemType == WorkspaceSystemType.WINDOWS_GPU) {
            with(TWorkspaceWindows.T_WORKSPACE_WINDOWS) {
                dslContext.insertInto(
                    this,
                    WORKSPACE_NAME,
                    WIN_CONFIG_ID,
                    IMAGE_ID,
                    ZONE_ID
                ).values(
                    workspace.workspaceName,
                    workspace.winConfigId,
                    workspace.imageId,
                    workspace.zoneId
                ).onDuplicateKeyIgnore().execute()
            }
        }

        return with(TWorkspace.T_WORKSPACE) {
            dslContext.insertInto(
                /* into = */
                this,
                /* ...fields = */
                PROJECT_ID,
                NAME,
                HOST_NAME,
                STATUS,
                LAST_STATUS_UPDATE_TIME,
                DOCKERFILE,
                CREATOR,
                CREATOR_BG_NAME,
                CREATOR_DEPT_NAME,
                CREATOR_CENTER_NAME,
                CREATOR_GROUP_NAME,
                WORKSPACE_MOUNT_TYPE,
                SYSTEM_TYPE,
                OWNER_TYPE,
                PROJECT_NAME,
                BUSINESS_LINE_NAME,
                BAK_NAME
            )
                .values(
                    workspace.projectId,
                    workspace.workspaceName,
                    workspace.hostName,
                    workspaceStatus.ordinal,
                    LocalDateTime.now(),
                    "",
                    workspace.createUserId,
                    organization.bgName ?: "",
                    organization.deptName ?: "",
                    organization.centerName ?: "",
                    organization.groupName ?: "",
                    workspace.workspaceMountType.name,
                    workspace.workspaceSystemType.name,
                    workspace.ownerType.name,
                    organization.projectName,
                    organization.businessLineName ?: "",
                    workspace.bakWorkspaceName
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

    /**
     * 获取正在使用的 workspace
     */
    fun fetchUserWorkspaceName(
        dslContext: DSLContext,
        projectId: String,
        ownerType: WorkspaceOwnerType
    ): Set<String> {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(OWNER_TYPE.eq(ownerType.name))
                .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .fetch().map { it.name }.toSet()
        }
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
        limit: SQLLimit? = null,
        userId: String? = null,
        projectId: String? = null,
        ownerType: WorkspaceOwnerType = WorkspaceOwnerType.PERSONAL,
        deleted: Boolean = false
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
                .let { if (!deleted) it.and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal)) else it }
                .unionAll(
                    DSL.selectFrom(this).where(
                        NAME.`in`(
                            DSL.select(shared.WORKSPACE_NAME).from(shared).where(
                                shared.SHARED_USER.eq(
                                    userId
                                )
                            )
                        )
                    ).let { if (!deleted) it.and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal)) else it }
                ).orderBy(CREATE_TIME.desc(), ID.desc())
                .let {
                    if (limit != null) it.limit(limit.limit).offset(limit.offset) else it
                }
                .skipCheck()
                .fetch(workspaceMapper)
        }
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

    fun fetchErrorWorkspace(
        dslContext: DSLContext
    ): List<WorkspaceRecord>? {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectFrom(this)
                .where(STATUS.`in`(WorkspaceStatus.Types.ERROR.status().map { s -> s.ordinal }))
                .fetch(workspaceMapper)
        }
    }

    fun getWorkspaceProject(
        dslContext: DSLContext,
        mountType: WorkspaceMountType? = null,
        projectId: String? = null
    ): Result<Record1<String>>? {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.selectDistinct(PROJECT_ID).from(this)
                .where(PROJECT_ID.ne(""))
                .let { i ->
                    if (mountType != null) {
                        i.and(WORKSPACE_MOUNT_TYPE.eq(mountType.name))
                    } else {
                        i
                    }
                }
                .let { i ->
                    if (projectId != null) {
                        i.and(PROJECT_ID.eq(projectId))
                    } else {
                        i
                    }
                }
                .fetch()
        }
    }

    /**
     * 拿项目下所有的owner
     */
    fun fetchWorkspaceOwnerInProject(
        dslContext: DSLContext,
        projectId: String
    ): Set<String> {
        val t1 = TWorkspace.T_WORKSPACE.`as`("t1")
        val t2 = TWorkspaceShared.T_WORKSPACE_SHARED.`as`("t2")
        val conditions = mutableListOf<Condition>()
        conditions.add(t1.PROJECT_ID.eq(projectId))
        conditions.add(
            t1.STATUS.notIn(
                listOf(
                    WorkspaceStatus.DELETED.ordinal,
                    WorkspaceStatus.PREPARING.ordinal,
                    WorkspaceStatus.DELIVERING_FAILED.ordinal
                )
            )
        )
        return dslContext.selectDistinct(t2.SHARED_USER).from(t1)
            .leftJoin(t2).on(t1.NAME.eq(t2.WORKSPACE_NAME))
            .where(conditions)
            .and(t2.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name))
            .and(t1.OWNER_TYPE.eq(WorkspaceOwnerType.PROJECT.name))
            .unionAll(
                dslContext.selectDistinct(
                    t1.CREATOR.`as`("SHARED_USER")
                )
                    .from(t1)
                    .where(conditions)
                    .and(t1.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name))
            )
            .fetch().map { it.value1() }.toSet()
    }

    private fun mixCondition(
        userId: String? = null,
        workspaceName: String? = null,
        status: WorkspaceStatus? = null,
        mountType: WorkspaceMountType? = null,
        projectId: String? = null,
        systemType: WorkspaceSystemType? = null,
        ownerType: WorkspaceOwnerType? = null
    ): MutableList<Condition> {
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
            if (systemType != null) {
                condition.add(SYSTEM_TYPE.eq(systemType.name))
            }
            if (projectId != null) {
                condition.add(PROJECT_ID.eq(projectId))
            }
            if (ownerType != null) {
                condition.add(OWNER_TYPE.eq(ownerType.name))
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
                    .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                    .execute()
            } else {
                dslContext.update(this)
                    .set(STATUS, status.ordinal)
                    .set(HOST_NAME, hostName)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .set(LAST_STATUS_UPDATE_TIME, LocalDateTime.now())
                    .where(NAME.eq(workspaceName))
                    .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
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

    fun modifyWorkspaceProperty(
        dslContext: DSLContext,
        projectId: String,
        workspaceName: String,
        workspaceProperty: WorkspaceProperty
    ) {
        with(workspaceProperty) {
            if (displayName != null || remark != null) {
                with(TWorkspace.T_WORKSPACE) {
                    dslContext.update(this)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .let { i ->
                            if (displayName != null) i.set(DISPLAY_NAME, displayName) else i
                        }
                        .let { i ->
                            if (remark != null) i.set(REMARK, remark) else i
                        }
                        .where(NAME.eq(workspaceName))
                        .execute()
                }
            }
            if (labels != null) {
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    with(TWorkspaceLabels.T_WORKSPACE_LABELS) {
                        transactionContext.delete(this)
                            .where(WORKSPACE_NAME.eq(workspaceName))
                            .execute()
                        transactionContext.batch(
                            labels!!.map { label ->
                                transactionContext.insertInto(
                                    this,
                                    PROJECT_ID,
                                    WORKSPACE_NAME,
                                    LABEL
                                ).values(
                                    projectId,
                                    workspaceName,
                                    label
                                ).onDuplicateKeyIgnore()
                            }
                        ).execute()
                        /*关联更新，查询关键字使用T_WORKSPACE_LABELS,但通过T_WORKSPACE拿到LABELS值*/
                        with(TWorkspace.T_WORKSPACE) {
                            transactionContext.update(this)
                                .set(UPDATE_TIME, LocalDateTime.now())
                                .set(LABELS, workspaceProperty.labels.let { self -> JsonUtil.toJson(self!!, false) })
                                .where(NAME.eq(workspaceName))
                                .execute()
                        }
                    }
                }
            }
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

    fun bakWorkspace(
        dslContext: DSLContext,
        workspaceName: String,
        bakName: String,
        status: WorkspaceStatus
    ): Int {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.update(this)
                .set(NAME, bakName)
                .set(STATUS, status.ordinal)
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

    @Deprecated("后期删除此方法")
    fun limitFetchWorkspaceDetail(
        dslContext: DSLContext,
        limit: SQLLimit
    ): List<TWorkspaceDetailRecord> {
        return with(TWorkspaceDetail.T_WORKSPACE_DETAIL) {
            dslContext.selectFrom(this)
                .orderBy(ID.desc())
                .limit(limit.offset, limit.limit)
                .skipCheck()
                .fetch()
        }
    }

    fun fetchAllUsedWindows(
        dslContext: DSLContext
    ): List<String> {
        with(TWorkspace.T_WORKSPACE) {
            return dslContext.select(NAME).from(this)
                .where(SYSTEM_TYPE.eq(WorkspaceSystemType.WINDOWS_GPU.name))
                .and(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .and(STATUS.notEqual(WorkspaceStatus.DELIVERING_FAILED.ordinal))
                .and(STATUS.notEqual(WorkspaceStatus.DELIVERING.ordinal))
                .and(STATUS.notEqual(WorkspaceStatus.PREPARING.ordinal))
                .fetch(NAME)
        }
    }

    fun getAvailableCgsWorkspace(
        dslContext: DSLContext,
        cgsId: String
    ): Int {
        return dslContext.fetchCount(
            dslContext.select(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP)
                .from(TWorkspace.T_WORKSPACE)
                .leftJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
                .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
                .where(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                .and(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.eq(cgsId))
        )
    }

    class TWorkspaceRecordJooqMapper : RecordMapper<TWorkspaceRecord, WorkspaceRecord> {
        override fun map(record: TWorkspaceRecord?): WorkspaceRecord? {
            return record?.run {
                WorkspaceRecord(
                    workspaceId = id,
                    projectId = projectId,
                    workspaceName = name,
                    displayName = displayName,
                    usageTime = usageTime,
                    sleepingTime = sleepingTime,
                    createUserId = creator,
                    creatorBgName = creatorBgName,
                    creatorDeptName = creatorDeptName,
                    creatorCenterName = creatorCenterName,
                    creatorGroupName = creatorGroupName,
                    status = WorkspaceStatus.load(status),
                    createTime = createTime,
                    updateTime = updateTime,
                    lastStatusUpdateTime = lastStatusUpdateTime,
                    workspaceMountType = WorkspaceMountType.valueOf(workspaceMountType),
                    workspaceSystemType = WorkspaceSystemType.valueOf(systemType),
                    ownerType = WorkspaceOwnerType.valueOf(ownerType),
                    remark = remark,
                    labels = labels?.let { self ->
                        JsonUtil.getObjectMapper().readValue(self) as List<String>
                    },
                    bakWorkspaceName = bakName
                )
            }
        }
    }

    class TWorkspaceRecordWithWindowsJooqMapper : RecordMapper<Record, WorkspaceRecordWithWindows> {
        override fun map(record: Record?): WorkspaceRecordWithWindows? {

            if (record == null) {
                return null
            }
            return WorkspaceRecordWithWindows(
                workspaceId = record.getOrNull(TWorkspace.T_WORKSPACE.ID) as Long? ?: -1,
                projectId = record.getOrNull(TWorkspace.T_WORKSPACE.PROJECT_ID) as String? ?: "NO_CHECK",
                workspaceName = record.getOrNull(TWorkspace.T_WORKSPACE.NAME) as String? ?: "NO_CHECK",
                displayName = record.getOrNull(TWorkspace.T_WORKSPACE.DISPLAY_NAME) as String? ?: "NO_CHECK",
                usageTime = record.getOrNull(TWorkspace.T_WORKSPACE.USAGE_TIME) as Int? ?: -1,
                sleepingTime = record.getOrNull(TWorkspace.T_WORKSPACE.SLEEPING_TIME) as Int? ?: -1,
                createUserId = record.getOrNull(TWorkspace.T_WORKSPACE.CREATOR) as String? ?: "NO_CHECK",
                creatorBgName = record.getOrNull(TWorkspace.T_WORKSPACE.CREATOR_BG_NAME) as String? ?: "NO_CHECK",
                creatorDeptName = record.getOrNull(TWorkspace.T_WORKSPACE.CREATOR_DEPT_NAME) as String?
                    ?: "NO_CHECK",
                creatorCenterName = record.getOrNull(TWorkspace.T_WORKSPACE.CREATOR_CENTER_NAME) as String?
                    ?: "NO_CHECK",
                creatorGroupName = record.getOrNull(TWorkspace.T_WORKSPACE.CREATOR_GROUP_NAME) as String?
                    ?: "NO_CHECK",
                status = WorkspaceStatus.values()[
                    record.getOrNull(TWorkspace.T_WORKSPACE.STATUS) as Int? ?: 1],
                createTime = record.getOrNull(TWorkspace.T_WORKSPACE.CREATE_TIME) as LocalDateTime?
                    ?: LocalDateTime.now(),
                updateTime = record.getOrNull(TWorkspace.T_WORKSPACE.UPDATE_TIME) as LocalDateTime?
                    ?: LocalDateTime.now(),
                lastStatusUpdateTime = record.getOrNull(TWorkspace.T_WORKSPACE.LAST_STATUS_UPDATE_TIME) as LocalDateTime?,
                workspaceMountType = WorkspaceMountType.valueOf(
                    record.getOrNull(TWorkspace.T_WORKSPACE.WORKSPACE_MOUNT_TYPE) as String? ?: "START"
                ),
                workspaceSystemType = WorkspaceSystemType.valueOf(
                    record.getOrNull(TWorkspace.T_WORKSPACE.SYSTEM_TYPE) as String? ?: "WINDOWS_GPU"
                ),
                ownerType = WorkspaceOwnerType.valueOf(
                    record.getOrNull(TWorkspace.T_WORKSPACE.OWNER_TYPE) as String? ?: "PROJECT"
                ),
                remark = record.getOrNull(TWorkspace.T_WORKSPACE.REMARK) as String?,
                hostIp = record.getOrNull(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP) as String?,
                macAddress = record.getOrNull(TWorkspaceWindows.T_WORKSPACE_WINDOWS.MAC_ADDRESS) as String?,
                imageId = record.getOrNull(TWorkspaceWindows.T_WORKSPACE_WINDOWS.IMAGE_ID) as String?,
                zoneId = record.getOrNull(TWorkspaceWindows.T_WORKSPACE_WINDOWS.ZONE_ID) as String?,
                winConfigId = record.getOrNull(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID) as Int?,
                curLaunchId = record.getOrNull(TWorkspaceWindows.T_WORKSPACE_WINDOWS.CUR_LAUNCH_ID) as Int?,
                regionId = record.getOrNull(TWorkspaceWindows.T_WORKSPACE_WINDOWS.REGION_ID) as Int?,
                labels = (record.getOrNull(TWorkspace.T_WORKSPACE.LABELS) as String?)?.let { self ->
                    JsonUtil.getObjectMapper().readValue(self) as List<String>
                },
                bakWorkspaceName = record.getOrNull(TWorkspace.T_WORKSPACE.BAK_NAME) as String?
            )
        }

        private fun Record.getOrNull(name: TableField<*, *>): Any? {
            val index = this.fieldsRow().fields().indexOfFirst { it == name }
            if (index < 0) return null
            return this.get(index)
        }
    }

    companion object {
        val workspaceMapper = TWorkspaceRecordJooqMapper()
        val workspaceWithWindowsMapper = TWorkspaceRecordWithWindowsJooqMapper()
    }
}
