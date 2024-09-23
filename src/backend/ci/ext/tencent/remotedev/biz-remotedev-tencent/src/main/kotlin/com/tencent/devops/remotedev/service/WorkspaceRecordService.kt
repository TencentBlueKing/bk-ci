package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.BkRepoRegion
import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceRecordUserApprovalDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.service.client.RemotedevBkRepoClient
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WorkspaceRecordService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val startAppLinkDao: ProjectStartAppLinkDao,
    private val workspaceRecordUserApprovalDao: WorkspaceRecordUserApprovalDao,
    private val workspaceDao: WorkspaceDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val remotedevBkRepoClient: RemotedevBkRepoClient,
    private val bkItsmService: BKItsmService,
    private val windowsResourceConfigService: WindowsResourceConfigService
) {

    fun enableRecord(
        workspaceName: String,
        enableUser: String?
    ) {
        // 关闭直接返回
        if (enableUser.isNullOrBlank()) {
            workspaceWindowsDao.updateRecord(
                dslContext = dslContext,
                workspaceName = workspaceName,
                enableUser = enableUser
            )
            return
        }

        val record = workspaceJoinDao.fetchAnyWindowsWorkspace(
            dslContext = dslContext,
            workspaceName = workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )

        val region = genRegion(record.hostIp)

        // 区域可能没有，需要判断创建bkrepo项目
        if (remotedevBkRepoClient.existProject(region, record.projectId) != true) {
            remotedevBkRepoClient.createProject(region, enableUser, record.projectId)
        }
        workspaceWindowsDao.updateRecord(
            dslContext = dslContext,
            workspaceName = workspaceName,
            enableUser = enableUser
        )
    }

    private fun genRegion(hostIp: String?): BkRepoRegion {
        val defaultZoneConfig = windowsResourceConfigService.getAllZone(WindowsResourceZoneConfigType.DEFAULT)
            .associateBy { it.zoneShortName }
        val specZoneConfig = windowsResourceConfigService.getAllSpecZone().associateBy { it.zoneShortName }
        val zone = if (hostIp != null) {
            /*后续直接取windows表中的zoneId，不通过ip进行解析*/
            val zoneId = hostIp.substringBefore(".")
            specZoneConfig[zoneId] ?: defaultZoneConfig[zoneId.removeSuffixNumb()]
        } else {
            null
        }
        val region = when (zone?.type) {
            WindowsResourceZoneConfigType.CSIG_USE -> BkRepoRegion.CSIG
            else -> BkRepoRegion.DEVX
        }
        return region
    }

    /**
     * @return <enable, address>
     */
    fun checkRecordAndAddress(
        appId: Long,
        ip: String
    ): Pair<Boolean, String?> {
        val projectId = startAppLinkDao.getAppName(dslContext, appId) ?: return Pair(false, null)
        val (workspaceName, enableUser, hostIp) = workspaceWindowsDao.fetchRecordByProjectIp(dslContext, projectId, ip)
            ?: return Pair(false, null)
        if (enableUser.isNullOrBlank()) {
            return Pair(false, null)
        }

        val region = genRegion(hostIp)
        return Pair(
            true,
            remotedevBkRepoClient.repoStreamCreate(
                region = region,
                projectId = projectId,
                workspaceName = workspaceName,
                userId = enableUser
            )
        )
    }

    // 审批流程 -> leader -> 安全
    fun approvalRecordView(
        projectId: String,
        user: String,
        workspaceName: String
    ) {
        workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )
        bkItsmService.createRecordView(projectId = projectId, userId = user, workspaceName = workspaceName)
    }

    fun approvalRecordViewCallback(
        projectId: String,
        userId: String,
        workspaceName: String
    ) {
        workspaceRecordUserApprovalDao.addOrUpdateApproval(
            dslContext = dslContext,
            projectId = projectId,
            user = userId,
            workspaceName = workspaceName
        )
    }

    fun checkWorkspaceUserApproval(
        workspaceName: String,
        userId: String
    ): Boolean {
        return workspaceRecordUserApprovalDao.checkApproval(
            dslContext = dslContext,
            workspaceName = workspaceName,
            user = userId,
            expiredDays = redisOperation.get(REMOTEDEV_WORKSPACE_USER_APPROVAL_EXPIRED_DAYS)?.toLongOrNull() ?: 7L
        )
    }

    fun getWorkspaceRecordMetadata(
        projectId: String,
        userId: String,
        workspaceName: String,
        page: Int?,
        pageSize: Int?
    ): Page<WorkspaceRecordMetadata> {
        val record = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(
            dslContext = dslContext,
            workspaceName = workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )
        val region = genRegion(record.hostIp)

        val resp = remotedevBkRepoClient.pageNodeList(
            region = region,
            projectId = projectId,
            userId = userId,
            workspaceName = workspaceName,
            page = page ?: 1,
            pageSize = pageSize ?: 20
        ) ?: return Page(0, 0, 0, emptyList())

        val data = resp.records.map {
            WorkspaceRecordMetadata(
                link = "bkRepoConfig.getRegionConfig(region)/api/user/stream/{projectId}/{repoName}/streams/${it.fullPath}",
                startTime = it.metadata?.mediaStartTime,
                stopTime = it.metadata?.mediaStopTime
            )
        }
        return Page(
            page = resp.pageNumber,
            pageSize = resp.pageSize,
            count = resp.totalRecords,
            records = data
        )
    }

    companion object {
        private const val REMOTEDEV_WORKSPACE_USER_APPROVAL_EXPIRED_DAYS =
            "remotedev:worksapce.user.approval.expiredDays"

        private fun String.removeSuffixNumb(): String {
            for (i in this.lastIndex downTo 0) {
                if (this[i].isDigit()) {
                    continue
                }
                return this.substring(0..i)
            }
            return ""
        }
    }
}