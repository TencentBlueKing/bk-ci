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

package com.tencent.devops.dispatch.docker.service

import com.tencent.devops.dispatch.docker.dao.DockerResourceOptionsDao
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceOptionsVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DockerResourceOptionsService constructor(
    private val dslContext: DSLContext,
    private val dockerResourceOptionsDao: DockerResourceOptionsDao
) {
    private val logger = LoggerFactory.getLogger(DockerResourceOptionsService::class.java)

    fun listDockerResourceConfig(
        userId: String
    ): List<DockerResourceOptionsVO> {
        logger.info("$userId list dockerResourceConfig.")
        try {
            val list = dockerResourceOptionsDao.getList(dslContext)

            if (list.size == 0) {
                return emptyList()
            }

            val optionsVOList = mutableListOf<DockerResourceOptionsVO>()
            list.forEach {
                optionsVOList.add(
                    DockerResourceOptionsVO(
                        memoryLimitBytes = it.memoryLimitBytes,
                        cpuPeriod = it.cpuPeriod,
                        cpuQuota = it.cpuQuota,
                        blkioDeviceWriteBps = it.blkioDeviceWriteBps,
                        blkioDeviceReadBps = it.blkioDeviceReadBps,
                        disk = it.disk,
                        description = it.description
                    )
                )
            }

            return optionsVOList
        } catch (e: Exception) {
            logger.error("$userId list dockerResourceConfig error.", e)
            throw RuntimeException("list dockerResourceConfig error.")
        }
    }

    fun createDockerResourceOptions(userId: String, dockerResourceOptionsVO: DockerResourceOptionsVO): Boolean {
        logger.info("$userId create dockerResourceOptions. resourceOptionsVO: $dockerResourceOptionsVO")

        try {
            dockerResourceOptionsDao.create(
                dslContext = dslContext,
                memoryLimitBytes = dockerResourceOptionsVO.memoryLimitBytes,
                cpuPeriod = dockerResourceOptionsVO.cpuPeriod,
                cpuQuota = dockerResourceOptionsVO.cpuQuota,
                blkioDeviceWriteBps = dockerResourceOptionsVO.blkioDeviceWriteBps,
                blkioDeviceReadBps = dockerResourceOptionsVO.blkioDeviceReadBps,
                disk = dockerResourceOptionsVO.disk,
                description = dockerResourceOptionsVO.description
            )
        } catch (e: Exception) {
            logger.error("$userId create dockerResourceOptions error.", e)
            throw RuntimeException("$userId create dockerResourceOptions error.")
        }

        return true
    }

    fun updateDockerResourceOptions(userId: String, id: Long, dockerResourceOptionsVO: DockerResourceOptionsVO): Boolean {
        logger.info("$userId update resourceOptionsVO: $dockerResourceOptionsVO")

        try {
            dockerResourceOptionsDao.update(dslContext, id, dockerResourceOptionsVO)
        } catch (e: Exception) {
            logger.error("$userId update resourceOptionsVO error.", e)
            throw RuntimeException("update resourceOptionsVO error.")
        }

        return true
    }

    fun deleteDockerResourceOptions(userId: String, id: Long): Boolean {
        logger.info("$userId delete dockerResourceOptions id: $id")
        checkParameter(userId, id.toString())
        val result = dockerResourceOptionsDao.delete(dslContext, id)
        return result == 1
    }

    private fun checkParameter(userId: String, projectId: String) {
        if (projectId.isEmpty()) {
            logger.error("$userId Add failed, projectId is null or ''.")
            throw RuntimeException("Add failed, projectId is null or ''.")
        }
    }
}
