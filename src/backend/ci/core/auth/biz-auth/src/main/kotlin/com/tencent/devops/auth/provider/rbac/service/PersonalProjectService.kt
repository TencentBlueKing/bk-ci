package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectApprovalResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.enums.ProjectScopeType
import org.springframework.stereotype.Service

/**
 * 判断项目是否为个人项目（依赖 Project 服务 `projectScope`）。
 */
@Service
class PersonalProjectService(private val client: Client) {

    fun isPersonalProject(projectCode: String): Boolean {
        val code = projectCode.trim()
        if (code.isEmpty()) return false
        return loadIsPersonal(code)
    }

    private fun loadIsPersonal(projectCode: String): Boolean {
        val projectScope =
            client.get(ServiceProjectResource::class).get(englishName = projectCode).data?.projectScope ?: client.get(
                ServiceProjectApprovalResource::class
            ).get(projectId = projectCode).data?.projectScope ?: return false
        return projectScope == ProjectScopeType.PERSONAL.value
    }
}
