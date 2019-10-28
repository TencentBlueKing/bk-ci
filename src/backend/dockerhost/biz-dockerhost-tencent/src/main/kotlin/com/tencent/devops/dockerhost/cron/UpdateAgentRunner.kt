package com.tencent.devops.dockerhost.cron

import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.dockerhost.config.DockerHostConfig
import com.tencent.devops.dockerhost.config.TXDockerHostConfig
import com.tencent.devops.dockerhost.dispatch.DockerHostBuildResourceApi
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.util.Random

class UpdateAgentRunner @Autowired constructor(
    private val dockerHostConfig: TXDockerHostConfig
) {
    private val logger = LoggerFactory.getLogger(UpdateAgentRunner::class.java)
    private val dockerHostBuildApi: DockerHostBuildResourceApi = DockerHostBuildResourceApi()

    fun update() {
        try {
            val agentFile = File(dockerHostConfig.dockerAgentPath!!)
            val localFileLength = if (agentFile.exists()) { agentFile.length() } else { 0 }
            val serverFileLength = dockerHostBuildApi.getDockerJarLength()
            if (0L == localFileLength || localFileLength != serverFileLength) {
                logger.info("need to update docker.jar")
                val bakFile = File(dockerHostConfig.dockerAgentPath!! + "_bak")
                logger.info("copy origin file to bak")
                if (agentFile.exists()) {
                    agentFile.copyTo(bakFile, true)
                }

                Thread.sleep(getRandom().toLong() * 60 * 1000) // 随机打散请求时间，防止同一时间请求网关，网关流量太大
                logger.info("Download new file...")
                OkhttpUtils.downloadFile(dockerHostConfig.downloadDockerAgentUrl!!, agentFile)
                logger.info("Download new file finished")
            } else {
                logger.info("No need to update.")
            }
        } catch (t: Throwable) {
            logger.error("StartBuild encounter unknown exception", t)
        }
    }

    private fun getRandom(): Int {
        val max = 30
        val min = 0
        val random = Random()
        return random.nextInt(max) % (max - min + 1) + min
    }
}
