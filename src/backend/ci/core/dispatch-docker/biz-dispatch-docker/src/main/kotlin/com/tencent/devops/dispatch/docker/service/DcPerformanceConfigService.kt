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
import com.tencent.devops.dispatch.docker.pojo.resource.ResourceConfigVO
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DcPerformanceConfigService constructor(
    private val dslContext: DSLContext,
    private val dockerResourceConfigDao: DockerResourceConfigDao
) {
    private val logger = LoggerFactory.getLogger(DcPerformanceConfigService::class.java)

    fun listDcPerformanceConfig(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): ListPage<ResourceConfigVO> {
        logger.info("$userId list performanceConfigList.")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        try {
            val list = dockerResourceConfigDao.getList(dslContext, pageNotNull, pageSizeNotNull)
            val count = dockerResourceConfigDao.getCount(dslContext)

            if (list == null || list.size == 0 || count == 0L) {
                return ListPage(pageNotNull, pageSizeNotNull, count ?: 0, emptyList())
            }

            val performanceConfigVOList = mutableListOf<ResourceConfigVO>()
            list.forEach {
                performanceConfigVOList.add(
                    ResourceConfigVO(
                        projectId = it["PROJECT_ID"] as String,
                        cpu = it["CPU"] as Int,
                        memory = (it["MEMORY"] as Int).toString() + "M",
                        disk = (it["DISK"] as Int).toString() + "G",
                        description = it["DESCRIPTION"] as String
                    )
                )
            }

            return ListPage(pageNotNull, pageSizeNotNull, count ?: 0, performanceConfigVOList)
        } catch (e: Exception) {
            logger.error("$userId list performanceConfigList error.", e)
            throw RuntimeException("list performanceConfigList error.")
        }
    }

    fun createDcPerformanceConfig(userId: String, createResourceConfigVO: CreateResourceConfigVO): Boolean {
        logger.info("$userId create opPerformanceConfigVO: $createResourceConfigVO")
        checkParameter(userId, createResourceConfigVO.projectId)

        try {
            dockerResourceConfigDao.createOrUpdate(
                dslContext = dslContext,
                projectId = createResourceConfigVO.projectId,
                optionId = createResourceConfigVO.optionId
            )
        } catch (e: Exception) {
            logger.error("$userId add performanceConfig error.", e)
            throw RuntimeException("add performanceConfig error.")
        }

        return true
    }

    fun updateDcPerformanceConfig(
        userId: String,
        projectId: String,
        createResourceConfigVO: CreateResourceConfigVO
    ): Boolean {
        logger.info("$userId update performanceConfig: $createResourceConfigVO")
        checkParameter(userId, projectId)

        try {
            dockerResourceConfigDao.createOrUpdate(
                dslContext = dslContext,
                projectId = projectId,
                optionId = createResourceConfigVO.optionId
            )

            return true
        } catch (e: Exception) {
            logger.error("$userId update performanceConfig error.", e)
            throw RuntimeException("update performanceConfig error")
        }
    }

    fun deleteDcPerformanceConfig(userId: String, projectId: String): Boolean {
        logger.info("$userId delete performanceConfig projectId: $projectId")
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
