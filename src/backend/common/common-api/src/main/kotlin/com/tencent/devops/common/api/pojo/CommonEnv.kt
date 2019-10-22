package com.tencent.devops.common.api.pojo

import org.slf4j.LoggerFactory

/**
 * This is to store the common build env
 * like the ssh-key, if it add the ssh key to ssh-agent, it need to add the SSH_AUTH_SOCK & SSH_AGENT_PID to env for the scripts
 */
object CommonEnv {
    private val commonEnv = HashMap<String, String>()
    private val logger = LoggerFactory.getLogger(CommonEnv::class.java)

    fun addCommonEnv(env: Map<String, String>) {
        logger.info("Add the env($env) to common environment")
        commonEnv.putAll(env)
    }

    fun getCommonEnv(): Map<String, String> {
        return HashMap(commonEnv)
    }
}