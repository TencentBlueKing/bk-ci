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
package com.tencent.devops.openapi.filter

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.RequestFilter
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.api.constant.OpenAPIMessageCode.ILLEGAL_USER
import com.tencent.devops.openapi.service.op.AppUserInfoService
import com.tencent.devops.openapi.service.op.OpAppUserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.PreMatching
import javax.ws.rs.ext.Provider

@Component
@Provider
@PreMatching
@RequestFilter
@Suppress("ALL")
@Priority(Priorities.USER)
class UserFilter @Autowired constructor(
    val redisOperation: RedisOperation,
    val appUserInfoService: AppUserInfoService,
    val opAppUserService: OpAppUserService,
    val bkTag: BkTag
) : ContainerRequestFilter {
    override fun filter(requestContext: ContainerRequestContext?) {
        if (requestContext == null) {
            return
        }
        val userId = requestContext.getHeaderString(AUTH_HEADER_USER_ID)
        if (userId == null) {
            val appCode = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_APP_CODE)
            logger.info("path: ${requestContext.uriInfo.path} not need userHead, appCode: $appCode ")
            return
        } else {
            if (!opAppUserService.checkUser(userId)) {
                val appCode = requestContext.getHeaderString(AUTH_HEADER_DEVOPS_APP_CODE)
                logger.info("$userId is not rtx user, appCode: $appCode , path: ${requestContext.uriInfo.path}")
                val appManagerUser = appUserInfoService.get(appCode)
                if (appManagerUser.isNullOrEmpty()) {
                    logger.warn("$userId is not rtx user, appCode: $appCode not has manager")
                    if (redisOperation.get("$FILTER_RUN_FLAG_PREFIX${bkTag.getLocalTag()}") != null) {
                        throw ParamBlankException(
                            MessageUtil.getMessageByLocale(
                                messageCode = ILLEGAL_USER,
                                language = I18nUtil.getLanguage(userId)
                            )
                        )
                    }
                } else {
                    requestContext.headers.putSingle(AUTH_HEADER_USER_ID, appManagerUser)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserFilter::class.java)
        const val FILTER_RUN_FLAG_PREFIX = "BK:OPENAPI:FILTER:FLAG:TAG:"
    }
}
