package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.JooqUtils
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
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.SelectConditionStep
import org.jooq.impl.TableImpl
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
        projectId: String?,
        workspaceName: String?,
        systemType: WorkspaceSystemType?,
        queryType: QueryType? = QueryType.WEB,
        ips: List<String>?,
        owner: String?,
        status: WorkspaceStatus?,
        zoneId: String?,
        machineType: String?,
        expertSupId: Long?
    ): Long {
        return dslContext.fetchCount(
            genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                projectId = projectId,
                workspaceName = workspaceName,
                systemType = systemType,
                queryType = queryType,
                ips = ips,
                owner = owner,
                status = status,
                zoneId = zoneId,
                machineType = machineType,
                expertSupId = expertSupId
            )
        ).toLong()
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
        ips: List<String>?,
        owner: String?,
        status: WorkspaceStatus?,
        zoneId: String?,
        machineType: String?,
        expertSupId: Long?
    ): List<WorkspaceRecordInf>? {
        with(TWorkspace.T_WORKSPACE) {
            // 没有包含其他表的条件
            if (ips.isNullOrEmpty() && owner == null && zoneId == null && machineType == null && expertSupId == null) {
                return (
                        genFetchProjectWorkspaceCond(
                            dslContext = dslContext,
                            projectId = projectId,
                            workspaceName = workspaceName,
                            systemType = systemType,
                            queryType = queryType,
                            ips = ips,
                            owner = owner,
                            status = status,
                            zoneId = zoneId,
                            machineType = machineType,
                            expertSupId = expertSupId
                        ) as SelectConditionStep<TWorkspaceRecord>
                        ).orderBy(CREATE_TIME.desc(), ID.desc())
                    .limit(limit.limit).offset(limit.offset)
                    .fetch(WorkspaceDao.workspaceMapper)
            }

            // 包含 detail 表的条件
            if (!ips.isNullOrEmpty() || zoneId != null) {
                return genFetchProjectWorkspaceCond(
                    dslContext = dslContext,
                    projectId = projectId,
                    workspaceName = workspaceName,
                    systemType = systemType,
                    queryType = queryType,
                    ips = ips,
                    owner = owner,
                    status = status,
                    zoneId = zoneId,
                    machineType = machineType,
                    expertSupId = expertSupId
                ).orderBy(CREATE_TIME.desc(), ID.desc())
                    .limit(limit.limit).offset(limit.offset)
                    .fetch(workspaceWithDetailMapper)
            }

            // 剩下的只剩 workspace 表的
            return genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                projectId = projectId,
                workspaceName = workspaceName,
                systemType = systemType,
                queryType = queryType,
                ips = ips,
                owner = owner,
                status = status,
                zoneId = zoneId,
                machineType = machineType,
                expertSupId = expertSupId
            ).orderBy(CREATE_TIME.desc(), ID.desc())
                .limit(limit.limit).offset(limit.offset)
                .fetch(workspaceFieldMapper)
        }
    }

    private fun genFetchProjectWorkspaceCond(
        dslContext: DSLContext,
        projectId: String?,
        workspaceName: String?,
        systemType: WorkspaceSystemType?,
        queryType: QueryType? = QueryType.WEB,
        ips: List<String>?,
        owner: String?,
        status: WorkspaceStatus?,
        zoneId: String?,
        machineType: String?,
        expertSupId: Long?
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

            if (status != null) {
                conditions.add(STATUS.eq(status.ordinal))
            } else {
                conditions.add(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
            }

            if (systemType != null) {
                conditions.add(SYSTEM_TYPE.eq(systemType.name))
            }

            if (queryType == QueryType.WEB) {
                conditions.add(OWNER_TYPE.eq(WorkspaceOwnerType.PROJECT.name))
            }
        }

        // 没有连表查询的条件
        if (ips.isNullOrEmpty() && owner == null && zoneId == null && machineType == null && expertSupId == null) {
            return dslContext.selectFrom(TWorkspace.T_WORKSPACE).where(conditions)
        }

        // ip 和 zoneId 的查询
        if (!ips.isNullOrEmpty() || zoneId != null) {
            // 先查询他俩都存在的情况，都存在可以直接拼接查询
            if (!ips.isNullOrEmpty() && zoneId != null) {
                val comIps = mutableSetOf<String>()
                ips.forEach { ip ->
                    comIps.add("$zoneId.$ip")
                }
                var comIpsCond = JooqUtils.jsonExtract(
                    t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                    t2 = "\$.hostIP",
                    lower = false,
                    removeDoubleQuotes = true
                ).eq(comIps.first())
                comIps.drop(1).forEach { comIp ->
                    comIpsCond = comIpsCond.or(
                        JooqUtils.jsonExtract(
                            t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                            t2 = "\$.hostIP",
                            lower = false,
                            removeDoubleQuotes = true
                        ).eq(comIp)
                    )
                }
                conditions.add(comIpsCond)
            } else if (!ips.isNullOrEmpty() && zoneId == null) {
                // 存在 ips 但不存在 zoneId
                var ipsCond = JooqUtils.jsonExtract(
                    t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                    t2 = "\$.hostIP",
                    lower = false,
                    removeDoubleQuotes = true
                ).like("%${ips.first()}") as Condition
                ips.drop(1).forEach { ip ->
                    ipsCond = ipsCond.or(
                        JooqUtils.jsonExtract(
                            t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                            t2 = "\$.hostIP",
                            lower = false,
                            removeDoubleQuotes = true
                        ).like("%$ip")
                    )
                }
                conditions.add(ipsCond)
            } else {
                // 只存在 zoneId
                val zoneCond = JooqUtils.jsonExtract(
                    t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
                    t2 = "\$.hostIP",
                    lower = false,
                    removeDoubleQuotes = true
                ).like("$zoneId%")
                conditions.add(zoneCond)
            }
        }

        // owner 条件查询
        if (owner != null) {
            val sql = (
                    TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name)
                        .and(TWorkspace.T_WORKSPACE.CREATOR.like("%$owner%"))
                    )
                .or(
                    TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name)
                        .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.like("%$owner%"))
                )
            conditions.add(sql)
        }

        // machineType 条件查询
        if (machineType != null) {
            conditions.add(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE.eq(machineType))
        }

        // expertSup
        if (expertSupId != null) {
            conditions.add(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.ID.eq(expertSupId))
        }

        // 添加连表查询条件以及获得连表
        val tables = joinTablesAndItems(
            conditions = conditions,
            ips = ips,
            owner = owner,
            zoneId = zoneId,
            machineType = machineType,
            expertSupId = expertSupId
        )

        val fields = TWorkspace.T_WORKSPACE.fields().toMutableList()
        if (!ips.isNullOrEmpty() || zoneId != null) {
            fields.add(TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL)
        }

        return dslContext.select(fields)
            .from(tables)
            .where(conditions)
    }

    private fun joinTablesAndItems(
        conditions: MutableList<Condition>,
        ips: List<String>?,
        owner: String?,
        zoneId: String?,
        machineType: String?,
        expertSupId: Long?
    ): List<TableImpl<*>> {
        var offset = 0
        val result = mutableListOf<TableImpl<*>>()
        result.add(TWorkspace.T_WORKSPACE)
        if (!ips.isNullOrEmpty() || zoneId != null) {
            result.add(TWorkspaceDetail.T_WORKSPACE_DETAIL)
            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceDetail.T_WORKSPACE_DETAIL.WORKSPACE_NAME))
            offset++
        }
        if (owner != null) {
            result.add(TWorkspaceShared.T_WORKSPACE_SHARED)
            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
            offset++
        }
        if (machineType != null) {
            result.add(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
            result.add(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE)
            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
            offset++
            conditions.add(
                offset,
                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.eq(
                    TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID.cast(Int::class.java)
                )
            )
            offset++
        }
        if (expertSupId != null) {
            result.add(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT)
            conditions.add(
                offset,
                TWorkspace.T_WORKSPACE.NAME.eq(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.WORKSPACE_NAME)
            )
        }

        return result
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
                ownerType = WorkspaceOwnerType.valueOf(record["OWNER_TYPE"] as String)
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
                workSpaceDetail = record["DETAIL"] as String
            )
        }
    }

    companion object {
        val workspaceFieldMapper = TWorkspaceFieldJooqMapper()
        val workspaceWithDetailMapper = TWorkspaceRecordWithDetailJooqMapper()
    }
}
