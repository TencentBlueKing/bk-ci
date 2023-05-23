package com.tencent.devops.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.worker.common.env.AgentEnv
import okhttp3.Request
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

    fun initEnv() {
        // 如果agentId环境变量存在，表示变量已注入，不用调用接口获取
        if (System.getProperty("devops.agent.id") != null) {
            return
        }

        // 旧版逻辑，轮训网关接口获取agentId等环境变量
        var startBuild = false
        val gateyway = AgentEnv.getGateway()
        val url = "http://$gateyway/dispatch-macos/gw/build/macos/startBuild"
        println("url:$url")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("X-DEVOPS-BUILD-TYPE", "MACOS")
            .get()
            .build()

        do {
            try {
                OkhttpUtils.doHttp(request).use { resp ->
                    val resoCode = resp.code
                    val responseStr = resp.body!!.string()
                    println("resoCode: $resoCode;responseStr:$responseStr")
                    if (resoCode == 200) {
                        val response: Map<String, String> = jacksonObjectMapper().readValue(responseStr)

                        // 将变量写入到property当中
                        response.forEach { (key, value) ->
                            when (key) {
                                "agentId" -> System.setProperty("devops.agent.id", value)
                                "secretKey" -> System.setProperty("devops.agent.secret.key", value)
                                "projectId" -> System.setProperty("devops.project.id", value)
                                "xcodeVersion" -> System.setProperty("xcodeVersion", value)
                            }
                        }
                        startBuild = true
                    } else {
                        println("There is no build for this macos,sleep for 5s.")
                    }
                }
                if (!startBuild) {
                    Thread.sleep(5000)
                }
            } catch (e: Exception) {
                println("Failed to connect to devops server.")
            }
        } while (!startBuild)
        println("Start to run.")
    }
}
