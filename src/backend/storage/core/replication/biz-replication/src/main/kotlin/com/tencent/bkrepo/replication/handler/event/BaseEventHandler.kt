/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.replication.handler.event

import com.google.common.cache.CacheBuilder
import com.tencent.bkrepo.replication.handler.BaseHandler
import com.tencent.bkrepo.replication.model.TReplicationTask
import com.tencent.bkrepo.replication.pojo.ReplicationRepoDetail
import com.tencent.bkrepo.replication.service.ReplicationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

/**
 * AbstractMessageHandler
 */
abstract class BaseEventHandler : BaseHandler() {

    // LateinitUsage: 抽象类中使用构造器注入会造成不便
    @Suppress("LateinitUsage")
    @Autowired
    lateinit var replicationService: ReplicationService

    private val repoDetailCache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(210, TimeUnit.SECONDS)
        .build<String, ReplicationRepoDetail>()

    fun getRepoDetail(projectId: String, repoName: String, remoteRepoName: String): ReplicationRepoDetail? {
        val cacheKey = "$projectId:$repoName:$remoteRepoName"
        var cacheDetail = repoDetailCache.getIfPresent(cacheKey)
        if (cacheDetail == null) {
            val repoDetail = repoDataService.getRepositoryDetail(projectId, repoName) ?: run {
                logger.warn("found no repo detail [$projectId, $repoName]")
                return null
            }
            cacheDetail = convertReplicationRepo(repoDetail, remoteRepoName)
            repoDetailCache.put(cacheKey, cacheDetail)
        }
        return cacheDetail
    }

    fun getRemoteProjectId(task: TReplicationTask, sourceProjectId: String): String {
        return task.remoteProjectId ?: task.localProjectId ?: sourceProjectId
    }

    fun getRemoteRepoName(task: TReplicationTask, sourceRepoName: String): String {
        return task.remoteRepoName ?: task.localRepoName ?: sourceRepoName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BaseEventHandler::class.java)
    }
}
