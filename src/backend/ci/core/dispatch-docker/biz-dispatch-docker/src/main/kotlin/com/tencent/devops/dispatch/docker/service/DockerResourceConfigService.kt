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

import com.tencent.devops.dispatch.docker.dao.DockerResourceConfigDao
import com.tencent.devops.dispatch.docker.pojo.resource.CreateResourceConfigVO
import com.tencent.devops.dispatch.docker.pojo.resource.ListPage
import com.tencent.devops.dispatch.docker.pojo.resource.DockerResourceConfigVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DockerResourceConfigService constructor(
    private val dslContext: DSLContext,
    private val dockerResourceConfigDao: DockerResourceConfigDao
) {
    private val logger = LoggerFactory.getLogger(DockerResourceConfigService::class.java)

    fun listDockerResourceConfig(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): ListPage<DockerResourceConfigVO> {
        logger.info("$userId list dockerResourceConfig.")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        try {
            val list = dockerResourceConfigDao.getList(dslContext, pageNotNull, pageSizeNotNull)
            val count = dockerResourceConfigDao.getCount(dslContext)

            if (list == null || list.size == 0 || count == 0L) {
                return ListPage(pageNotNull, pageSizeNotNull, count ?: 0, emptyList())
            }

            val performanceConfigVOList = mutableListOf<DockerResourceConfigVO>()
            list.forEach {
                performanceConfigVOList.add(
                    DockerResourceConfigVO(
                        projectId = it["PROJECT_ID"] as String,
                        cpuPeriod = it["CPU_PERIOD"] as Int,
                        cpuQuota = it["CPU_QUOTA"] as Int,
                        memoryLimitBytes = it["MEMORY_LIMIT_BYTES"] as Long,
                        blkioDeviceReadBps = it["BLKIO_DEVICE_READ_BPS"] as Long,
                        blkioDeviceWriteBps = it["BLKIO_DEVICE_WRITE_BPS"] as Long,
                        disk = it["DISK"] as Int,
                        description = it["DESCRIPTION"] as String
                    )
                )
            }

            return ListPage(pageNotNull, pageSizeNotNull, count ?: 0, performanceConfigVOList)
        } catch (e: Exception) {
            logger.error("$userId list dockerResourceConfig error.", e)
            throw RuntimeException("list dockerResourceConfig error.")
        }
    }

    fun getDockerResourceConfig(projectId: String): DockerResourceConfigVO {
        val dockerResourceRecord = dockerResourceConfigDao.getByProjectId(dslContext, projectId)
        if (dockerResourceRecord != null) {
            return DockerResourceConfigVO(
                projectId = dockerResourceRecord["PROJECT_ID"] as String,
                cpuPeriod = dockerResourceRecord["CPU_PERIOD"] as Int,
                cpuQuota = dockerResourceRecord["CPU_QUOTA"] as Int,
                memoryLimitBytes = dockerResourceRecord["MEMORY_LIMIT_BYTES"] as Long,
                blkioDeviceReadBps = dockerResourceRecord["BLKIO_DEVICE_READ_BPS"] as Long,
                blkioDeviceWriteBps = dockerResourceRecord["BLKIO_DEVICE_WRITE_BPS"] as Long,
                disk = dockerResourceRecord["DISK"] as Int,
                description = dockerResourceRecord["DESCRIPTION"] as String
            )
        } else {
            return DockerResourceConfigVO(
                projectId = projectId,
                memoryLimitBytes = 34359738368L,
                cpuPeriod = 10000,
                cpuQuota = 160000,
                blkioDeviceReadBps = 125829120,
                blkioDeviceWriteBps = 125829120,
                disk = 100,
                description = ""
            )
        }
    }

    fun createDockerResourceConfig(userId: String, createResourceConfigVO: CreateResourceConfigVO): Boolean {
        logger.info("$userId createDockerResourceConfig createResourceConfigVO: $createResourceConfigVO")
        checkParameter(userId, createResourceConfigVO.projectId)

        try {
            dockerResourceConfigDao.createOrUpdate(
                dslContext = dslContext,
                projectId = createResourceConfigVO.projectId,
                optionId = createResourceConfigVO.optionId
            )
        } catch (e: Exception) {
            logger.error("$userId createDockerResourceConfig error.", e)
            throw RuntimeException("$userId createDockerResourceConfig error.")
        }

        return true
    }

    fun updateDockerResourceConfig(
        userId: String,
        projectId: String,
        createResourceConfigVO: CreateResourceConfigVO
    ): Boolean {
        logger.info("$userId update createResourceConfigVO: $createResourceConfigVO")
        checkParameter(userId, projectId)

        try {
            dockerResourceConfigDao.createOrUpdate(
                dslContext = dslContext,
                projectId = projectId,
                optionId = createResourceConfigVO.optionId
            )

            return true
        } catch (e: Exception) {
            logger.error("$userId update createResourceConfigVO error.", e)
            throw RuntimeException("update createResourceConfigVO error")
        }
    }

    fun deleteDockerResourceConfig(userId: String, projectId: String): Boolean {
        logger.info("$userId delete dockerResourceConfig projectId: $projectId")
        checkParameter(userId, projectId)
        val result = dockerResourceConfigDao.delete(dslContext, projectId)
        return result == 1
    }

    private fun checkParameter(userId: String, projectId: String) {
        if (projectId.isEmpty()) {
            logger.error("$userId Add failed, projectId is null or ''.")
            throw RuntimeException("Add failed, projectId is null or ''.")
        }
    }
}
