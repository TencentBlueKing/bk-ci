package com.tencent.devops.remotedev.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.util.timestampmilli
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
import com.tencent.devops.remotedev.pojo.record.UserWorkspaceRecordPermissionInfo
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordMetadata
import com.tencent.devops.remotedev.pojo.record.WorkspaceRecordTicketType
import com.tencent.devops.remotedev.service.client.NodeSearchBody
import com.tencent.devops.remotedev.service.client.NodeSearchPage
import com.tencent.devops.remotedev.service.client.NodeSearchRule
import com.tencent.devops.remotedev.service.client.NodeSearchRulesItem
import com.tencent.devops.remotedev.service.client.NodeSearchSort
import com.tencent.devops.remotedev.service.client.RemotedevBkRepoClient
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys.REMOTEDEV_WORKSPACE_USER_APPROVAL_EXPIRED_DAYS
import com.tencent.devops.remotedev.utils.RsaUtil
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class WorkspaceRecordService @Autowired constructor(
    private val dslContext: DSLContext,
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
    private val permissionService: PermissionService,
    private val configCacheService: ConfigCacheService,
    private val redisOperation: RedisOperation
) {

    private val objectMapper = ObjectMapper()

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
        saveWorkspaceRecordTicket(workspaceName, WorkspaceRecordTicketType.RECORD)

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
        userId: String,
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
            expiredInSecond = 7 * 24 * 3600
        )
        return Pair(
            true,
            remotedevBkRepoClient.repoStreamCreate(
                region = region,
                projectId = projectId,
                repoName = genRepoName(workspaceName),
                userId = enableUser
            ) + "&skToken=$token&recordUser=$userId"
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

    fun updateApprovalRecordViewPermission(
        userId: String,
        workspaceName: String
    ) {
        val record =
            workspaceDao.fetchAnyWorkspace(dslContext, workspaceName = workspaceName) ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        approvalRecordViewCallback(record.projectId, userId, workspaceName)
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
            expiredDays = configCacheService.get(REMOTEDEV_WORKSPACE_USER_APPROVAL_EXPIRED_DAYS)?.toLongOrNull() ?: 7L
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
                    ),
                    NodeSearchRulesItem(
                        field = "fullPath",
                        value = "*.mp4",
                        operation = "MATCH"
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
                stopTime = it.metadata?.mediaStopTime,
                fileSize = it.size,
                // TODO: 未来有了参数再加
                recordUser = ""
            )
        }
        return Page(
            page = resp.pageNumber,
            pageSize = resp.pageSize,
            count = resp.totalRecords,
            records = data
        )
    }

    fun getUserWorkspaceRecordPermission(userId: String, workspaceName: String): UserWorkspaceRecordPermissionInfo {
        val r = workspaceWindowsDao.fetchAnyWorkspaceWindowsInfo(dslContext, workspaceName)
            ?: throw ErrorCodeException(
                errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                params = arrayOf(workspaceName)
            )
        val enableRecord = r.enableRecordUser
        val record = workspaceRecordUserApprovalDao.fetchAnyApproval(dslContext, workspaceName, userId)
            ?: return UserWorkspaceRecordPermissionInfo(
                enableRecord = !enableRecord.isNullOrBlank(),
                viewPermission = false,
                viewPermissionEndTime = null
            )
        val endTime = record.updateTime.plusDays(
            configCacheService.get(REMOTEDEV_WORKSPACE_USER_APPROVAL_EXPIRED_DAYS)?.toLongOrNull() ?: 7L
        )
        return UserWorkspaceRecordPermissionInfo(
            enableRecord = !enableRecord.isNullOrBlank(),
            viewPermission = endTime > LocalDateTime.now(),
            viewPermissionEndTime = endTime.timestampmilli()
        )
    }

    fun saveWorkspaceRecordTicket(workspaceName: String, type: WorkspaceRecordTicketType) {
        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        workspaceRecordTicketDao.create(
            dslContext = dslContext,
            workspaceName = workspaceName,
            cert = BkCryptoUtil.encryptSm4ButAes(aesKey, Base64.getEncoder().encodeToString(random)),
            type = type
        )
    }

    fun getWorkspaceRecordTicket(workspaceName: String, token: String, type: WorkspaceRecordTicketType): String {
        val checkedToken = permissionService.checkAndGetUser1PasswordNoDelete(token)
        if (checkedToken.workspaceName != workspaceName) {
            throw OperationException("Token verification failed.Please reapply for authorization.")
        }
        val record = workspaceRecordTicketDao.fetchAny(dslContext, workspaceName, type) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )
        // 如果传入了type参数，验证type是否匹配
        if (record.type != type.name) {
            throw OperationException("Type verification failed.")
        }
        return BkCryptoUtil.decryptSm4OrAes(aesKey, record.cert)
    }

    /**
     * 获取THUMBNAIL类型的加密密钥
     * 密钥格式：{"key": {真实加密密钥}, "timestamp": {秒级时间戳}, "expiredSeconds": {过期秒数}}
     * 使用RSA公钥加密，并在有效期内缓存
     *
     * @param workspaceName 工作空间名称
     * @param expiredSeconds 过期秒数，默认600秒（10分钟）
     * @return RSA加密后的密钥（Base64编码）
     */
    fun getThumbnailEncryptedTicket(workspaceName: String, expiredSeconds: Long? = null): String {
        // 1. 检查缓存
        val actualExpiredSeconds = expiredSeconds ?: 600L // 默认10分钟有效期
        val cacheKey = "$THUMBNAIL_ENCRYPTED_TICKET_CACHE_KEY_PREFIX$workspaceName:$actualExpiredSeconds"
        val cachedTicket = redisOperation.get(cacheKey)
        if (!cachedTicket.isNullOrBlank()) {
            return cachedTicket
        }

        // 2. 获取工作空间信息以确定region
        val workspaceInfo = workspaceJoinDao.fetchAnyWindowsWorkspace(
            dslContext = dslContext,
            workspaceName = workspaceName
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            params = arrayOf(workspaceName)
        )

        // 3. 根据hostIp确定region
        val region = genRegion(workspaceInfo.hostIp)

        // 4. 获取对应region的RSA公钥
        val rsaPublicKeyStr = bkRepoConfig.getRegionConfig(region).rsaPublicKey
        if (rsaPublicKeyStr.isBlank()) {
            throw OperationException("RSA public key not configured for region: ${region.name}")
        }

        // 5. 获取THUMBNAIL类型的密钥
        val record = workspaceRecordTicketDao.fetchAny(
            dslContext = dslContext,
            workspaceName = workspaceName,
            type = WorkspaceRecordTicketType.THUMBNAIL,
            enable = true
        ) ?: throw ErrorCodeException(
            errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
            defaultMessage = "Thumbnail ticket not found for workspace: $workspaceName"
        )

        // 6. 解密密钥
        val decryptedKey = BkCryptoUtil.decryptSm4OrAes(aesKey, record.cert)

        // 7. 构建JSON格式的数据
        val currentTimestamp = System.currentTimeMillis() / 1000 // 秒级时间戳
        val ticketData = mapOf(
            "key" to decryptedKey,
            "timestamp" to currentTimestamp,
            "expiredSeconds" to actualExpiredSeconds
        )
        val ticketJson = objectMapper.writeValueAsString(ticketData)

        // 8. 使用RSA公钥加密
        val rsaPublicKey = RsaUtil.generatePublicKey(rsaPublicKeyStr.toByteArray())
        val encryptedTicket = RsaUtil.rsaEncrypt(ticketJson, rsaPublicKey)

        // 9. 缓存加密后的密钥（有效期内避免频繁生成）
        redisOperation.set(
            key = cacheKey,
            value = encryptedTicket,
            expiredInSecond = actualExpiredSeconds
        )

        return encryptedTicket
    }

    /**
     * 更新工作空间录屏密钥的启用状态
     *
     * @param workspaceName 工作空间名称
     * @param type 密钥类型
     * @param enable 是否启用
     * @return 是否更新成功
     */
    fun updateWorkspaceRecordTicketEnable(
        workspaceName: String,
        type: WorkspaceRecordTicketType,
        enable: Boolean
    ): Boolean {
        // 更新enable状态
        val updateCount = workspaceRecordTicketDao.updateEnable(
            dslContext = dslContext,
            workspaceName = workspaceName,
            type = type,
            enable = enable
        )

        // 如果是THUMBNAIL类型，清除缓存
        if (type == WorkspaceRecordTicketType.THUMBNAIL) {
            val cacheKey = "$THUMBNAIL_ENCRYPTED_TICKET_CACHE_KEY_PREFIX$workspaceName"
            redisOperation.delete(cacheKey)
        }

        return updateCount > 0
    }

    companion object {

        private const val BKREPO_WORKSPACE_REPONAME_PREFIX = "REMOTEDEV_"
        private const val THUMBNAIL_ENCRYPTED_TICKET_CACHE_KEY_PREFIX = "remotedev:thumbnail:encrypted-ticket:"

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
