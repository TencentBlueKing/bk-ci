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
import com.tencent.devops.dispatch.docker.pojo.resource.ResourceOptionsVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DcPerformanceOptionsService constructor(
    private val dslContext: DSLContext,
    private val dockerResourceOptionsDao: DockerResourceOptionsDao
) {
    private val logger = LoggerFactory.getLogger(DcPerformanceOptionsService::class.java)

    fun listDcPerformanceConfig(
        userId: String
    ): List<ResourceOptionsVO> {
        logger.info("$userId list listDcPerformanceConfig.")
        try {
            val list = dockerResourceOptionsDao.getList(dslContext)

            if (list.size == 0) {
                return emptyList()
            }

            val performanceOptionsVOList = mutableListOf<ResourceOptionsVO>()
            list.forEach {
                /*performanceOptionsVOList.add(
                    ResourceOptionsVO(
                        cpu = it.cpu,
                        memory = it.memory,
                        disk = it.disk,
                        description = it.description
                    )
                )*/
            }

            return performanceOptionsVOList
        } catch (e: Exception) {
            logger.error("$userId list listDcPerformanceConfig error.", e)
            throw RuntimeException("list listDcPerformanceConfig error.")
        }
    }

    fun createDcPerformanceOptions(userId: String, resourceOptionsVO: ResourceOptionsVO): Boolean {
        logger.info("$userId create performanceOptionsVO: $resourceOptionsVO")

        try {
            /*dockerResourceOptionsDao.create(
                dslContext = dslContext,
                cpu = resourceOptionsVO.cpu,
                memory = resourceOptionsVO.memory,
                disk = resourceOptionsVO.disk,
                description = resourceOptionsVO.description
            )*/
        } catch (e: Exception) {
            logger.error("$userId add performanceOptionsVO error.", e)
            throw RuntimeException("add performanceOptionsVO error.")
        }

        return true
    }

    fun updateDcPerformanceOptions(userId: String, id: Long, resourceOptionsVO: ResourceOptionsVO): Boolean {
        logger.info("$userId update performanceOptionsVO: $resourceOptionsVO")

        try {
            dockerResourceOptionsDao.update(dslContext, id, resourceOptionsVO)
        } catch (e: Exception) {
            logger.error("$userId update performanceOptionsVO error.", e)
            throw RuntimeException("update performanceOptionsVO error.")
        }

        return true
    }

    fun deleteDcPerformanceOptions(userId: String, id: Long): Boolean {
        logger.info("$userId delete performanceOptions id: $id")
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
