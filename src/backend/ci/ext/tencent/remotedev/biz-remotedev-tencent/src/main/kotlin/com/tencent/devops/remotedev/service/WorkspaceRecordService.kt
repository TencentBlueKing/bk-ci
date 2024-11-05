package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.config.BkRepoRegion
import com.tencent.devops.remotedev.config.RemoteDevBkRepoConfig
import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dao.WorkspaceRecordTicketDao
import com.tencent.devops.remotedev.dao.WorkspaceRecordUserApprovalDao
import com.tencent.devops.remotedev.dao.WorkspaceWindowsDao
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.service.client.NodeSearchBody
import com.tencent.devops.remotedev.service.client.NodeSearchPage
import com.tencent.devops.remotedev.service.client.NodeSearchRule
import com.tencent.devops.remotedev.service.client.NodeSearchRulesItem
import com.tencent.devops.remotedev.service.client.NodeSearchSort
import com.tencent.devops.remotedev.service.client.RemotedevBkRepoClient
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64

@Service
class WorkspaceRecordService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val bkRepoConfig: RemoteDevBkRepoConfig,
    private val workspaceWindowsDao: WorkspaceWindowsDao,
    private val startAppLinkDao: ProjectStartAppLinkDao,
    private val workspaceRecordUserApprovalDao: WorkspaceRecordUserApprovalDao,
    private val workspaceDao: WorkspaceDao,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val workspaceRecordTicketDao: WorkspaceRecordTicketDao,
    private val remotedevBkRepoClient: RemotedevBkRepoClient,
    private val bkItsmService: BKItsmService,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    private val permissionService: PermissionService
) {

    @Value("\${workspaceRecordTicket.aes-key}")
    private val aesKey = ""

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

        // 创建工作空间编码密钥
        saveWorkspaceRecordTicket(workspaceName)

        workspaceWindowsDao.updateRecord(
            dslContext = dslContext,
            workspaceName = workspaceName,
            enableUser = enableUser
        )
    }

    private fun genRegion(hostIp: String?): BkRepoRegion {
        val defaultZoneConfig = windowsResourceConfigService.getAllZone()
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
        // 生成访问token
        val token = permissionService.init1Password(
            userId = enableUser,
            workspaceName = workspaceName,
            projectId = null,
            expiredInSecond = 24 * 3600
        )
        return Pair(
            true,
            remotedevBkRepoClient.repoStreamCreate(
                region = region,
                projectId = projectId,
                repoName = genRepoName(workspaceName),
                userId = enableUser
            ) + "&skToken=$token"
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
        pageSize: Int?,
        startTime: Long,
        stopTime: Long
    ): Page<WorkspaceRecordMetadata> {
        val record = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(
            dslContext = dslContext,
            workspaceName = workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )
        val region = genRegion(record.hostIp)

        val searchBody = NodeSearchBody(
            select = listOf("name", "fullPath", "metadata"),
            page = NodeSearchPage(
                pageNumber = page ?: 1,
                pageSize = pageSize ?: 20
            ),
            sort = NodeSearchSort(
                properties = listOf("metadata.media.startTime"),
                direction = "DESC"
            ),
            rule = NodeSearchRule(
                rules = listOf(
                    NodeSearchRulesItem(
                        field = "projectId",
                        value = projectId,
                        operation = "EQ"
                    ),
                    NodeSearchRulesItem(
                        field = "repoName",
                        value = genRepoName(workspaceName),
                        operation = "EQ"
                    ),
                    NodeSearchRulesItem(
                        field = "path",
                        value = "/streams/",
                        operation = "EQ"
                    ),
                    NodeSearchRulesItem(
                        field = "folder",
                        value = false,
                        operation = "EQ"
                    ),
                    NodeSearchRulesItem(
                        field = "metadata.media.startTime",
                        value = startTime,
                        operation = "GTE"
                    ),
                    NodeSearchRulesItem(
                        field = "metadata.media.startTime",
                        value = stopTime,
                        operation = "LTE"
                    )
                ),
                relation = "AND"
            )
        )
        val resp = remotedevBkRepoClient.nodeSearch(
            region = region,
            userId = userId,
            body = searchBody
        ) ?: return Page(0, 0, 0, emptyList())

        // 生成访问token
        val token = permissionService.init1Password(
            userId = userId,
            workspaceName = workspaceName,
            projectId = null,
            expiredInSecond = 24 * 3600
        )
        val data = resp.records.map {
            WorkspaceRecordMetadata(
                link = bkRepoConfig.getRegionConfig(region).webUrl +
                        "/web/media/api/user/stream/$projectId/${genRepoName(workspaceName)}${it.fullPath}" +
                        "?skToken=$token",
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

    private fun saveWorkspaceRecordTicket(workspaceName: String) {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        workspaceRecordTicketDao.create(
            dslContext = dslContext,
            workspaceName = workspaceName,
            cert = BkCryptoUtil.encryptSm4ButAes(aesKey, Base64.getEncoder().encodeToString(random))
        )
    }

    fun getWorkspaceRecordTicket(workspaceName: String, token: String): String {
        val checkedToken = permissionService.checkAndGetUser1Password(token)
        if (checkedToken.workspaceName != workspaceName) {
            throw OperationException("Token verification failed.Please reapply for authorization.")
        }
        val ticket = workspaceRecordTicketDao.fetchAny(dslContext, workspaceName)?.cert ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )
        return BkCryptoUtil.decryptSm4OrAes(aesKey, ticket)
    }

    companion object {
        private const val REMOTEDEV_WORKSPACE_USER_APPROVAL_EXPIRED_DAYS =
            "remotedev:worksapce.user.approval.expiredDays"

        private const val BKREPO_WORKSPACE_REPONAME_PREFIX = "REMOTEDEV_"

        private fun genRepoName(workspaceName: String) = "$BKREPO_WORKSPACE_REPONAME_PREFIX$workspaceName"

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
