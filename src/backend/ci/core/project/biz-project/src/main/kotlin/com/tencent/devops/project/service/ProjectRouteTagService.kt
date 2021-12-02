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

package com.tencent.devops.project.service

import com.tencent.devops.common.client.consul.ConsulConstants.singelProjectRedisKey
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ProjectRouteTagService @Autowired constructor(
    val projectService: ProjectService,
    val redisOperation: RedisOperation
) {
    @Value("\${spring.cloud.consul.discovery.tags:#{null}}")
    private val tag: String? = null

    @Value("\${tag.prod:#{null}}")
    private val prodTag: String? = null

    // 判断当前项目流量与当前集群匹配
    fun checkProjectTag(projectId: String): Boolean {
        // 优先走缓存
        if (redisOperation.get(singelProjectRedisKey(projectId)) != null) {
            val cacheCheck= projectClusterCheck(redisOperation.get(singelProjectRedisKey(projectId))!!)
            // cache校验成功直接返回
            if (cacheCheck) {
                return cacheCheck
            }
        }
        val projectInfo = projectService.getByEnglishName(projectId) ?: return false
        logger.info("checkProjectTag $projectId cache not match, get from db. ${projectInfo.routerTag}")
        // 请求源大量来自定时任务, redis缓存2分钟
        redisOperation.set(singelProjectRedisKey(projectId), projectInfo.routerTag ?: "", 120L)
        return projectClusterCheck(projectInfo.routerTag)
    }

    private fun projectClusterCheck(routerTag: String?): Boolean {
        // 默认集群是不会有routerTag的信息
        if (routerTag.isNullOrBlank()) {
            // 只有默认集群在routerTag为空的时候才返回true
            return tag == prodTag
        }
        return tag == routerTag
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectRouteTagService::class.java)
    }
}
