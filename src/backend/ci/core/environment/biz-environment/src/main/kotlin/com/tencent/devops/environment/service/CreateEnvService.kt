package com.tencent.devops.environment.service

import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.StreamingOutput
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

    fun genCreateNodeInstallScript(
        token: String,
        deviceId: String,
        userId: String
    ): Response {
        return Response.ok(StreamingOutput { output ->
            output.write("".toByteArray())
            output.flush()
        }, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = ")
            .build()
    }
}