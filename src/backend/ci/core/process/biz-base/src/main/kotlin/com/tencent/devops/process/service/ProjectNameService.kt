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
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * deng
 * 2018-12-29
 */
@Suppress("ALL")
@Service
class ProjectNameService @Autowired constructor(private val client: Client) {

    /**
     * ProjectID -> ProjectName cache
     */
    private val cache = CacheBuilder.newBuilder().maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(object : CacheLoader<String, String>() {
            override fun load(projectId: String) =
                retrieveProjectName(projectId)
        })

    fun getProjectName(projectId: String): String? {
        try {
            return cache.get(projectId)
        } catch (ignored: Throwable) {
            logger.warn("Fail to get the project name project code($projectId)", ignored)
        }
        return null
    }

    private fun retrieveProjectName(projectId: String): String {
        val project = client.get(ServiceProjectResource::class).get(projectId)
        if (project.data == null) {
            logger.warn("Fail to get the project name with message ${project.message}")
            throw RemoteServiceException("Fail to get the project name")
        }
        logger.info("Get the project ${project.data} of id $projectId")
        return project.data!!.projectName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectNameService::class.java)
    }
}
