/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.pojo.ProjectVO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import jakarta.ws.rs.NotFoundException

@Service
class ProjectCacheService @Autowired constructor(private val client: Client) {

    /**
     * ProjectID -> ProjectName cache
     */
    private val cache = CacheBuilder.newBuilder().maximumSize(cacheSize)
        .expireAfterWrite(cacheTimeMinute, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, ProjectVO>() {
            override fun load(projectId: String) = getProjectInner(projectId)
        })

    fun getProjectName(projectId: String): String? {
        return getProject(projectId)?.projectName
    }

    fun getProject(projectId: String): ProjectVO? {
        return try {
            cache.get(projectId)
        } catch (ignored: Exception) {
            logger.warn("Fail to get the project name project code($projectId)", ignored)
            try {
                getProjectInner(projectId)
            } catch (ignored2: Exception) {
                logger.warn("Fail to retry get the project name project code($projectId)", ignored2)
            }
            null
        }
    }

    fun getProjectDialect(projectId: String): String? {
        return getProject(projectId = projectId)?.properties?.pipelineDialect
    }

    fun getLoggingLineLimit(projectId: String): Int? {
        return getProject(projectId = projectId)?.properties?.loggingLineLimit
    }

    private fun getProjectInner(projectId: String): ProjectVO {
        return client.get(ServiceProjectResource::class).get(projectId).data
            ?: throw NotFoundException("Fail to find the project info of project($projectId)")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectCacheService::class.java)
        private const val cacheSize: Long = 5000
        private const val cacheTimeMinute: Long = 5
    }
}
