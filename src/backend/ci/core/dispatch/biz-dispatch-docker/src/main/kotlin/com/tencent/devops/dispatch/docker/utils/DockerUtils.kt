/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.docker.utils

import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadLocalRandom

object DockerUtils {
    fun parseShortImage(image: String): String {
        return if (image.contains("/")) {
            image.substring(image.lastIndexOf("/") + 1)
        } else {
            image
        }
    }

    /**
     * 构建固定构建机信息与上一次构建机信息来决定分配的构建机，
     * 专用构建机上存在一些特殊的依赖环境或配置，所以即使飘移到公共集群也会无法正常构建，所以专用构建机不允许柔性处理
     * @param dockerHosts 专机列表
     * @param lastHostIp 上次构建机
     * @param buildId 构建ID
     */
    @Suppress("ALL")
    fun getDockerHostIp(dockerHosts: List<String>, lastHostIp: String?, buildId: String): String {
        return when {
            dockerHosts.size == 1 -> { // 只有一台固定构建机
                logger.info("[$buildId]|Fixed build host machine, hostIp:${dockerHosts[0]}")
                dockerHosts[0]
            }
            dockerHosts.size > 1 -> {
                if (lastHostIp != null) {
                    dockerHosts.forEach {
                        // 固定构建机中寻找上一次使用的构建机
                        if (it == lastHostIp) {
                            logger.info("[$buildId]|Last fixed build host machine, hostIp:$lastHostIp")
                            return lastHostIp
                        }
                    }
                }
                // 随机分配
                val randomIp = dockerHosts[ThreadLocalRandom.current().nextInt(0, dockerHosts.size)]
                logger.info("[$buildId]|Random a fixed build host machine, hostIp:$randomIp")
                randomIp
            }
            null != lastHostIp -> { // 使用上一次的构建机
                logger.info("[$buildId]|Use last build hostIp: $lastHostIp")
                lastHostIp
            }
            else -> ""
        }
    }

    private val logger = LoggerFactory.getLogger(DockerUtils::class.java)
}
