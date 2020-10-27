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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.repository.service.impl

import com.tencent.bkrepo.auth.api.ServiceRoleResource
import com.tencent.bkrepo.auth.api.ServiceUserResource
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.security.http.HttpAuthProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.mongodb.core.MongoTemplate

/**
 * 服务抽象类，封装公共逻辑
 */
abstract class AbstractService {

    @Autowired
    open lateinit var roleResource: ServiceRoleResource

    @Autowired
    open lateinit var userResource: ServiceUserResource

    @Autowired
    open lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var authProperties: HttpAuthProperties

    fun publishEvent(any: Any) {
        eventPublisher.publishEvent(any)
    }

    fun createRepoManager(projectId: String, repoName: String, userId: String) {
        try {
            if (userId != ANONYMOUS_USER) {
                val repoManagerRoleId = roleResource.createRepoManage(projectId, repoName).data!!
                userResource.addUserRole(userId, repoManagerRoleId)
            }
        } catch (ignored: RuntimeException) {
            if (authProperties.enabled) {
                throw ignored
            } else {
                logger.warn("Create repository manager failed, ignore exception due to auth disabled[${ignored.message}].")
            }
        }
    }

    fun createProjectManager(projectId: String, userId: String) {
        try {
            if (userId != ANONYMOUS_USER) {
                val projectManagerRoleId = roleResource.createProjectManage(projectId).data!!
                userResource.addUserRole(userId, projectManagerRoleId)
            }
        } catch (ignored: RuntimeException) {
            if (authProperties.enabled) {
                throw ignored
            } else {
                logger.warn("Create project manager failed, ignore exception due to auth disabled[${ignored.message}].")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractService::class.java)
    }
}
