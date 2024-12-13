package com.tencent.devops.remotedev.filter.impl

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.ClientDao
import com.tencent.devops.remotedev.dao.ClientVersionDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.filter.ApiFilter
import com.tencent.devops.remotedev.pojo.WorkspaceSearch
import com.tencent.devops.remotedev.pojo.common.QueryType
import com.tencent.devops.remotedev.pojo.common.RemoteDevNotifyType
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.redis.RedisKeys.CLIENT_VERSION_LIMIT
import com.tencent.devops.remotedev.service.redis.RedisKeys.CLIENT_VERSION_WARNING
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import com.tencent.devops.remotedev.service.workspace.NotifyControl.Companion.CLIENT_VERSION_WARNING_NOTIFY
import java.util.concurrent.TimeUnit
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.PreMatching
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

@Provider
@PreMatching
@RequestFilter
class ClientVersionFilter constructor(
    private val cacheService: ConfigCacheService,
    private val clientVersionDao: ClientVersionDao,
    private val dslContext: DSLContext,
    private val notifyControl: NotifyControl,
    private val clientDao: ClientDao,
    private val workspaceJoinDao: WorkspaceJoinDao
) : ApiFilter {
    companion object {
        private val logger = LoggerFactory.getLogger(ClientVersionFilter::class.java)
        private const val BK_CI_CLIENT_VERSION = "BK-CI-CLIENT-VERSION"
        private const val HEADER_IP = "x-client-ip"
        private const val HEADER_MAC_ADDRESS = "BK-CI-CLIENT-MAC"
        private const val HEADER_MAC_OS = "BK-CI-CLIENT-OS"
        private const val BK_CI_CLIENT_START_VERSION = "BK-CI-CLIENT-START-VERSION"

        private fun String.format(): String {
            if (this.trim() == "null" || this.isBlank()) {
                return ""
            }
            return this
        }
    }

    private val cache = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(2)
        .build<String/*key*/, List<Int>> { key ->
            cacheService.get(key)?.split(".")?.map { it.toInt() } ?: emptyList()
        }

    lateinit var clientVersion: MutableMap<String, String>

    enum class ApiType(val startContextPath: String, val verify: Boolean) {

        USER_SETTINGS("remotedev/settings", true),
        START_CLOUD_WORKSPACE_DETAIL("workspaces/start_cloud_workspace_detail", true),
        USER("api/user/", false),
        DESKTOP("/api/desktop/", false),
        EXTERNAL("/api/external/", false),
        REMOTEDEV("/api/remotedev/", false),
        SERVICE("/api/service/", false),
        OP("/api/op/", false),
        SWAGGER("/api/swagger.json", false);

        companion object {
            fun parseType(path: String): ApiType? {
                values().forEach { type ->
                    if (path.contains(other = type.startContextPath, ignoreCase = true)) {
                        return type
                    }
                }
                return null
            }
        }
    }

    @Suppress("ComplexMethod")
    override fun verify(requestContext: ContainerRequestContext): Boolean {
        // path为为空的时候，直接退出
        val path = requestContext.uriInfo.requestUri.path
        // 开关
        if (cacheService.get(RedisKeys.REDIS_CLIENT_VERSION_CHECK).toString() == false.toString()) return true
        // 判断是否为合法的路径
        val apiType = ApiType.parseType(path) ?: return true
        // 如果是op的接口访问直接跳过jwt认证
        if (!apiType.verify) return true
        val version = requestContext.headers[BK_CI_CLIENT_VERSION]?.get(0)
        val split = version?.substringBefore("-")?.split(".") ?: kotlin.run {
            logger.info(
                "user(${requestContext.headers[AUTH_HEADER_USER_ID]}) request" +
                    " $path not have $BK_CI_CLIENT_VERSION,return error."
            )
            return false
        }
        val user = requestContext.headers[AUTH_HEADER_USER_ID]?.get(0).toString()
        kotlin.runCatching {
            recordClientVersion(
                ip = requestContext.headers[HEADER_IP]?.get(0).toString(),
                user = user,
                version = version.toString(),
                macAddress = requestContext.headers[HEADER_MAC_ADDRESS]?.get(0).toString(),
                startVersion = requestContext.headers[BK_CI_CLIENT_START_VERSION]?.get(0) ?: "",
                os = requestContext.headers[HEADER_MAC_OS]?.get(0) ?: ""
            )
        }.onFailure { logger.warn("recordClientVersion error ${it.message}", it) }

        if (checkClientVersionWarning(split = split)) {
            notifyControl.notify4User(
                userIds = mutableSetOf(user),
                notifyType = mutableSetOf(RemoteDevNotifyType.CLIENT_PUSH, RemoteDevNotifyType.EMAIL),
                bodyParams = mutableMapOf(
                    "version" to version,
                    "notifyTemplateCode" to CLIENT_VERSION_WARNING_NOTIFY
                )
            )
        }

        cache.get(CLIENT_VERSION_LIMIT)?.forEachIndexed { index, s ->
            if (split.lastIndex < index) {
                return false
            }
            val v = split[index].toIntOrNull()
            when {
                v == null -> return false
                v < s -> return false
                v == s -> return@forEachIndexed
                v > s -> return true
            }
        }
        return true
    }

    /*
     * 检查是否需要告警
     * true： 告警
     * false： 不告警
     * */
    private fun checkClientVersionWarning(split: List<String>): Boolean {
        kotlin.run {
            cache.get(CLIENT_VERSION_WARNING)?.forEachIndexed { index, s ->
                if (split.lastIndex < index) {
                    return true
                }
                val v = split[index].toIntOrNull()
                when {
                    v == null -> return true
                    v < s -> return true
                    v == s -> return@forEachIndexed
                    v > s -> return false
                }
            }
        }
        return false
    }

    private fun recordClientVersion(
        ip: String,
        user: String,
        version: String,
        macAddress: String,
        startVersion: String,
        os: String
    ) {
        if (!this::clientVersion.isInitialized) {
            clientVersion = clientVersionDao.fetchAll(dslContext)
                .associateByTo(mutableMapOf(), { "${it.first}-${it.second}" }, { it.third })
        }
        val recordVersion = clientVersion["$ip-$user"]
        logger.info("recordClientVersion|$ip|$user|$version|$recordVersion|$macAddress|$startVersion")
        if (macAddress.format().isNotBlank()) {
            val currentWorkspaceNames = workspaceJoinDao.limitFetchProjectWorkspace(
                dslContext = dslContext,
                queryType = QueryType.CLIENT,
                limit = PageUtil.convertPageSizeToSQLLimit(1, 1000),
                search = WorkspaceSearch(
                    viewers = listOf(user),
                    onFuzzyMatch = false
                )
            )?.map { it.workspaceName } ?: emptyList()
            clientDao.createOrUpdate(
                dslContext = dslContext,
                macAddress = macAddress,
                currentUserId = user.format(),
                version = version.format(),
                startVersion = startVersion.format(),
                currentProjectIds = workspaceJoinDao.fetchProjectFromUser(dslContext, user),
                currentWorkspaceNames = currentWorkspaceNames.toSet(),
                os = OS.parse(os)
            )
        } else {
            logger.warn("recordClientVersion macAddress is null")
        }
        when {
            recordVersion == null -> {
                val count = clientVersionDao.create(
                    dslContext = dslContext,
                    ip = ip,
                    userId = user,
                    version = version,
                    macAddress = macAddress
                )
                logger.info("init client version record|$ip|$user|$version|$count|$macAddress")
                clientVersion["$ip-$user"] = version
            }

            recordVersion != version -> {
                val count = clientVersionDao.update(
                    dslContext = dslContext,
                    ip = ip,
                    userId = user,
                    version = version,
                    lastVersion = recordVersion,
                    macAddress = macAddress
                )
                logger.info("client update now|$ip|$user|$version|$recordVersion|$count|$macAddress")
                if (count > 0) {
                    clientVersion["$ip-$user"] = version
                } else {
                    clientVersion["$ip-$user"] = clientVersionDao.fetch(
                        dslContext = dslContext, ip = ip, userId = user
                    )!!
                }
            }

            // 只要上报就更新时间
            else -> {
                val key = "$macAddress-$user"
                if (recordClientCache.getIfPresent(key) != null) {
                    return
                }
                recordClientCache.put(key, "")
                clientVersionDao.updateTime(dslContext, ip, macAddress, user, recordVersion)
                logger.info("client update time|$ip|$user|$version|$recordVersion|$macAddress")
            }
        }
    }

    private val recordClientCache: Cache<String, String> = CacheBuilder.newBuilder().maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.MINUTES).build()

    override fun filter(requestContext: ContainerRequestContext) {
        if (!verify(requestContext)) {
            requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(
                        I18nUtil.generateResponseDataObject(
                            messageCode = ErrorCodeEnum.CLIENT_NEED_UPDATED.errorCode,
                            params = arrayOf(cacheService.get(RedisKeys.REDIS_CLIENT_INSTALL_URL).toString()),
                            data = null,
                            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()),
                            defaultMessage = ErrorCodeEnum.CLIENT_NEED_UPDATED.formatErrorMessage
                        )
                    )
                    .build()
            )
            return
        }
    }
}
