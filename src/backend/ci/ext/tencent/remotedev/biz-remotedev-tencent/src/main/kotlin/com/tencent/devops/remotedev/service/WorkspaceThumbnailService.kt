package com.tencent.devops.remotedev.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.remotedev.config.BkRepoRegion
import com.tencent.devops.remotedev.constant.BkRepoConstants
import com.tencent.devops.remotedev.constant.ThumbnailRedisKeys
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.service.client.RemotedevBkRepoClient
import com.tencent.devops.remotedev.service.client.StartCloudClient
import java.util.concurrent.TimeUnit
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service

/**
 * 云桌面截图缩略图业务服务
 */
@Service
class WorkspaceThumbnailService @Autowired constructor(
    private val dslContext: DSLContext,
    private val remotedevBkRepoClient: RemotedevBkRepoClient,
    private val startCloudClient: StartCloudClient,
    private val redisOperation: RedisOperation,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val windowsResourceConfigService: WindowsResourceConfigService,
    @Qualifier("screenshotTaskExecutor")
    private val screenshotTaskExecutor: ThreadPoolTaskExecutor
) {

    /**
     * 批量获取截图地址（同步方法，快速返回）
     *
     * @param userId 用户ID
     * @param workspaceNames 工作空间名称列表
     * @return 工作空间名称到截图下载地址的映射
     */
    fun batchGetThumbnails(userId: String, workspaceNames: List<String>): Map<String, String> {
        if (workspaceNames.isEmpty()) {
            return emptyMap()
        }

        logger.info("batch get thumbnails: userId=$userId, workspaceNames=$workspaceNames")

        // 步骤1: 批量查询Redis缓存
        val cacheKeys = workspaceNames.map { ThumbnailCacheKey(it).toRedisKey() }
        val cachedResults = mutableMapOf<String, String>()

        cacheKeys.forEachIndexed { index, key ->
            val cachedUrl = redisOperation.get(key)
            if (!cachedUrl.isNullOrBlank()) {
                cachedResults[workspaceNames[index]] = cachedUrl
            }
        }

        logger.debug("cache hit: ${cachedResults.size}/${workspaceNames.size}")

        // 步骤2: 为缓存未命中的工作空间生成临时链接
        val missedWorkspaceNames = workspaceNames.filter { !cachedResults.containsKey(it) }
        val newResults = mutableMapOf<String, String>()

        if (missedWorkspaceNames.isNotEmpty()) {
            // 预加载zone配置，避免在循环中重复查询数据库
            val zoneConfigs = loadZoneConfigs()

            // 批量查询工作空间信息（一次性查询所有需要的workspace）
            val workspaceInfoMap = workspaceJoinDao.fetchWindowsWorkspaces(
                dslContext = dslContext,
                workspaceNames = missedWorkspaceNames.toSet(),
                checkField = listOf(
                    TWorkspace.T_WORKSPACE.NAME,
                    TWorkspace.T_WORKSPACE.PROJECT_ID,
                    TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP
                )
            ).associateBy { it.workspaceName }

            missedWorkspaceNames.forEach { workspaceName ->
                try {
                    // 从批量查询结果中获取工作空间信息
                    val workspaceInfo = workspaceInfoMap[workspaceName]
                    if (workspaceInfo == null) {
                        logger.warn("工作空间不存在: workspaceName=$workspaceName")
                        return@forEach
                    }

                    val region = genRegion(workspaceInfo.hostIp, zoneConfigs)

                    // 生成临时下载Token
                    val token = remotedevBkRepoClient.createTemporaryAccessToken(
                        region = region,
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                        fullPathSet = listOf("/screenshot/$workspaceName.jpg"),
                        expireSeconds = BkRepoConstants.TOKEN_EXPIRE_SECONDS,
                        type = BkRepoConstants.TOKEN_TYPE_DOWNLOAD,
                        userId = userId
                    )

                    // 构建下载URL
                    val downloadUrl = buildDownloadUrl(
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                        workspaceName = workspaceName,
                        token = token
                    )

                    // 更新缓存
                    val cacheKey = ThumbnailCacheKey(workspaceName).toRedisKey()
                    redisOperation.set(
                        key = cacheKey,
                        value = downloadUrl,
                        expiredInSecond = ThumbnailRedisKeys.THUMBNAIL_TTL_SECONDS.toLong()
                    )

                    newResults[workspaceName] = downloadUrl
                } catch (e: Exception) {
                    logger.error("generate thumbnail url failed: workspaceName=$workspaceName", e)
                }
            }
        }

        // 步骤3: 合并结果
        val finalResults = cachedResults + newResults

        logger.info("batch get thumbnails success: total=${finalResults.size}")

        return finalResults
    }

    /**
     * 异步处理截图上传任务
     *
     * @param workspaceNames 工作空间名称列表
     */
    fun processScreenshotUpload(workspaceNames: List<String>) {
        if (workspaceNames.isEmpty()) {
            return
        }

        screenshotTaskExecutor.submit {
            logger.info("start process screenshot upload: workspaceNames=$workspaceNames")

            // 预加载zone配置，避免在循环中重复查询数据库
            val zoneConfigs = loadZoneConfigs()

            // 批量查询工作空间信息（一次性查询所有需要的workspace）
            val workspaceInfoMap = workspaceJoinDao.fetchWindowsWorkspaces(
                dslContext = dslContext,
                workspaceNames = workspaceNames.toSet(),
                checkField = listOf(
                    TWorkspace.T_WORKSPACE.NAME,
                    TWorkspace.T_WORKSPACE.PROJECT_ID,
                    TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP
                )
            ).associateBy { it.workspaceName }

            // 收集所有成功生成的cgsId和uploadUrl
            val uploadUrls = mutableMapOf<String, String>()

            workspaceNames.forEach { workspaceName ->
                try {
                    // 从批量查询结果中获取工作空间信息
                    val workspaceInfo = workspaceInfoMap[workspaceName]
                    if (workspaceInfo == null) {
                        logger.warn("工作空间不存在: workspaceName=$workspaceName")
                        return@forEach
                    }

                    val cgsId = workspaceInfo.hostIp
                    if (cgsId.isNullOrBlank()) {
                        logger.warn("工作空间hostIp为空: workspaceName=$workspaceName")
                        return@forEach
                    }

                    val region = genRegion(cgsId, zoneConfigs)

                    // 确保仓库存在（带缓存优化）
                    ensureRepoExists(
                        region = region,
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME
                    )

                    // 生成上传Token
                    val uploadToken = remotedevBkRepoClient.createTemporaryAccessToken(
                        region = region,
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                        fullPathSet = listOf("/screenshot/$workspaceName.jpg"),
                        expireSeconds = BkRepoConstants.TOKEN_EXPIRE_SECONDS,
                        type = BkRepoConstants.TOKEN_TYPE_ALL,
                        userId = SYSTEM_USER
                    )

                    // 构建上传URL
                    val uploadUrl = buildUploadUrl(
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                        workspaceName = workspaceName,
                        token = uploadToken
                    )

                    // 收集cgsId和uploadUrl
                    uploadUrls[cgsId] = uploadUrl

                    logger.info("prepare screenshot upload success: workspaceName=$workspaceName, cgsId=$cgsId")
                } catch (e: Exception) {
                    logger.error("prepare screenshot upload failed: workspaceName=$workspaceName", e)
                }
            }

            // 批量通知CDS执行截图上传
            if (uploadUrls.isNotEmpty()) {
                try {
                    startCloudClient.notifyScreenshotUpload(
                        uploadUrls = uploadUrls
                    )
                } catch (e: Exception) {
                    logger.error("batch notify screenshot upload failed: cgsIds=${uploadUrls.keys}", e)
                }
            }

            logger.info("finish process screenshot upload: workspaceNames=$workspaceNames")
        }
    }

    /**
     * 拼接BkRepo临时下载链接
     *
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param workspaceName 工作空间名称
     * @param token 临时访问Token
     * @return 完整的下载URL
     */
    private fun buildDownloadUrl(
        projectId: String,
        repoName: String,
        workspaceName: String,
        token: String
    ): String {
        return "/generic/temporary/download/$projectId/$repoName/screenshot/$workspaceName.jpg?token=$token"
    }

    /**
     * 拼接BkRepo临时上传链接
     *
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param workspaceName 工作空间名称
     * @param token 临时访问Token
     * @return 完整的上传URL
     */
    private fun buildUploadUrl(
        projectId: String,
        repoName: String,
        workspaceName: String,
        token: String
    ): String {
        return "/generic/temporary/upload/$projectId/$repoName/screenshot/$workspaceName.jpg?token=$token"
    }

    /**
     * 预加载Zone配置数据
     * 避免在循环中重复查询数据库
     *
     * @return Zone配置数据容器
     */
    private fun loadZoneConfigs(): ZoneConfigs {
        val defaultZoneConfig = windowsResourceConfigService.getAllZone()
            .associateBy { it.zoneShortName }
        val specZoneConfig = windowsResourceConfigService.getAllSpecZone()
            .associateBy { it.zoneShortName }
        return ZoneConfigs(defaultZoneConfig, specZoneConfig)
    }

    /**
     * 确保BkRepo仓库存在，如果不存在则创建并配置权限
     * 使用Caffeine本地缓存，10分钟内只检查一次
     *
     * @param region BkRepo区域
     * @param projectId 项目ID
     * @param repoName 仓库名称
     */
    private fun ensureRepoExists(
        region: BkRepoRegion,
        projectId: String,
        repoName: String
    ) {
        val cacheKey = "${region.name}:$projectId:$repoName"
        
        // 从缓存中获取仓库存在状态，如果缓存未命中则检查并创建
        val repoExists = repoExistCache.get(cacheKey) { key ->
            logger.info("checking repo existence: $key")
            
            val exists = remotedevBkRepoClient.checkRepoExist(
                region = region,
                projectId = projectId,
                repoName = repoName,
                userId = SYSTEM_USER
            )
            
            if (!exists) {
                logger.info("repo not exists, create it: projectId=$projectId, repoName=$repoName, region=${region.name}")
                
                // 创建仓库
                remotedevBkRepoClient.createRepo(
                    region = region,
                    projectId = projectId,
                    repoName = repoName,
                    userId = SYSTEM_USER
                )
                
                // 切换权限模式为STRICT
                remotedevBkRepoClient.changeRepoToggle(
                    region = region,
                    projectId = projectId,
                    repoName = repoName,
                    userId = SYSTEM_USER
                )
                
                // 创建仓库权限
                remotedevBkRepoClient.createRepoPermission(
                    region = region,
                    projectId = projectId,
                    repoName = repoName,
                    userId = SYSTEM_USER
                )
                
                logger.info("repo created successfully: projectId=$projectId, repoName=$repoName, region=${region.name}")
            }
            
            // 返回true表示仓库已存在（或已创建）
            true
        }
        
        logger.debug("repo exists check result: $cacheKey = $repoExists")
    }

    /**
     * 根据hostIp生成BkRepo Region
     * 参照WorkspaceRecordService#genRegion实现
     *
     * @param hostIp 主机IP地址
     * @param zoneConfigs 预加载的Zone配置数据
     * @return BkRepo区域配置
     */
    private fun genRegion(hostIp: String?, zoneConfigs: ZoneConfigs): BkRepoRegion {
        val zone = if (hostIp != null) {
            /*后续直接取windows表中的zoneId，不通过ip进行解析*/
            val zoneId = hostIp.substringBefore(".")
            zoneConfigs.specZoneConfig[zoneId] ?: zoneConfigs.defaultZoneConfig[zoneId.removeSuffixNumb()]
        } else {
            null
        }
        val region = when (zone?.type) {
            WindowsResourceZoneConfigType.CSIG_USE -> BkRepoRegion.CSIG
            else -> BkRepoRegion.DEVX
        }
        return region
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WorkspaceThumbnailService::class.java)
        private const val SYSTEM_USER = "admin"
        
        /**
         * BkRepo仓库存在性缓存
         * Key格式: "region:projectId:repoName"
         * Value: true表示仓库已存在
         * 缓存时间: 10分钟
         */
        private val repoExistCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build<String, Boolean>()

        /**
         * 移除字符串末尾的数字
         */
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

/**
 * Zone配置数据容器
 * 用于缓存zone配置，避免重复查询数据库
 */
private data class ZoneConfigs(
    val defaultZoneConfig: Map<String, WindowsResourceZoneConfig>,
    val specZoneConfig: Map<String, WindowsResourceZoneConfig>
)

/**
 * 缓存Key数据类
 */
data class ThumbnailCacheKey(
    val workspaceName: String
) {
    /**
     * 生成Redis Key格式
     */
    fun toRedisKey(): String {
        return "${ThumbnailRedisKeys.THUMBNAIL_PREFIX}$workspaceName"
    }
}
