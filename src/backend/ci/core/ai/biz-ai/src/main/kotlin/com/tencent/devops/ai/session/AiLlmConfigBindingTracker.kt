package com.tencent.devops.ai.session

import com.tencent.devops.ai.service.UserLlmConfigService
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * 跟踪每个 threadId 上次 resolve Agent 时绑定的用户 LLM 配置指纹。
 *
 * 当用户在同一会话中修改自定义大模型配置后，[resolveFingerprint] 会变化，
 * 从而触发 [invalidateIfStale] 使 [ThreadSessionManager] 中的 Agent 缓存失效并重建。
 */
@Component
class AiLlmConfigBindingTracker(
    private val userLlmConfigService: UserLlmConfigService
) {

    private val threadBindings = ConcurrentHashMap<String, Long>()

    /**
     * 解析用户当前 LLM 配置指纹。
     *
     * - `0L`：无配置、已删除或 `enabled=false`，对应平台默认模型。
     * - 非 `0L`：启用中的用户配置，取 [updatedTime][com.tencent.devops.ai.pojo.UserLlmConfigInfo.updatedTime]。
     */
    fun resolveFingerprint(userId: String): Long {
        val info = userLlmConfigService.get(userId) ?: return PLATFORM_FINGERPRINT
        if (!info.enabled) {
            return PLATFORM_FINGERPRINT
        }
        return info.updatedTime
    }

    /**
     * 判断 threadId 已绑定的指纹是否与当前用户配置不一致。
     *
     * @return `true` 表示应失效会话缓存；首次绑定（无历史记录）返回 `false`。
     */
    fun invalidateIfStale(threadId: String, userId: String): Boolean {
        val bound = threadBindings[threadId] ?: return false
        return bound != resolveFingerprint(userId)
    }

    fun bind(threadId: String, fingerprint: Long) {
        threadBindings[threadId] = fingerprint
    }

    fun evict(threadId: String) {
        threadBindings.remove(threadId)
    }

    companion object {
        const val PLATFORM_FINGERPRINT = 0L
    }
}
