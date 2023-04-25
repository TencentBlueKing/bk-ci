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

package com.tencent.devops.artifactory.service

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.process.api.service.ServiceJfrogResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class PipelineService @Autowired constructor(
    private val client: Client
) {

    fun getFullName(path: String, pipelineId: String, pipelineName: String): String {
        return path.replaceFirst("/$pipelineId", "/$pipelineName")
    }

    fun getFullName(
        path: String,
        pipelineId: String,
        pipelineName: String,
        buildId: String,
        buildName: String
    ): String {
        return path.replaceFirst("/$pipelineId/$buildId", "/$pipelineName/$buildName")
    }

    fun isRootDir(path: String): Boolean {
        return path == "/"
    }

    fun isPipelineDir(path: String): Boolean {
        val roadList = path.split("/")
        return roadList.size == 2 && roadList[1].isNotBlank()
    }

    fun getPipelineId(path: String): String {
        val roads = path.split("/")
        if (roads.size < 2) throw RuntimeException("Path $path doesn't contain pipelineId")
        return roads[1]
    }

    fun getBuildId(path: String): String {
        val roads = path.split("/")
        if (roads.size < 3) throw RuntimeException("Path $path doesn't contain buildId")
        return roads[2]
    }

    fun getPipelineName(projectId: String, pipelineId: String): String {
        val startTimestamp = System.currentTimeMillis()
        try {
            return client.get(ServiceJfrogResource::class).getPipelineNameByIds(
                projectId,
                setOf(pipelineId)
            ).data!![pipelineId]!!
        } finally {
            logger.info("getPipelineName[$projectId,$pipelineId] cost${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun getPipelineNames(projectId: String, pipelineIds: Set<String>): Map<String, String> {
        val startTimestamp = System.currentTimeMillis()
        try {
            if (pipelineIds.isEmpty()) return emptyMap()
            return client.get(ServiceJfrogResource::class).getPipelineNameByIds(projectId, pipelineIds).data!!
        } finally {
            logger.info(
                "getPipelineNames[$projectId, $pipelineIds] cost${System.currentTimeMillis() - startTimestamp}ms"
            )
        }
    }

    fun getBuildName(projectId: String?, buildId: String): String {
        val startTimestamp = System.currentTimeMillis()
        try {
            return client.get(ServiceJfrogResource::class).getBuildNoByBuildIdsNew(
                buildIds = setOf(buildId),
                projectId = projectId
            ).data!![buildId]!!
        } finally {
            logger.info("getBuildName [$buildId] cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    fun getBuildNames(projectId: String?, buildIds: Set<String>): Map<String, String> {
        val startTimestamp = System.currentTimeMillis()
        try {
            if (buildIds.isEmpty()) return emptyMap()
            return client.get(ServiceJfrogResource::class).getBuildNoByBuildIdsNew(buildIds, projectId).data!!
        } finally {
            logger.info("getBuildNames [$buildIds] cost ${System.currentTimeMillis() - startTimestamp}ms")
        }
    }

    abstract fun validatePermission(
        userId: String,
        projectId: String,
        pipelineId: String? = null,
        permission: AuthPermission? = null,
        message: String? = null
    )

    abstract fun hasPermission(
        userId: String,
        projectId: String,
        pipelineId: String? = null,
        permission: AuthPermission? = null
    ): Boolean

    abstract fun filterPipeline(user: String, projectId: String): List<String>

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineService::class.java)
    }
}
