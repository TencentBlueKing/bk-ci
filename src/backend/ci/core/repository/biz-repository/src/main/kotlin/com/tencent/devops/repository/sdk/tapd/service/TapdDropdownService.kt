package com.tencent.devops.repository.sdk.tapd.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.scm.pojo.tapd.TapdAuthorizedWorkspaces
import com.tencent.devops.scm.pojo.tapd.TapdStatusOption
import com.tencent.devops.scm.pojo.tapd.TapdWorkspaceOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * TAPD 触发器下拉选项聚合服务
 *
 * 编辑态的数据源聚合全部在这里完成，Webhook 匹配链路不会调用 TAPD OpenAPI。
 */
@Service
class TapdDropdownService @Autowired constructor(
    private val tapdItemService: ITapdItemService
) {

    private val authorizedCache: Cache<String, TapdAuthorizedWorkspaces> = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build()

    private val statusCache: Cache<String, List<TapdStatusOption>> = Caffeine.newBuilder()
        .maximumSize(CACHE_MAX_SIZE)
        .expireAfterWrite(CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES)
        .build()

    /**
     * 当前用户可见且应用已授权的 TAPD 项目下拉选项
     *
     * 未安装/未授权项目不再展示；授权集合未知时返回空列表，避免误展示未授权项目。
     */
    fun workspaceOptions(userId: String): List<TapdWorkspaceOption> {
        if (userId.isBlank()) {
            logger.warn("invalid tapd workspace options query")
            return emptyList()
        }
        return try {
            val authorized = authorizedWorkspaces()
            if (!authorized.known) {
                logger.warn("tapd authorized workspaces unknown, return empty workspace options")
                return emptyList()
            }
            val workspaces = tapdItemService.listParticipantWorkspaces(nick = userId)
            if (workspaces.isEmpty()) {
                logger.warn("tapd participant workspaces is empty")
                return emptyList()
            }
            workspaces.mapNotNull { it.workspace }
                .filter { it.id.isNotBlank() && authorized.workspaceIds.contains(it.id) }
                .distinctBy { it.id }
                .map { workspace ->
                    TapdWorkspaceOption(
                        id = workspace.id,
                        name = workspace.name,
                        status = workspace.status,
                        disabled = false
                    )
                }
        } catch (ignored: Exception) {
            logger.warn("fail to build tapd workspace options", ignored)
            emptyList()
        }
    }

    /**
     * 按项目 ID 与事件类型返回状态英文 key 下拉选项
     */
    fun statusOptions(workspaceId: String, eventType: String): List<TapdStatusOption> {
        if (workspaceId.isBlank()) {
            throw InvalidParamException(message = "workspaceId is blank", params = arrayOf("workspaceId"))
        }
        val normalizedEventType = when {
            eventType.equals(EVENT_TYPE_STORY, ignoreCase = true) -> EVENT_TYPE_STORY
            eventType.equals(EVENT_TYPE_BUG, ignoreCase = true) -> EVENT_TYPE_BUG
            else -> throw InvalidParamException(
                message = "unsupported tapd eventType",
                params = arrayOf("eventType")
            )
        }
        val cacheKey = workspaceId + STATUS_CACHE_KEY_SEPARATOR + normalizedEventType
        statusCache.getIfPresent(cacheKey)?.let { return it }
        return try {
            val authorized = authorizedWorkspaces()
            if (!authorized.known || !authorized.workspaceIds.contains(workspaceId)) {
                logger.warn(
                    "tapd status options skipped for unknown or unauthorized workspace|" +
                        "workspaceId=$workspaceId|eventType=$normalizedEventType|authorizedKnown=${authorized.known}"
                )
                return emptyList()
            }
            val options = when (normalizedEventType) {
                EVENT_TYPE_STORY -> tapdItemService.getStoryFieldConfig(workspaceId)?.status?.options
                else -> tapdItemService.getBugFieldConfig(workspaceId, true)?.status?.options
            }
            if (options == null) {
                logger.warn(
                    "tapd status options unavailable|workspaceId=$workspaceId|eventType=$normalizedEventType"
                )
                return emptyList()
            }
            val result = options.map { (id, name) -> TapdStatusOption(id = id, name = name) }
                .sortedBy { it.id }
            statusCache.put(cacheKey, result)
            result
        } catch (ignored: Exception) {
            logger.warn(
                "fail to build tapd status options|workspaceId=$workspaceId|eventType=$normalizedEventType",
                ignored
            )
            emptyList()
        }
    }

    /**
     * 聚合已授权与已安装项目；任一数据源失败时返回 unknown，下拉数据按返回空列表处理。
     */
    fun authorizedWorkspaces(): TapdAuthorizedWorkspaces {
        authorizedCache.getIfPresent(AUTHORIZED_CACHE_KEY)?.let { return it }
        return try {
            val granted = tapdItemService.listGrantedWorkspaceIds()
            val installed = tapdItemService.listInstalledWorkspaceIds()
            if (granted == null || installed == null) {
                logger.warn(
                    "tapd authorized workspaces unknown|grantedKnown=${granted != null}|" +
                        "installedKnown=${installed != null}"
                )
                return TapdAuthorizedWorkspaces(known = false)
            }
            val result = TapdAuthorizedWorkspaces(
                known = true,
                workspaceIds = (granted + installed).toSet()
            )
            authorizedCache.put(AUTHORIZED_CACHE_KEY, result)
            result
        } catch (ignored: Exception) {
            logger.warn("fail to load tapd authorized workspaces", ignored)
            TapdAuthorizedWorkspaces(known = false)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TapdDropdownService::class.java)
        private const val AUTHORIZED_CACHE_KEY = "tapd_authorized_workspaces"
        private const val STATUS_CACHE_KEY_SEPARATOR = ":"
        private const val CACHE_MAX_SIZE = 1000L
        private const val CACHE_EXPIRE_MINUTES = 10L
        private const val EVENT_TYPE_STORY = "STORY"
        private const val EVENT_TYPE_BUG = "BUG"
    }
}
