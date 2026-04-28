package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.util.CacheHelper
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.enums.ProjectScopeType
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * 判断项目是否为个人项目（依赖 Project 服务 `projectScope`），结果做本地缓存以降低 RPC 压力。
 */
@Service
class PersonalProjectService(
    private val client: Client
) {

    private val personalFlagCache = CacheHelper.createCache<String, Boolean>(
        maxSize = 50_000L,
        duration = 60L,
        unit = TimeUnit.MINUTES
    )

    fun isPersonalProject(projectCode: String): Boolean {
        val code = projectCode.trim()
        if (code.isEmpty()) return false
        return personalFlagCache.get(code) { loadIsPersonal(code) }
    }

    /**
     * 项目信息在项目侧变更后可调用以刷新缓存（如后续接入 MQ 时再统一调用）。
     */
    fun invalidateCache(projectCode: String) {
        personalFlagCache.invalidate(projectCode.trim())
    }

    private fun loadIsPersonal(projectCode: String): Boolean {
        val vo = client.get(ServiceProjectResource::class).get(englishName = projectCode).data ?: return false
        return vo.projectScope == ProjectScopeType.PERSONAL.value
    }
}
