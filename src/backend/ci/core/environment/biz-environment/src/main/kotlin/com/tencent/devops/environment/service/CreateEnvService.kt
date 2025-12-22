package com.tencent.devops.environment.service

import org.springframework.stereotype.Service

/**
 * 创作环境相关
 */
@Service
class CreateEnvService {
    fun fetchUserWorkspaceId(projectId: String, userId: String): List<String> {
        return emptyList()
    }

    fun fetchAllWorkspaceId(projectId: String): List<String> {
        return emptyList()
    }
}