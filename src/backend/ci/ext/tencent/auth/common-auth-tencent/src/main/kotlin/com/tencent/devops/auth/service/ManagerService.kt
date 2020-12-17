package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.api.ServiceManagerUserResource
import com.tencent.devops.auth.pojo.PermissionInfo
import com.tencent.devops.auth.pojo.UserPermissionInfo
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@Service
class ManagerService @Autowired constructor(
    val client: Client
) {
    private val userPermissionMap = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build<String/*userId*/, Map<String/*organizationId*/, UserPermissionInfo>>()

    fun isManagerPermission(userId: String, projectId: String) : Boolean {

        val cacheManagerInfo = userPermissionMap.getIfPresent(userId)

        val manageInfo = if (cacheManagerInfo == null) {
            val remoteManagerInfo = client.get(ServiceManagerUserResource::class).getManagerInfo(userId)
            userPermissionMap.put(userId, remoteManagerInfo.data)
            remoteManagerInfo.data
        }  else {
            userPermissionMap.getIfPresent(userId)
        }
        logger.info("user managerInfo $userId| $manageInfo")
        if (manageInfo == null) {
            return false
        }

        val orgList = manageInfo.keys

        client.get()
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
