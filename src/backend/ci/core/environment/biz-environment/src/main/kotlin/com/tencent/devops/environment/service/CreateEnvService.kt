package com.tencent.devops.environment.service

import com.tencent.devops.common.api.pojo.OS
import org.springframework.stereotype.Service

/**
 * 创作环境相关
 */
@Service
class CreateEnvService {
    fun fetchUserWorkspaceId(projectId: String, userId: String): List<String> {
        return emptyList()
    }

    fun getWorkspaceZoneName(projectId: String, workspaceId: String): String? {
        return null
    }

    fun getWorkspaceDisplayName(userId: String, projectId: String, workspaceId: String?): String? {
        return null
    }

    fun addCreateNode(token: String, deviceId: String,userId: String, os: OS, zoneName: String?): String {
        return ""
    }
}