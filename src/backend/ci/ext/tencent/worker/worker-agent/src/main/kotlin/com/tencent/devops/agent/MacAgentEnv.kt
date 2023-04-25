package com.tencent.devops.agent

import com.tencent.devops.worker.common.env.AgentEnv
import org.slf4j.LoggerFactory

object MacAgentEnv {

    private val logger = LoggerFactory.getLogger(AgentEnv::class.java)
    private const val MACOS_WORKSPACE = "DEVOPS_MACOS_DIR"
    private var macOSWorkspace: String? = null

    fun getMacOSWorkspace(): String {
        if (macOSWorkspace.isNullOrBlank()) {
            synchronized(this) {
                if (macOSWorkspace.isNullOrBlank()) {
                    macOSWorkspace = AgentEnv.getEnvProp(MACOS_WORKSPACE)
                    if (macOSWorkspace.isNullOrBlank()) {
                        logger.info("Empty macOSWorkspace. set default: /Volumes/data")
                        macOSWorkspace = "/Volumes/data"
                    } else {
                        logger.info("Get the macOSWorkspace($macOSWorkspace)")
                    }
                }
            }
        }
        return macOSWorkspace!! + "/workspace"
    }
}
