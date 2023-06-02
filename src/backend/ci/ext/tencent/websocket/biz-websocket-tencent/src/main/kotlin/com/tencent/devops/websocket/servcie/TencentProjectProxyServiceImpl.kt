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

package com.tencent.devops.websocket.servcie

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.websocket.keys.WebsocketKeys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TencentProjectProxyServiceImpl @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag
) : ProjectProxyService {

    @Suppress("ReturnCount")
    override fun checkProject(projectId: String, userId: String): Boolean {
        val tag = bkTag.getLocalTag()
        if (tag.isNotBlank() && tag.contains(IGNORE_TAG)) {
            return true
        }

        try {
            val redisKey = WebsocketKeys.PROJECT_USER_REDIS_KEY + tag + userId
            val redisData = redisOperation.get(redisKey)
            if (redisData != null) {
                val redisProjectList = redisData.split(",")
                if (redisProjectList.contains(projectId)) {
                    return true
                }
            }

            val projectList = client.get(ServiceProjectResource::class).list(userId).data
            val privilegeProjectCodeList = mutableListOf<String>()
            projectList?.map {
                privilegeProjectCodeList.add(it.projectCode)
            }

            redisOperation.set(
                key = redisKey,
                value = JsonUtil.toJson(privilegeProjectCodeList, formatted = false),
                expiredInSecond = ONE_DAY
            )

            return if (privilegeProjectCodeList.contains(projectId)) {
                true
            } else {
                logger.warn(
                    "changePage checkProject fail:" +
                            " user:$userId,projectId:$projectId,projectList:$privilegeProjectCodeList"
                )
                false
            }
        } catch (ignore: Exception) {
            logger.warn("checkProject fail,message:", ignore)
            // 此处为了解耦，假设调用超时，默认还是做changePage的操作
            return true
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TencentProjectProxyServiceImpl::class.java)
        private const val IGNORE_TAG = "gitci"
        private val ONE_DAY: Long = TimeUnit.DAYS.toSeconds(1)
    }
}
