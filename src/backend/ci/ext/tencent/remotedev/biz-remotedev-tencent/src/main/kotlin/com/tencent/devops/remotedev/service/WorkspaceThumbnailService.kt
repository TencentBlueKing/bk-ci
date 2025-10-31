package com.tencent.devops.remotedev.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.model.remotedev.tables.TWorkspace
import com.tencent.devops.model.remotedev.tables.TWorkspaceWindows
import com.tencent.devops.remotedev.config.BkRepoRegion
import com.tencent.devops.remotedev.config.RemoteDevBkRepoConfig
import com.tencent.devops.remotedev.constant.BkRepoConstants
import com.tencent.devops.remotedev.constant.ThumbnailRedisKeys
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfig
import com.tencent.devops.remotedev.pojo.WindowsResourceZoneConfigType
import com.tencent.devops.remotedev.pojo.startcloud.FetchDesktopThumbnailReq
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
    private val permissionService: PermissionService,
    private val startCloudClient: StartCloudClient,
    private val redisOperation: RedisOperation,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val bkRepoConfig: RemoteDevBkRepoConfig,
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
    fun batchGetThumbnails(
        userId: String,
        workspaceNames: List<String>,
        width: Int,
        high: Int,
        screenId: Int
    ): Map<String, String> {
        if (workspaceNames.isEmpty()) {
            return emptyMap()
        }

        logger.info("batch get thumbnails: userId=$userId, workspaceNames=$workspaceNames")

        val cachedResults = mutableMapOf<String, String>()

        // 第一步： 过滤出相应的分辨率, 从小分辨率到大分辨率有一个渐变效果会体现出来
        val workspace2Size = workspaceNames.map { workspaceName ->
            val cacheKey = ThumbnailCacheKey(workspaceName)
            val maxSize = redisOperation.get(cacheKey.toDownloadLimitRedisKey(userId))
                ?.split("-")?.let { ScreenSize(it[0].toInt(), it[1].toInt()) }
            if (maxSize != null && width * high <= maxSize.width * maxSize.high) {
                return@map Pair(workspaceName, ScreenSize(width, high))
            }
            redisOperation.set(
                key = cacheKey.toDownloadLimitRedisKey(userId),
                value = "$width-$high",
                expiredInSecond = ThumbnailRedisKeys.THUMBNAIL_DOWNLOAD_LIMIT_TTL_SECONDS
            )
            Pair(workspaceName, maxSize ?: ScreenSize(width, high))
        }.toMap()

        // 第二步： 过滤出未命中的工作空间
        workspace2Size.forEach { (workspaceName, size) ->
            val cacheKey = ThumbnailCacheKey(workspaceName)
            val cachedUrl = redisOperation.get(cacheKey.toDownloadRedisKey(screenId, size))
            if (!cachedUrl.isNullOrBlank()) {
                cachedResults[workspaceName] = cachedUrl
            }
        }

        logger.debug("cache hit: ${cachedResults.size}/${workspaceNames.size}")

        // 第三步: 为缓存未命中的工作空间生成临时链接
        val missedWorkspaceNames = workspace2Size.filter { !cachedResults.containsKey(it.key) }
        val newResults = mutableMapOf<String, String>()

        if (missedWorkspaceNames.isNotEmpty()) {
            // 预加载zone配置，避免在循环中重复查询数据库
            val zoneConfigs = loadZoneConfigs()

            // 批量查询工作空间信息（一次性查询所有需要的workspace）
            val workspaceInfoMap = workspaceJoinDao.fetchWindowsWorkspaces(
                dslContext = dslContext,
                workspaceNames = missedWorkspaceNames.keys.toSet(),
                checkField = listOf(
                    TWorkspace.T_WORKSPACE.NAME,
                    TWorkspace.T_WORKSPACE.PROJECT_ID,
                    TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP
                )
            ).associateBy { it.workspaceName }

            missedWorkspaceNames.forEach { (workspaceName, size) ->
                try {
                    // 从批量查询结果中获取工作空间信息
                    val workspaceInfo = workspaceInfoMap[workspaceName]
                    if (workspaceInfo == null) {
                        logger.warn("workspace not exist: workspaceName=$workspaceName")
                        return@forEach
                    }

                    // 检查是否有查看权限
                    if (!permissionService.hasViewerPermission(userId, workspaceName, workspaceInfo.projectId)) {
                        logger.warn("user not viewer permission: userId=$userId, workspaceName=$workspaceName")
                        return@forEach
                    }

                    val region = genRegion(workspaceInfo.hostIp, zoneConfigs)

                    // 生成临时下载Token
                    val token = remotedevBkRepoClient.createTemporaryAccessToken(
                        region = region,
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                        fullPathSet = listOf("/screenshot/$workspaceName-${size.width}x${size.high}.jpg"),
                        expireSeconds = BkRepoConstants.TOKEN_EXPIRE_SECONDS,
                        type = BkRepoConstants.TOKEN_TYPE_DOWNLOAD,
                        userId = SYSTEM_USER
                    )

                    // 构建下载URL
                    val downloadUrl = buildDownloadUrl(
                        host = bkRepoConfig.getRegionConfig(region).url,
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                        workspaceName = workspaceName,
                        token = token,
                        size = size
                    )

                    // 更新缓存
                    val cacheKey = ThumbnailCacheKey(workspaceName).toDownloadRedisKey(screenId, size)
                    redisOperation.set(
                        key = cacheKey,
                        value = downloadUrl,
                        expiredInSecond = ThumbnailRedisKeys.THUMBNAIL_TTL_SECONDS
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
     * @param width 缩略图宽度
     * @param high 缩略图高度
     * @param jpegQuality JPEG图片质量
     */
    fun processScreenshotUpload(
        userId: String,
        workspaceNames: Set<String>,
        width: Int,
        high: Int,
        jpegQuality: Int,
        screenId: Int
    ) {
        if (workspaceNames.isEmpty()) {
            return
        }

        screenshotTaskExecutor.submit {
            logger.info("start process screenshot upload: workspaceNames=$workspaceNames")
            val size = ScreenSize(width, high)

            // 预加载zone配置，避免在循环中重复查询数据库
            val zoneConfigs = loadZoneConfigs()

            // 批量查询工作空间信息（一次性查询所有需要的workspace）
            val workspaceInfoMap = workspaceJoinDao.fetchWindowsWorkspaces(
                dslContext = dslContext,
                workspaceNames = workspaceNames,
                checkField = listOf(
                    TWorkspace.T_WORKSPACE.NAME,
                    TWorkspace.T_WORKSPACE.PROJECT_ID,
                    TWorkspaceWindows.T_WORKSPACE_WINDOWS.HOST_IP
                )
            ).associateBy { it.workspaceName }

            // 收集所有成功生成的截图上传请求
            val uploadRequests = mutableListOf<FetchDesktopThumbnailReq>()

            workspaceNames.forEach { workspaceName ->
                try {
                    // 2s内同画质只允许上传一次
                    val limitKey = ThumbnailCacheKey(workspaceName).toUploadLimitRedisKey(screenId, size)
                    val limit = redisOperation.get(limitKey)
                    if (limit != null) {
                        return@forEach
                    }
                    redisOperation.set(
                        key = limitKey,
                        value = userId,
                        expiredInSecond = ThumbnailRedisKeys.THUMBNAIL_UPLOAD_LIMIT_TTL_SECONDS
                    )
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
                    val cacheKey = ThumbnailCacheKey(workspaceName).toUploadRedisKey(screenId, size)
                    val uploadToken = redisOperation.get(cacheKey)
                        ?: remotedevBkRepoClient.createTemporaryAccessToken(
                            region = region,
                            projectId = workspaceInfo.projectId,
                            repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                            fullPathSet = listOf("/screenshot/$workspaceName-${size.width}x${size.high}.jpg"),
                            expireSeconds = BkRepoConstants.TOKEN_EXPIRE_SECONDS,
                            type = BkRepoConstants.TOKEN_TYPE_ALL,
                            userId = SYSTEM_USER
                        ).also {
                            redisOperation.set(
                                key = cacheKey,
                                value = it,
                                expiredInSecond = ThumbnailRedisKeys.THUMBNAIL_TTL_SECONDS
                            )
                        }

                    // 构建上传URL
                    val uploadUrl = buildUploadUrl(
                        host = bkRepoConfig.getRegionConfig(region).url,
                        projectId = workspaceInfo.projectId,
                        repoName = BkRepoConstants.REMOTE_DEV_REPO_NAME,
                        workspaceName = workspaceName,
                        token = uploadToken,
                        size = size
                    )

                    // 构建截图上传请求对象
                    val uploadRequest = FetchDesktopThumbnailReq(
                        userId = userId,
                        cdsId = cgsId,
                        width = width,
                        high = high,
                        screenId = screenId,
                        jpegQuality = jpegQuality,
                        jpegUrl = uploadUrl
                    )
                    uploadRequests.add(uploadRequest)

                    logger.info("prepare screenshot upload success: workspaceName=$workspaceName, cgsId=$cgsId")
                } catch (e: Exception) {
                    logger.error("prepare screenshot upload failed: workspaceName=$workspaceName", e)
                }
            }

            // 批量通知CDS执行截图上传
            if (uploadRequests.isNotEmpty()) {
                try {
                    startCloudClient.notifyScreenshotUpload(
                        requests = uploadRequests
                    )
                } catch (e: Exception) {
                    logger.error("batch notify screenshot upload failed: cdsIds=${uploadRequests.map { it.cdsId }}", e)
                }
            }

            logger.info("finish process screenshot upload: workspaceNames=$workspaceNames")
        }
    }

    /**
     * 拼接BkRepo临时下载链接
     *
     * @param host 域名
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param workspaceName 工作空间名称
     * @param token 临时访问Token
     * @return 完整的下载URL
     */
    private fun buildDownloadUrl(
        host: String,
        projectId: String,
        repoName: String,
        workspaceName: String,
        token: String,
        size: ScreenSize
    ): String {
        return "$host/generic/temporary/download/$projectId/$repoName/screenshot" +
            "/$workspaceName-${size.width}x${size.high}.jpg?token=$token"
    }

    /**
     * 拼接BkRepo临时上传链接
     *
     * @param host 域名
     * @param projectId 项目ID
     * @param repoName 仓库名称
     * @param workspaceName 工作空间名称
     * @param token 临时访问Token
     * @return 完整的上传URL
     */
    private fun buildUploadUrl(
        host: String,
        projectId: String,
        repoName: String,
        workspaceName: String,
        token: String,
        size: ScreenSize
    ): String {
        return "$host/generic/temporary/upload/$projectId/$repoName/screenshot" +
            "/$workspaceName-${size.width}x${size.high}.jpg?token=$token"
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
            WindowsResourceZoneConfigType.DEVCLOUD -> BkRepoRegion.DEVCLOUD
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

data class ScreenSize(
    val width: Int,
    val high: Int
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
    fun toDownloadRedisKey(screenId: Int, size: ScreenSize): String {
        return "${ThumbnailRedisKeys.THUMBNAIL_DOWNLOAD_PREFIX}$workspaceName-$screenId-${size.width}x${size.high}"
    }

    /**
     * 存储10秒内同一个用户+机器的最大分辨率
     */
    fun toDownloadLimitRedisKey(userId: String): String {
        return "${ThumbnailRedisKeys.THUMBNAIL_DOWNLOAD_LIMIT_PREFIX}$workspaceName:$userId"
    }

    /**
     * 生成Redis Key格式
     */
    fun toUploadRedisKey(screenId: Int, size: ScreenSize): String {
        return "${ThumbnailRedisKeys.THUMBNAIL_UPLOAD_PREFIX}$workspaceName-$screenId-${size.width}x${size.high}"
    }

    /**
     * 2秒内同样的屏幕id和分辨率只请求一次
     */
    fun toUploadLimitRedisKey(screenId: Int, size: ScreenSize): String {
        return "${ThumbnailRedisKeys.THUMBNAIL_UPLOAD_LIMIT_PREFIX}$workspaceName-$screenId-${size.width}x${size.high}"
    }
}
