/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dockerhost.cron

import com.tencent.devops.common.api.util.OkhttpUtils
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
