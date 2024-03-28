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
//            projectId?.let {
//                if (queryType == QueryType.OP) {
//                    conditions.add(PROJECT_ID.like("%$it%"))
//                } else {
//                    conditions.add(PROJECT_ID.eq(it))
//                }
//            }

            search.projectId?.ifEmpty { null }?.let { projects ->
                /*仅op支持项目通过模糊查询*/
                if (search.onFuzzyMatch && queryType == QueryType.OP) {
                    conditions.add(PROJECT_ID.likeRegex(projects.joinToString("|")))
                } else {
                    conditions.add(PROJECT_ID.`in`(projects))
                }
            }

//            workspaceName?.let {
//                if (queryType == QueryType.OP) {
//                    conditions.add(NAME.like("%$it%"))
//                } else {
//                    conditions.add(NAME.eq(it))
//                }
//            }
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

//            if (status != null) {
//                conditions.add(STATUS.eq(status.ordinal))
//            } else {
//                conditions.add(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
//            }
            search.status?.ifEmpty { null }?.let { status ->
                conditions.add(STATUS.`in`(status.map { it.ordinal }))
            } ?: kotlin.run {
                conditions.add(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
            }

//            if (systemType != null) {
//                conditions.add(SYSTEM_TYPE.eq(systemType.name))
//            }
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

//            if (queryType == QueryType.WEB) {
//                conditions.add(OWNER_TYPE.eq(WorkspaceOwnerType.PROJECT.name))
//            }
        }

        // 没有连表查询的条件
        if (search.onlyNeedCheckWorkspace()) {
            return dslContext.selectFrom(TWorkspace.T_WORKSPACE).where(conditions)
        }

//        // 没有连表查询的条件
//        if (ips.isNullOrEmpty() && owner == null && zoneId == null && machineType == null && expertSupId == null) {
//            return dslContext.selectFrom(TWorkspace.T_WORKSPACE).where(conditions)
//        }
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

//        // ip 和 zoneId 的查询
//        if (!ips.isNullOrEmpty() || zoneId != null) {
//            // 先查询他俩都存在的情况，都存在可以直接拼接查询
//            if (!ips.isNullOrEmpty() && zoneId != null) {
//                val comIps = mutableSetOf<String>()
//                ips.forEach { ip ->
//                    comIps.add("$zoneId.$ip")
//                }
//                var comIpsCond = JooqUtils.jsonExtract(
//                    t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
//                    t2 = "\$.hostIP",
//                    lower = false,
//                    removeDoubleQuotes = true
//                ).eq(comIps.first())
//                comIps.drop(1).forEach { comIp ->
//                    comIpsCond = comIpsCond.or(
//                        JooqUtils.jsonExtract(
//                            t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
//                            t2 = "\$.hostIP",
//                            lower = false,
//                            removeDoubleQuotes = true
//                        ).eq(comIp)
//                    )
//                }
//                conditions.add(comIpsCond)
//            } else if (!ips.isNullOrEmpty() && zoneId == null) {
//                // 存在 ips 但不存在 zoneId
//                var ipsCond = JooqUtils.jsonExtract(
//                    t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
//                    t2 = "\$.hostIP",
//                    lower = false,
//                    removeDoubleQuotes = true
//                ).like("%${ips.first()}") as Condition
//                ips.drop(1).forEach { ip ->
//                    ipsCond = ipsCond.or(
//                        JooqUtils.jsonExtract(
//                            t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
//                            t2 = "\$.hostIP",
//                            lower = false,
//                            removeDoubleQuotes = true
//                        ).like("%$ip")
//                    )
//                }
//                conditions.add(ipsCond)
//            } else {
//                // 只存在 zoneId
//                val zoneCond = JooqUtils.jsonExtract(
//                    t1 = TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL,
//                    t2 = "\$.hostIP",
//                    lower = false,
//                    removeDoubleQuotes = true
//                ).like("$zoneId%")
//                conditions.add(zoneCond)
//            }
//        }

//        // owner 条件查询
//        if (owner != null) {
//            val sql = (
//                    TWorkspace.T_WORKSPACE.OWNER_TYPE.eq(WorkspaceOwnerType.PERSONAL.name)
//                        .and(TWorkspace.T_WORKSPACE.CREATOR.like("%$owner%"))
//                    )
//                .or(
//                    TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name)
//                        .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.like("%$owner%"))
//                )
//            conditions.add(sql)
//        }

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
//        if (machineType != null) {
//            conditions.add(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE.eq(machineType))
//        }

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
//        if (expertSupId != null) {
//            conditions.add(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.ID.eq(expertSupId))
//        }

        // expertSup
        search.expertSupId?.ifEmpty { null }?.let { ids ->
            val sql = if (search.onFuzzyMatch) {
                TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.ID.likeRegex(ids.joinToString("|"))
            } else {
                TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.ID.`in`(ids)
            }
            conditions.add(sql)
        }

        // 添加连表查询条件以及获得连表
//        val tables = joinTablesAndItems(
//            conditions = conditions,
//            search = search
//        )

        val fields = TWorkspace.T_WORKSPACE.fields().toMutableList()
//        if (!ips.isNullOrEmpty() || zoneId != null) {
//            fields.add(TWorkspaceDetail.T_WORKSPACE_DETAIL.DETAIL)
//        }

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

    private fun joinTablesAndItems(
        conditions: MutableList<Condition>,
        search: WorkspaceSearch
    ): List<TableImpl<*>> {
        var offset = 0
        val result = mutableListOf<TableImpl<*>>()
        result.add(TWorkspace.T_WORKSPACE)
//        if (!ips.isNullOrEmpty() || zoneId != null) {
//            result.add(TWorkspaceDetail.T_WORKSPACE_DETAIL)
//            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceDetail.T_WORKSPACE_DETAIL.WORKSPACE_NAME))
//            offset++
//        }
        if (!search.ips.isNullOrEmpty() || !search.zoneShortName.isNullOrEmpty()) {
            result.add(TWorkspaceDetail.T_WORKSPACE_DETAIL)
            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceDetail.T_WORKSPACE_DETAIL.WORKSPACE_NAME))
            offset++
        }
//        if (owner != null) {
//            result.add(TWorkspaceShared.T_WORKSPACE_SHARED)
//            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
//            offset++
//        }
//        if (!search.owner.isNullOrEmpty() || !search.viewers.isNullOrEmpty()) {
//            result.add(TWorkspaceShared.T_WORKSPACE_SHARED)
//            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
//            offset++
//        }

//        if (machineType != null) {
//            result.add(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
//            result.add(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE)
//            conditions.add(offset, TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
//            offset++
//            conditions.add(
//                offset,
//                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.eq(
//                    TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID.cast(Int::class.java)
//                )
//            )
//            offset++
//        }
        if (!search.size.isNullOrEmpty()) {
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
//        if (expertSupId != null) {
//            result.add(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT)
//            conditions.add(
//                offset,
//                TWorkspace.T_WORKSPACE.NAME.eq(TRemotedevExpertSupport.T_REMOTEDEV_EXPERT_SUPPORT.WORKSPACE_NAME)
//            )
//        }
        if (!search.expertSupId.isNullOrEmpty()) {
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

    companion object {
        val workspaceFieldMapper = TWorkspaceFieldJooqMapper()
        val workspaceWithDetailMapper = TWorkspaceRecordWithDetailJooqMapper()
    }
}
