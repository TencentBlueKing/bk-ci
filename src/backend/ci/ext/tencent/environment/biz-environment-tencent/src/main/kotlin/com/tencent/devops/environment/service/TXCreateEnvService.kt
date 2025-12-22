package com.tencent.devops.environment.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class TXCreateEnvService @Autowired constructor() : CreateEnvService() {
    override fun fetchUserWorkspaceId(projectId: String, userId: String): List<String> {
        return emptyList()
    }

    override fun fetchAllWorkspaceId(projectId: String): List<String> {
        return emptyList()
    }
}