package com.tencent.devops.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.worker.common.BUILD_TYPE
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.utils.ExecutorUtil
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.File

object MacAgentEnv {

    private val logger = LoggerFactory.getLogger(AgentEnv::class.java)
    private const val MACOS_WORKSPACE = "DEVOPS_MACOS_DIR"
    private const val XCODE_VERSION = "devops_xcodeVersion"
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
        if (AgentEnv.getAgentId().isNotBlank()) {
            // 设置buildType=MACOS_NEW, 适配网关兼容逻辑
            System.setProperty(BUILD_TYPE, BuildType.MACOS_NEW.name)
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
                                "agentId" -> System.setProperty(AgentEnv.AGENT_ID, value)
                                "secretKey" -> System.setProperty(AgentEnv.AGENT_SECRET_KEY, value)
                                "projectId" -> System.setProperty(AgentEnv.PROJECT_ID, value)
                                "xcodeVersion" -> System.setProperty(XCODE_VERSION, value)
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

        selectXcode()
    }

    private fun selectXcode() {
        println("Start to select xcode.")
        // 选择XCODE版本
        val xcodeVersion = AgentEnv.getEnvProp(XCODE_VERSION) ?: throw RuntimeException("Not found xcodeVersion")
        val xcodePath = "/Applications/Xcode_$xcodeVersion.app"
        val xcodeFile = File(xcodePath)
        // 当指定XCode版本存在的时候，切换xcode
        if (xcodeFile.exists() && xcodeFile.isDirectory) {
            try {
                // 删除软链
                val rmCommand = "sudo rm -rf /Applications/Xcode.app"
                ExecutorUtil.runCommand(rmCommand, rmCommand)
                // 新建软链
                val lnCommand = "sudo ln -s /Applications/Xcode_$xcodeVersion.app  /Applications/Xcode.app"
                ExecutorUtil.runCommand(lnCommand, lnCommand)
                // 选择xcode
                val selectCommand = "sudo xcode-select -s /Applications/Xcode.app/Contents/Developer/"
                ExecutorUtil.runCommand(selectCommand, selectCommand)
                println("End to select xcode:select Xcode_$xcodeVersion.app.")
            } catch (e: Exception) {
                println("End to select xcode with error: $e")
            }
        } else {
            println("End to select xcode:nothing to do.")
        }
    }
}
