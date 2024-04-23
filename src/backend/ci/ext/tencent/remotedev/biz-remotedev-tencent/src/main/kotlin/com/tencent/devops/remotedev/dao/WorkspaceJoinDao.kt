package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.JooqUtils
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TRemotedevExpertSupport
import com.tencent.devops.model.remotedev.tables.TWindowsResourceType
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceDetail
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecord
import com.tencent.devops.remotedev.pojo.WorkspaceRecordInf
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithDetail
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.SelectConditionStep
import org.jooq.SelectJoinStep
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 针对 workspace 需要连表查询的复杂场景独立出来的 dao 方便整理代码
 */
@Suppress("ALL")
@Repository
class WorkspaceJoinDao {

    fun countProjectWorkspace(
        dslContext: DSLContext,
        queryType: QueryType = QueryType.WEB,
        search: WorkspaceSearch
    ): Long {
        return dslContext.fetchCount(
            genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                queryType = queryType,
                search = search
            ).skipCheck()
        ).toLong()
    }

    /**
     * 获取项目下工作空间列表
     */
    fun limitFetchProjectWorkspace(
        dslContext: DSLContext,
        limit: SQLLimit?,
        queryType: QueryType = QueryType.WEB,
        search: WorkspaceSearch
    ): List<WorkspaceRecordInf>? {
        with(TWorkspace.T_WORKSPACE) {
            // 没有包含其他表的条件
            if (search.onlyNeedCheckWorkspace()) {
                val dsl = (
                    genFetchProjectWorkspaceCond(
                        dslContext = dslContext,
                        queryType = queryType,
                        search = search
                    ) as SelectConditionStep<TWorkspaceRecord>
                    ).orderBy(CREATE_TIME.desc(), ID.desc())
                if (limit != null) {
                    dsl.limit(limit.limit).offset(limit.offset)
                }
                return dsl.skipCheck()
                    .fetch(WorkspaceDao.workspaceMapper)
            }

            // 包含 detail 表的条件
            if (search.needCheckDetail()) {
                val dsl = genFetchProjectWorkspaceCond(
                    dslContext = dslContext,
                    queryType = queryType,
                    search = search
                ).orderBy(CREATE_TIME.desc(), ID.desc())
                if (limit != null) {
                    dsl.limit(limit.limit).offset(limit.offset)
                }
                return dsl.skipCheck()
                    .fetch(workspaceWithDetailMapper)
            }

            // 剩下的只剩 workspace 表的
            val dsl = genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                queryType = queryType,
                search = search
            ).orderBy(CREATE_TIME.desc(), ID.desc())
            if (limit != null) {
                dsl.limit(limit.limit).offset(limit.offset)
            }
            return dsl.skipCheck()
                .fetch(workspaceFieldMapper)
        }
    }

    private fun genFetchProjectWorkspaceCond(
        dslContext: DSLContext,
        queryType: QueryType = QueryType.WEB,
        search: WorkspaceSearch
    ): SelectConditionStep<*> {
        val conditions = mutableListOf<Condition>()
        with(TWorkspace.T_WORKSPACE) {
            search.projectId?.ifEmpty { null }?.let { projects ->
                /*仅op支持项目通过模糊查询*/
                if (search.onFuzzyMatch && queryType == QueryType.OP) {
                    conditions.add(PROJECT_ID.likeRegex(projects.joinToString("|")))
                } else {
                    conditions.add(PROJECT_ID.`in`(projects))
                }
            }

            search.workspaceName?.ifEmpty { null }?.let { names ->
                if (search.onFuzzyMatch) {
                    conditions.add(NAME.likeRegex(names.joinToString("|")))
                } else {
                    conditions.add(NAME.`in`(names))
                }
            }

            search.displayName?.ifEmpty { null }?.let { names ->
                if (search.onFuzzyMatch) {
                    conditions.add(DISPLAY_NAME.likeRegex(names.joinToString("|")))
                } else {
                    conditions.add(DISPLAY_NAME.`in`(names))
                }
            }

            search.status?.ifEmpty { null }?.let { status ->
                conditions.add(STATUS.`in`(status.map { it.ordinal }))
            } ?: kotlin.run {
                conditions.add(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
            }

            search.workspaceSystemType?.ifEmpty { null }?.let { types ->
                if (search.onFuzzyMatch) {
                    conditions.add(SYSTEM_TYPE.likeRegex(types.joinToString("|") { it.name }))
                } else {
                    conditions.add(SYSTEM_TYPE.`in`(types.map { it.name }))
                }
            }

            queryType.ownerType()?.let {
                conditions.add(OWNER_TYPE.eq(it.name))
            }
        }

        // 没有连表查询的条件
        if (search.onlyNeedCheckWorkspace()) {
            return dslContext.selectFrom(TWorkspace.T_WORKSPACE).where(conditions)
        }

        if (!search.ips.isNullOrEmpty()) {
            val j = JooqUtils.jsonExtract(
                t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                t2 = "\$.hostIP",
                lower = false,
                removeDoubleQuotes = true
            )
            val condition = if (search.onFuzzyMatch) {
                j.likeRegex(search.ips?.joinToString("|"))
            } else {
                j.`in`(search.ips)
            }
            conditions.add(condition)
        }

        if (!search.zoneShortName.isNullOrEmpty()) {
            val j = JooqUtils.jsonExtract(
                t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                t2 = "\$.hostIP",
                lower = false,
                removeDoubleQuotes = true
            )
            val condition = j.likeRegex(search.zoneShortName?.joinToString("|"))
            conditions.add(condition)
        }

        // owner 条件查询
        search.owner?.ifEmpty { null }?.let { owners ->
            val sql = if (search.onFuzzyMatch) {
                (
                    TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name)
                        .and(TWorkspace.T_WORKSPACE.CREATOR.likeRegex(owners.joinToString("|")))
                    )
                    .or(
                        TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(
                            WorkspaceShared.AssignType.OWNER.name
                        )
                            .and(
                                TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER
                                    .likeRegex(owners.joinToString("|"))
                            )
                    )
            } else {
                (
                    TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name)
                        .and(TWorkspace.T_WORKSPACE.CREATOR.`in`(owners))
                    ).or(
                    TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name)
                        .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.`in`(owners))
                )
            }
            conditions.add(sql)
        }

        // viewers 条件查询
        search.viewers?.ifEmpty { null }?.let { viewers ->
            val sql = if (search.onFuzzyMatch) {
                (
                    TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name)
                        .and(TWorkspace.T_WORKSPACE.CREATOR.likeRegex(viewers.joinToString("|")))
                    ).or(
                    TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.likeRegex("VIEWER|OWNER")
                        .and(
                            TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER
                                .likeRegex(viewers.joinToString("|"))
                        )
                )
            } else {
                (
                    TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name)
                        .and(TWorkspace.T_WORKSPACE.CREATOR.`in`(viewers))
                    ).or(
                    TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.VIEWER.name)
                        .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.`in`(viewers))
                ).or(
                    TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name)
                        .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.`in`(viewers))
                )
            }
            conditions.add(sql)
        }

        // machineType 条件查询
        search.size?.ifEmpty { null }?.let { type ->
            conditions.add(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE.`in`(type))
        }

        // mac地址 条件查询
        search.macAddress?.ifEmpty { null }?.let { mac ->
            if (search.onFuzzyMatch) {
                conditions.add(TWorkspaceWindows.T_WORKSPACE_WINDOWS.MAC_ADDRESS.likeRegex(mac.joinToString("|")))
            } else {
                conditions.add(TWorkspaceWindows.T_WORKSPACE_WINDOWS.MAC_ADDRESS.`in`(mac))
            }
        }

        // expertSup
        search.expertSupId?.ifEmpty { null }?.let { ids ->
            val sql = if (search.onFuzzyMatch) {
                TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.ID.likeRegex(ids.joinToString("|"))
            } else {
                TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.ID.`in`(ids)
            }
            conditions.add(sql)
        }

        val fields = TWorkspace.T_WORKSPACE.fields().toMutableList()

        if (!search.ips.isNullOrEmpty() || !search.zoneShortName.isNullOrEmpty()) {
            fields.add(TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL)
        }

        return dslContext.selectDistinct(fields)
            .from(TWorkspace.T_WORKSPACE)
            .joinTable(search)
            .where(conditions)
    }

    private fun SelectJoinStep<*>.joinTable(search: WorkspaceSearch): SelectJoinStep<*> {
        if (!search.owner.isNullOrEmpty() || !search.viewers.isNullOrEmpty()) {
            this.leftJoin(TWorkspaceShared.T_WORKSPACE_SHARED)
                .on(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME.eq(TWorkspace.T_WORKSPACE.NAME))
        }
        if (!search.ips.isNullOrEmpty() || !search.zoneShortName.isNullOrEmpty()) {
            this.leftJoin(TWorkspaceDetail.T_WORKSPACE_DETAIL)
                .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceDetail.T_WORKSPACE_DETAIL.WORKSPACE_NAME))
        }
        if (!search.macAddress.isNullOrEmpty() || !search.size.isNullOrEmpty()) {
            this.leftJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS).on(
                TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME)
            )
        }

        if (!search.size.isNullOrEmpty()) {
            this.leftJoin(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE).on(
                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.eq(
                    TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID.cast(Int::class.java)
                )
            )
        }
        if (!search.expertSupId.isNullOrEmpty()) {
            this.leftJoin(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT).on(
                TWorkspace.T_WORKSPACE.NAME.eq(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.WORKSPACE_NAME)
            )
        }
        return this
    }

    fun fetchProjectFromUser(
        dslContext: DSLContext,
        userId: String
    ): Set<String> {
        return dslContext.select(TWorkspace.T_WORKSPACE.PROJECT_ID)
            .from(TWorkspace.T_WORKSPACE, TWorkspaceShared.T_WORKSPACE_SHARED)
            .where(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
            .and(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
            .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.eq(userId))
            .fetch().distinct()
            .map { it[TWorkspace.T_WORKSPACE.PROJECT_ID.name] as String? ?: "" }
            .filter { it.isNotBlank() }
            .toSet()
    }

    // 获取正在运行的 workspace 的用户
    fun fetchProjectSharedUser(
        dslContext: DSLContext,
        projectId: String
    ): Set<String> {
        return dslContext.select(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER)
            .from(TWorkspace.T_WORKSPACE, TWorkspaceShared.T_WORKSPACE_SHARED)
            .where(TWorkspace.T_WORKSPACE.PROJECT_ID.eq(projectId))
            .and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
            .and(
                TWorkspace.T_WORKSPACE.STATUS.notIn(
                    WorkspaceStatus.PREPARING.ordinal,
                    WorkspaceStatus.DELETED.ordinal,
                    WorkspaceStatus.DELIVERING_FAILED.ordinal
                )
            )
            .fetch().distinct()
            .map { it[TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER] ?: "" }
            .filter { it.isNotBlank() }
            .toSet()
    }

    class TWorkspaceFieldJooqMapper : RecordMapper<Record, WorkspaceRecord> {
        override fun map(record: Record?): WorkspaceRecord? {
            if (record == null) {
                return null
            }
            return WorkspaceRecord(
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
                remark = record["REMARK"] as String?
            )
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
                workSpaceDetail = record["DETAIL"] as String,
                remark = record["REMARK"] as String?
            )
        }
    }

    fun fetchProjectMachineType(
        dslContext: DSLContext,
        projectId: String
    ): Set<String> {
        return dslContext.selectDistinct(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE)
            .from(
                TWorkspace.T_WORKSPACE,
                TWorkspaceWindows.T_WORKSPACE_WINDOWS,
                TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE
            ).where(TWorkspace.T_WORKSPACE.PROJECT_ID.eq(projectId)).and(
                TWorkspace.T_WORKSPACE.STATUS.notIn(
                    WorkspaceStatus.PREPARING.ordinal,
                    WorkspaceStatus.DELETED.ordinal,
                    WorkspaceStatus.DELIVERING_FAILED.ordinal
                )
            ).and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
            .and(
                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.eq(
                    TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID.cast(Int::class.java)
                )
            ).fetch().map { it["SIZE"].toString() }.toSet()
    }

    fun fetchIp(
        dslContext: DSLContext,
        projectId: String,
        size: String?,
        owners: Set<String>?
    ): Set<String> {
        val tables = mutableListOf(TWorkspace.T_WORKSPACE, TWorkspaceWindows.T_WORKSPACE_WINDOWS)
        if (!size.isNullOrEmpty()) {
            tables.add(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE)
        }
        if (!owners.isNullOrEmpty()) {
            tables.add(TWorkspaceShared.T_WORKSPACE_SHARED)
        }
        val dsl = dslContext.select(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP).from(tables)
            .where(TWorkspace.T_WORKSPACE.PROJECT_ID.eq(projectId)).and(
                TWorkspace.T_WORKSPACE.STATUS.notIn(
                    WorkspaceStatus.PREPARING.ordinal,
                    WorkspaceStatus.DELETED.ordinal,
                    WorkspaceStatus.DELIVERING_FAILED.ordinal
                )
            ).and(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
        if (!size.isNullOrBlank()) {
            dsl.and(
                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.eq(
                    TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID.cast(Int::class.java)
                ).and(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE.eq(size))
            )
        }
        if (!owners.isNullOrEmpty()) {
            dsl.and(
                TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name)
                    .and(TWorkspace.T_WORKSPACE.CREATOR.`in`(owners))
            ).or(
                TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name)
                    .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.`in`(owners))
            )
        }
        return dsl.fetch().map { it["HOST_IP"] as String }.toSet()
    }

    companion object {
        val workspaceFieldMapper = TWorkspaceFieldJooqMapper()
        val workspaceWithDetailMapper = TWorkspaceRecordWithDetailJooqMapper()
    }
}
