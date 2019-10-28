package com.tencent.devops.environment.service.thirdPartyAgent

interface IAgentDisconnectNotifyService {

    /**
     * Agent上线通知
     */
    fun online(projectId: String, ip: String, hostname: String, createUser: String, os: String)

    /**
     * Agent离线通知
     */
    fun offline(projectId: String, ip: String, hostname: String, createUser: String, os: String)
}