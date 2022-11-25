package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.pojo.Workspace
import com.tencent.devops.remotedev.pojo.WorkspaceAction
import com.tencent.devops.remotedev.pojo.WorkspaceDetail
import com.tencent.devops.remotedev.pojo.WorkspaceOpHistory
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class WorkspaceService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val workspaceDao: WorkspaceDao,
    private val workspaceHistoryDao: WorkspaceHistoryDao,
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceService::class.java)
    }

    fun getWorkspaceList(userId: String, page: Int?, pageSize: Int?): Page<Workspace> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val count = workspaceDao.countWorkspace(dslContext, userId)
        val result = workspaceDao.limitFetchWorkspace(
            dslContext = dslContext,
            userId = userId,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )

        val

        return Page(page = pageNotNull, pageSize = pageSizeNotNull, count = count, records = result.map {
            val status = WorkspaceStatus.values()[it.status]
            Workspace(
                workspaceId = it.id,
                name = it.name,
                repositoryUrl = it.url,
                branch = it.branch,
                devFilePath = it.yamlPath,
                wsTemplateId = it.templateId,
                status = status
            )
        })
    }

    fun getWorkspaceDetail(userId: String, workspaceId: Long): WorkspaceDetail? {
        val workspace = workspaceDao.fetchAnyWorkspace(dslContext, workspaceId = workspaceId) ?: return null

        val workspaceStatus = WorkspaceStatus.values()[workspace.status]

        val history = workspaceHistoryDao.fetchHistory(dslContext, workspaceId)

        val last = history.firstOrNull() ?: return null

        // TODO: 2022/11/24 优惠时间需后续配置
        val discountTime = 0

        val usageTime = history.sumOf { Duration.between(it.startTime, it.endTime).seconds }.run {
            this + if (workspaceStatus.isRunning()) {
                // 如果正在运行，需要加上该次启动距离当前的时间
                Duration.between(last.startTime, LocalDateTime.now()).seconds
            } else 0
        }

        val sleepingTime = history.sumOf { it.lastSleepTimeCost }.run {
            this + if (workspaceStatus.isSleeping()) {
                // 如果正在休眠，需要加上距离上次结束距离的时间
                Duration.between(last.endTime, LocalDateTime.now()).seconds
            } else 0
        }
        val chargeableTime = usageTime - discountTime

        return with(workspace) {
            WorkspaceDetail(
                workspaceId = id,
                name = name,
                status = workspaceStatus,
                lastUpdateTime = updateTime.timestamp(),
                chargeableTime = chargeableTime,
                usageTime = usageTime,
                sleepingTime = sleepingTime,
                cpu = cpu,
                memory = memory,
                disk = disk,
                yaml = yaml
            )
        }
    }

    fun getWorkspaceTimeline(
        userId: String,
        workspaceId: Long,
        page: Int?,
        pageSize: Int?
    ): Page<WorkspaceOpHistory> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 20
        val count = workspaceOpHistoryDao.countOpHistory(dslContext, workspaceId)
        val result = workspaceOpHistoryDao.limitFetchOpHistory(
            dslContext = dslContext,
            workspaceId = workspaceId,
            limit = PageUtil.convertPageSizeToSQLLimit(pageNotNull, pageSizeNotNull)
        )

        return Page(page = pageNotNull, pageSize = pageSizeNotNull, count = count, records = result.map {
            WorkspaceOpHistory(
                createdTime = it.createdTime.timestamp(),
                operator = it.operator,
                action = WorkspaceAction.values()[it.action],
                actionMessage = it.actionMsg
            )
        })
    }
}
