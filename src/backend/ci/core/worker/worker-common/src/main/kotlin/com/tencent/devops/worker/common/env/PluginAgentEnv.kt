package com.tencent.devops.worker.common.env

import com.tencent.devops.worker.common.exception.PropertyNotExistException
import org.slf4j.LoggerFactory

object PluginAgentEnv {
    private val logger = LoggerFactory.getLogger(PluginAgentEnv::class.java)

    private const val AGENT_ID = "devops.agent.id"
    private const val AGENT_SECRET_KEY = "devops.agent.secret.key"
    private const val AGENT_GATEWAY = "landun.gateway"

    private var agentId: String? = null
    private var secretKey: String? = null
    private var gateway: String? = null

    fun getAgentId(): String {
        if (agentId.isNullOrBlank()) {
            synchronized(this) {
                if (agentId.isNullOrBlank()) {
                    agentId = System.getProperty(AGENT_ID)
                    if (agentId.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_ID, "Empty agent Id")
                    }
                    logger.info("Get the agent id($agentId)")
                }
            }
        }
        return agentId!!
    }

    fun getAgentSecretKey(): String {
        if (secretKey.isNullOrBlank()) {
            synchronized(this) {
                if (secretKey.isNullOrBlank()) {
                    secretKey = System.getProperty(AGENT_SECRET_KEY)
                    if (secretKey.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_SECRET_KEY, "Empty secret key")
                    }
                }
            }
        }
        return secretKey!!
    }

    fun getGateway(): String {
        if (gateway.isNullOrBlank()) {
            synchronized(this) {
                if (gateway.isNullOrBlank()) {
                    gateway = System.getProperty(AGENT_GATEWAY)
                    if (gateway.isNullOrBlank()) {
                        throw PropertyNotExistException(AGENT_GATEWAY, "Empty gateway")
                    }
                    logger.info("Get the gateway($gateway)")
                }
            }
        }
        return gateway!!
    }
}