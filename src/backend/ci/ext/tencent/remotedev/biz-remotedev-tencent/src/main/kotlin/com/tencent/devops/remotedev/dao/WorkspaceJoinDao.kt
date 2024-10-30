package com.tencent.devops.remotedev.dao

import com.tencent.devops.common.api.model.SQLLimit
import com.tencent.devops.common.db.utils.skipCheck
import com.tencent.devops.model.remotedev.tables.TRemotedevExpertSupport
import com.tencent.devops.model.remotedev.tables.TWindowsResourceType
import com.tencent.devops.model.remotedev.tables.TWindowsResourceZone
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceLabels
import com.tencent.devops.model.remotedev.tables.TWorkspaceShared
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.remotedev.dao.WorkspaceDao.Companion.workspaceWithWindowsMapper
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceRecordInf
import com.tencent.devops.remotedev.pojo.WorkspaceRecordWithWindows
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.WorkspaceShared
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import com.tencent.devops.remotedev.pojo.common.QueryType
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.ExecuteContext
import org.jooq.Field
import org.jooq.Record1
import org.jooq.SelectConditionStep
import org.jooq.SelectJoinStep
import org.jooq.SelectSelectStep
import org.jooq.Table
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListener
import org.jooq.impl.DefaultExecuteListenerProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

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
                search = search,
                checkField = windowsFullFields
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
            // 目前只有windows，如果后期增加横向扩展即可
            val dsl = genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                queryType = queryType,
                search = search,
                checkField = windowsFullFields
            ).orderBy(CREATE_TIME.desc(), ID.desc())
            if (limit != null) {
                dsl.limit(limit.limit).offset(limit.offset)
            }
            return dsl.skipCheck()
                .fetch(workspaceWithWindowsMapper)
        }
    }

    fun fetchAnyWindowsWorkspace(
        dslContext: DSLContext,
        workspaceName: String,
        status: WorkspaceStatus? = null
    ): WorkspaceRecordWithWindows? {
        // 目前只有windows，如果后期增加横向扩展即可
        val dsl = genFetchProjectWorkspaceCond(
            dslContext = dslContext,
            queryType = QueryType.SERVICE,
            search = WorkspaceSearch(
                onFuzzyMatch = false,
                status = status?.let { listOf(status) },
                workspaceName = listOf(workspaceName),
                workspaceSystemType = listOf(WorkspaceSystemType.WINDOWS_GPU)
            ),
            checkField = windowsFullFields
        )
        return dsl.skipCheck()
            .fetchAny(workspaceWithWindowsMapper)
    }

    /**
     * @param sip 不带区域的ip，比如1.23.45.234
     * @param checkField 注意注意！！如果查询比较频繁，必须指定该参数，减少返回内容
     */
    fun fetchWindowsWorkspacesSimple(
        dslContext: DSLContext,
        status: WorkspaceStatus? = null,
        ownerType: WorkspaceOwnerType? = null,
        projectId: String? = null,
        sip: String? = null,
        businessLineName: String? = null,
        owner: String? = null,
        workspaceName: String? = null,
        notStatus: List<WorkspaceStatus>? = null,
        checkField: List<Field<*>>? = null
    ): List<WorkspaceRecordWithWindows> {
        with(TWorkspace.T_WORKSPACE) {
            // 目前只有windows，如果后期增加横向扩展即可
            val dsl = genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                queryType = QueryType.SERVICE,
                search = WorkspaceSearch(
                    onFuzzyMatch = false,
                    status = status?.let { listOf(status) },
                    workspaceOwnerType = ownerType?.let { listOf(ownerType) },
                    projectId = projectId?.let { listOf(projectId) },
                    sips = sip?.let { listOf(sip) },
                    businessLineNames = businessLineName?.let { listOf(businessLineName) },
                    owner = owner?.let { listOf(owner) },
                    workspaceName = workspaceName?.let { listOf(workspaceName) },
                    notStatus = notStatus,
                    workspaceSystemType = listOf(WorkspaceSystemType.WINDOWS_GPU)
                ),
                checkField = checkField ?: windowsFullFields
            ).orderBy(CREATE_TIME.desc(), ID.desc())
            return dsl.skipCheck()
                .fetch(workspaceWithWindowsMapper)
        }
    }

    fun fetchWindowsWorkspaces(
        dslContext: DSLContext,
        workspaceNames: Set<String>? = null,
        size: Set<String>? = null,
        projectIds: Set<String>? = null,
        sips: Set<String>? = null,
        owners: Set<String>? = null,
        notStatus: Set<WorkspaceStatus>? = null,
        checkField: List<Field<*>>? = null
    ): List<WorkspaceRecordWithWindows> {
        with(TWorkspace.T_WORKSPACE) {
            // 目前只有windows，如果后期增加横向扩展即可
            val dsl = genFetchProjectWorkspaceCond(
                dslContext = dslContext,
                queryType = QueryType.SERVICE,
                search = WorkspaceSearch(
                    onFuzzyMatch = false,
                    workspaceName = workspaceNames?.toList(),
                    size = size?.toList(),
                    sips = sips?.toList(),
                    owner = owners?.toList(),
                    notStatus = notStatus?.toList(),
                    projectId = projectIds?.toList(),
                    workspaceSystemType = listOf(WorkspaceSystemType.WINDOWS_GPU)
                ),
                checkField = checkField ?: windowsFullFields
            ).orderBy(CREATE_TIME.desc(), ID.desc())
            return dsl.skipCheck()
                .fetch(workspaceWithWindowsMapper)
        }
    }

    private fun genFetchProjectWorkspaceCond(
        dslContext: DSLContext,
        queryType: QueryType = QueryType.WEB,
        search: WorkspaceSearch,
        checkField: List<Field<*>>
    ): SelectConditionStep<*> {
        dslContext.configuration().set(DefaultExecuteListenerProvider(object : DefaultExecuteListener() {
            override fun resultEnd(ctx: ExecuteContext) {
                val sql = DSL.using(ctx.configuration()).renderInlined(ctx.query())
                logger.info("genFetchProjectWorkspaceCond Executed SQL|$sql|${ctx.result()?.size}")
                super.executeEnd(ctx)
            }
        }))
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
                if (queryType != QueryType.SERVICE) {
                    conditions.add(STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
                    conditions.add(STATUS.notEqual(WorkspaceStatus.UNUSED.ordinal))
                }
            }

            search.notStatus?.ifEmpty { null }?.let { status ->
                conditions.add(STATUS.notIn(status.map { it.ordinal }))
            }

            search.businessLineNames?.ifEmpty { null }?.let { name ->
                conditions.add(CREATOR_DEPT_NAME.`in`(name))
            }

            search.workspaceSystemType?.ifEmpty { null }?.let { types ->
                if (search.onFuzzyMatch) {
                    conditions.add(SYSTEM_TYPE.likeRegex(types.joinToString("|") { it.name }))
                } else {
                    conditions.add(SYSTEM_TYPE.`in`(types.map { it.name }))
                }
            }

            search.workspaceOwnerType?.ifEmpty { null }?.let { types ->
                if (search.onFuzzyMatch) {
                    conditions.add(OWNER_TYPE.likeRegex(types.joinToString("|") { it.name }))
                } else {
                    conditions.add(OWNER_TYPE.`in`(types.map { it.name }))
                }
            }

            queryType.ownerType()?.let {
                conditions.add(OWNER_TYPE.eq(it.name))
            }
        }

        if (!search.ips.isNullOrEmpty()) {
            if (search.onFuzzyMatch) {
                conditions.add(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.likeRegex(search.ips?.joinToString("|")))
            } else {
                conditions.add(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.`in`(search.ips))
            }
        }

        if (!search.sips.isNullOrEmpty()) {
            conditions.add(
                TWorkspace.T_WORKSPACE.IP.`in`(search.sips)
            )
        }

        // TODO windows表已经存在Zone id字段，但是数据没有补齐，后续可以切换过来就不使用HOST_IP进行模糊查询了
        if (!search.zoneShortName.isNullOrEmpty()) {
            conditions.add(TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP.likeRegex(search.zoneShortName?.joinToString("|")))
        }

        if (!search.logicalArea.isNullOrEmpty()) {
            conditions.add(logicalAreaGetZone(dslContext, search.logicalArea!!))
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

        return dslContext.selectDistinct(checkField)
            .from(TWorkspace.T_WORKSPACE)
            .joinTable(search)
            .where(conditions)
    }

    private fun SelectJoinStep<*>.joinTable(search: WorkspaceSearch): SelectJoinStep<*> {
        this.innerJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS).on(
            TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME)
        )
        if (search.needCheckShared()) {
            this.leftJoin(TWorkspaceShared.T_WORKSPACE_SHARED)
                .on(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME.eq(TWorkspace.T_WORKSPACE.NAME))
        }
        if (search.needCheckLabels()) {
            val label = labelsTable(search.labels!!)
            this.rightJoin(label)
                .on(TWorkspace.T_WORKSPACE.NAME.eq(label.field(TWorkspaceLabels.T_WORKSPACE_LABELS.WORKSPACE_NAME)))
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

    private fun logicalAreaGetZone(dslContext: DSLContext, areas: List<WindowsResourceZoneConfigType>): Condition {
        /*考虑到查询此频率不会太高，不做缓存。后续视情况可做缓存*/
        val stMap = with(TWindowsResourceZone.T_WINDOWS_RESOURCE_ZONE) {
            dslContext.select(SHORT_NAME, TYPE).from(this)
                .where(AVAILABLED.eq(1))
                .skipCheck()
                .fetch().groupBy({ it.value2() }) { it.value1() }
        }
        return if (WindowsResourceZoneConfigType.DEFAULT in areas) {
            /*如果DEFAULT在查询中，则利用反查*/
            val zoneIds = stMap.filter { WindowsResourceZoneConfigType.parse(it.key) !in areas }.flatMap { it.value }
            TWorkspaceWindows.T_WORKSPACE_WINDOWS.ZONE_ID.notLikeRegex(
                zoneIds.joinToString(
                    separator = "$|^",
                    prefix = "^",
                    postfix = "$"
                )
            )
        } else {
            val zoneIds = stMap.filter { WindowsResourceZoneConfigType.parse(it.key) in areas }.flatMap { it.value }
            TWorkspaceWindows.T_WORKSPACE_WINDOWS.ZONE_ID.likeRegex(
                zoneIds.joinToString(
                    separator = "$|^",
                    prefix = "^",
                    postfix = "$"
                )
            )
        }
    }

    /**
     * labels 的交集查询子句
     *
     * sql示例：
     * ```sql
     * select alias_107434951.`WORKSPACE_NAME`
     * from (select `devops_remotedev`.`T_WORKSPACE_LABELS`.`WORKSPACE_NAME`
     *      from `devops_remotedev`.`T_WORKSPACE_LABELS`
     *      where `devops_remotedev`.`T_WORKSPACE_LABELS`.`LABEL` = '2121') as `alias_107434951`
     * group by alias_107434951.`WORKSPACE_NAME`
     * having count(*) = 1;
     * ```
     **/
    private fun labelsTable(labels: List<String>): Table<Record1<String>> {
        kotlin.run { }
        with(TWorkspaceLabels.T_WORKSPACE_LABELS) {
            val subquery = DSL.select(WORKSPACE_NAME)
                .from(this)
                .where(LABEL.eq(labels.first()))
                .also { dsl ->
                    labels.drop(1).forEach {
                        dsl.unionAll(
                            DSL.select(WORKSPACE_NAME)
                                .from(this)
                                .where(LABEL.eq(it))
                        )
                    }
                }.asTable()
            return DSL.select(subquery.field(WORKSPACE_NAME))
                .from(subquery)
                .groupBy(subquery.field(WORKSPACE_NAME))
                .having(DSL.count().eq(labels.count()))
                .asTable()
        }
    }

    fun fetchProjectFromUser(
        dslContext: DSLContext,
        userId: String
    ): Set<String> {
        return dslContext.select(TWorkspace.T_WORKSPACE.PROJECT_ID)
            .from(TWorkspace.T_WORKSPACE)
            .leftJoin(TWorkspaceShared.T_WORKSPACE_SHARED)
            .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
            .where(TWorkspace.T_WORKSPACE.STATUS.notEqual(WorkspaceStatus.DELETED.ordinal))
            .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.eq(userId))
            .fetch().distinct()
            .map { it[TWorkspace.T_WORKSPACE.PROJECT_ID.name] as String? ?: "" }
            .filter { it.isNotBlank() }
            .toSet()
    }

    // 获取正常状态的 workspace 的用户
    fun fetchProjectSharedUser(
        dslContext: DSLContext,
        projectIds: Set<String>
    ): Set<String> {
        val dsl = dslContext.select(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER)
            .from(TWorkspace.T_WORKSPACE)
            .leftJoin(TWorkspaceShared.T_WORKSPACE_SHARED)
            .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME))
            .where(TWorkspace.T_WORKSPACE.PROJECT_ID.`in`(projectIds))
            .and(
                TWorkspace.T_WORKSPACE.STATUS.notIn(
                    WorkspaceStatus.PREPARING.ordinal,
                    WorkspaceStatus.DELETED.ordinal,
                    WorkspaceStatus.DELIVERING_FAILED.ordinal
                )
            )
        return dsl.fetch().distinct()
            .map { it[TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER] ?: "" }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun fetchProjectMachineType(
        dslContext: DSLContext,
        projectId: String
    ): Set<String> {
        return dslContext.selectDistinct(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE)
            .addResourceJoin(projectId).fetch().map { it["SIZE"].toString() }.toSet()
    }

    fun fetchProjectMachineTypeCount(
        dslContext: DSLContext,
        projectId: String
    ): Map<String, Int> {
        return dslContext.select(
            TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE,
            DSL.count(TWorkspace.T_WORKSPACE.NAME).`as`("COUNT")
        ).addResourceJoin(projectId).groupBy(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE).fetch()
            .map {
                it.getValue(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE) to
                    it.get("COUNT").toString().toLong().toInt()
            }.toMap()
    }

    private fun SelectSelectStep<*>.addResourceJoin(projectId: String) =
        this.from(TWorkspace.T_WORKSPACE)
            .leftJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
            .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
            .leftJoin(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE)
            .on(
                TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.eq(
                    TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID.cast(Int::class.java)
                )
            )
            .where(TWorkspace.T_WORKSPACE.PROJECT_ID.eq(projectId))
            .and(
                TWorkspace.T_WORKSPACE.STATUS.notIn(
                    WorkspaceStatus.PREPARING.ordinal,
                    WorkspaceStatus.DELETED.ordinal,
                    WorkspaceStatus.DELIVERING_FAILED.ordinal
                )
            )

    fun fetchRunningIp(
        dslContext: DSLContext,
        projectId: String,
        size: String?,
        owners: Set<String>?
    ): Set<String> {
        val dsl = dslContext.select(TWorkspace.T_WORKSPACE.HOST_NAME).from(TWorkspace.T_WORKSPACE)
        if (!size.isNullOrEmpty()) {
            dsl.leftJoin(TWorkspaceWindows.T_WORKSPACE_WINDOWS)
                .on(TWorkspace.T_WORKSPACE.NAME.eq(TWorkspaceWindows.T_WORKSPACE_WINDOWS.WORKSPACE_NAME))
            dsl.leftJoin(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE)
                .on(
                    TWorkspaceWindows.T_WORKSPACE_WINDOWS.WIN_CONFIG_ID.eq(
                        TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.ID.cast(Int::class.java)
                    )
                )
        }
        if (!owners.isNullOrEmpty()) {
            dsl.leftJoin(TWorkspaceShared.T_WORKSPACE_SHARED)
                .on(TWorkspaceShared.T_WORKSPACE_SHARED.WORKSPACE_NAME.eq(TWorkspace.T_WORKSPACE.NAME))
        }
        val stepDsl = dsl.where(TWorkspace.T_WORKSPACE.PROJECT_ID.eq(projectId))
            .and(TWorkspace.T_WORKSPACE.STATUS.eq(WorkspaceStatus.RUNNING.ordinal))
        if (!size.isNullOrBlank()) {
            stepDsl.and(TWindowsResourceType.T_WINDOWS_RESOURCE_TYPE.SIZE.eq(size))
        }
        if (!owners.isNullOrEmpty()) {
            stepDsl.and(TWorkspaceShared.T_WORKSPACE_SHARED.ASSIGN_TYPE.eq(WorkspaceShared.AssignType.OWNER.name))
                .and(TWorkspaceShared.T_WORKSPACE_SHARED.SHARED_USER.`in`(owners))
        }
        return stepDsl.fetch().map { it["HOST_NAME"] as String }.toSet()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceJoinDao::class.java)
        val windowsFullFields = TWorkspace.T_WORKSPACE.fields()
            .plus(TWorkspaceWindows.T_WORKSPACE_WINDOWS.fields())
            .toList()
    }
}
