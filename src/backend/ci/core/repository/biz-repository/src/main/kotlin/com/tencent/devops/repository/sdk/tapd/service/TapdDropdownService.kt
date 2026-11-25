package com.tencent.devops.repository.sdk.tapd.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.scm.pojo.tapd.TapdAuthorizedWorkspaces
import com.tencent.devops.scm.pojo.tapd.TapdStatusOption
import com.tencent.devops.scm.pojo.tapd.TapdWorkspaceOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * TAPD 触发器下拉选项与状态映射服务
 *
 * 下拉选项与 Webhook 匹配所需的中文状态映射均实时请求 TAPD OpenAPI，不使用缓存。
 */
@Service
class TapdDropdownService @Autowired constructor(
    private val tapdItemService: ITapdItemService
) {

    /**
     * 当前用户可见且应用已授权的 TAPD 项目下拉选项
     *
     * 未授权项目不展示；授权集合未知时返回空列表。授权集合每次实时拉取。
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
            logger.error("fail to build tapd workspace options", ignored)
            emptyList()
        }
    }

    /**
     * 按项目 ID 与事件类型返回中文状态名下拉选项
     *
     * id 与 name 均为中文状态名，前端和 PAC/模型保存值均为中文。
     * 每次请求都会实时拉取 TAPD 状态数据，不使用缓存。
     */
    fun statusOptions(workspaceId: String, eventType: String): List<TapdStatusOption> {
        val normalizedEventType = normalizeEventType(workspaceId, eventType)
        return try {
            val authorized = authorizedWorkspaces()
            if (!authorized.known || !authorized.workspaceIds.contains(workspaceId)) {
                logger.warn(
                    "tapd status options skipped for unknown or unauthorized workspace|" +
                        "workspaceId=$workspaceId|eventType=$normalizedEventType|authorizedKnown=${authorized.known}"
                )
                return emptyList()
            }
            loadStatusMap(workspaceId, normalizedEventType)
                .map { (_, chineseName) -> TapdStatusOption(id = chineseName, name = chineseName) }
                .sortedBy { it.id }
        } catch (ignored: Exception) {
            logger.error(
                "fail to build tapd status options|workspaceId=$workspaceId|eventType=$normalizedEventType",
                ignored
            )
            emptyList()
        }
    }

    /**
     * 实时查询 TAPD 状态映射
     *
     * 返回 Map<英文状态 key, 中文状态名>。每次调用都会请求 TAPD OpenAPI，不使用缓存。
     */
    fun statusMap(workspaceId: String, eventType: String): Map<String, String> {
        val normalizedEventType = normalizeEventType(workspaceId, eventType)
        val authorized = authorizedWorkspaces()
        if (!authorized.known || !authorized.workspaceIds.contains(workspaceId)) {
            throw IllegalStateException(
                "tapd status map unavailable: unknown or unauthorized workspace|" +
                    "workspaceId=$workspaceId|eventType=$normalizedEventType|authorizedKnown=${authorized.known}"
            )
        }
        return loadStatusMap(workspaceId, normalizedEventType)
    }

    /**
     * 实时聚合已授权与已安装项目；任一数据源失败时返回 unknown。
     */
    fun authorizedWorkspaces(): TapdAuthorizedWorkspaces {
        return try {
            val granted = tapdItemService.listGrantedWorkspaceIds()
            val installed = tapdItemService.listInstalledWorkspaceIds()
            if (granted == null || installed == null) {
                logger.error(
                    "tapd authorized workspaces unknown|grantedKnown=${granted != null}|" +
                        "installedKnown=${installed != null}"
                )
                return TapdAuthorizedWorkspaces(known = false)
            }
            TapdAuthorizedWorkspaces(
                known = true,
                workspaceIds = (granted + installed).toSet()
            )
        } catch (ignored: Exception) {
            logger.error("fail to load tapd authorized workspaces", ignored)
            TapdAuthorizedWorkspaces(known = false)
        }
    }

    private fun normalizeEventType(workspaceId: String, eventType: String): String {
        if (workspaceId.isBlank()) {
            throw InvalidParamException(message = "workspaceId is blank", params = arrayOf("workspaceId"))
        }
        return when {
            eventType.equals(EVENT_TYPE_STORY, ignoreCase = true) -> EVENT_TYPE_STORY
            eventType.equals(EVENT_TYPE_BUG, ignoreCase = true) -> EVENT_TYPE_BUG
            else -> throw InvalidParamException(
                message = "unsupported tapd eventType",
                params = arrayOf("eventType")
            )
        }
    }

    private fun loadStatusMap(workspaceId: String, normalizedEventType: String): Map<String, String> {
        val options = when (normalizedEventType) {
            EVENT_TYPE_STORY -> tapdItemService.getStoryFieldConfig(workspaceId)?.status?.options
            else -> tapdItemService.getBugFieldConfig(workspaceId, true)?.status?.options
        }
        if (options == null) {
            throw IllegalStateException(
                "tapd status options unavailable|workspaceId=$workspaceId|eventType=$normalizedEventType"
            )
        }
        val duplicateNames = options.entries.groupBy({ it.value }, { it.key }).filter { it.value.size > 1 }
        if (duplicateNames.isNotEmpty()) {
            logger.error(
                "duplicate tapd status names|workspaceId=$workspaceId|eventType=$normalizedEventType|" +
                    "duplicates=$duplicateNames"
            )
        }
        return options
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TapdDropdownService::class.java)
        private const val EVENT_TYPE_STORY = "STORY"
        private const val EVENT_TYPE_BUG = "BUG"
    }
}
