package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.enums.ProjectScopeType
import org.springframework.stereotype.Service

/**
 * 判断项目是否为个人项目（依赖 Project 服务 `projectScope`）。
 */
@Service
class PersonalProjectService(
    private val client: Client
) {

    fun isPersonalProject(projectCode: String): Boolean {
        val code = projectCode.trim()
        if (code.isEmpty()) return false
        return loadIsPersonal(code)
    }

    private fun loadIsPersonal(projectCode: String): Boolean {
        val vo = client.get(ServiceProjectResource::class).get(englishName = projectCode).data ?: return false
        return vo.projectScope == ProjectScopeType.PERSONAL.value
    }
}
