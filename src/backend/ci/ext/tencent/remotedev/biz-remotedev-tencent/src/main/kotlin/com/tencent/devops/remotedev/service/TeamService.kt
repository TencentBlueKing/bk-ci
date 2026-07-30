package com.tencent.devops.remotedev.service

import org.springframework.stereotype.Service

// 开黑相关的逻辑
@Service
class TeamService {
    fun enableTeam(workspaceName: String): Boolean {
        return true
    }
}