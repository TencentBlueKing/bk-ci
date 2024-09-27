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
package com.tencent.devops.openapi.filter.manager.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.openapi.constant.OpenAPIMessageCode.ILLEGAL_USER
import com.tencent.devops.openapi.filter.manager.ApiFilterFlowState
import com.tencent.devops.openapi.filter.manager.ApiFilterManager
import com.tencent.devops.openapi.filter.manager.FilterContext
import com.tencent.devops.openapi.service.op.AppUserInfoService
import com.tencent.devops.openapi.service.op.OpAppUserService
import jakarta.ws.rs.core.Response
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ManagerUserApiFilter(
    private val redisOperation: RedisOperation,
    private val appUserInfoService: AppUserInfoService,
    private val opAppUserService: OpAppUserService,
    private val bkTag: BkTag
) : ApiFilterManager {

    companion object {
        private val logger = LoggerFactory.getLogger(ManagerUserApiFilter::class.java)
        const val FILTER_RUN_FLAG_PREFIX = "BK:OPENAPI:FILTER:FLAG:TAG:"
    }

    /*返回true时执行check逻辑*/
    override fun canExecute(requestContext: FilterContext): Boolean {
        return true
    }

    override fun verify(requestContext: FilterContext): ApiFilterFlowState {
        val userId = requestContext.requestContext.getHeaderString(AUTH_HEADER_USER_ID)
        if (userId == null) {
            val appCode = requestContext.requestContext.getHeaderString(AUTH_HEADER_DEVOPS_APP_CODE)
            logger.info("path: ${requestContext.requestContext.uriInfo.path} not need userHead, appCode: $appCode ")
            return ApiFilterFlowState.CONTINUE
        }
        if (opAppUserService.checkUser(userId)) {
            logger.info("path: ${requestContext.requestContext.uriInfo.path} , user id: $userId, and is rtx person.")
            return ApiFilterFlowState.CONTINUE
        }

        val appCode = requestContext.requestContext.getHeaderString(AUTH_HEADER_DEVOPS_APP_CODE)
        logger.info("$userId is not rtx user, appCode: $appCode , path: ${requestContext.requestContext.uriInfo.path}")
        val appManagerUser = appCode?.let { appUserInfoService.get(appCode) }
        if (appManagerUser.isNullOrEmpty()) {
            logger.warn("$userId is not rtx user, appCode: $appCode not has manager")
            if (redisOperation.get("$FILTER_RUN_FLAG_PREFIX${bkTag.getLocalTag()}") != null) {
                requestContext.requestContext.abortWith(
                    Response.status(Response.Status.BAD_REQUEST)
                        .entity(
                            MessageUtil.getMessageByLocale(
                                messageCode = ILLEGAL_USER,
                                language = I18nUtil.getLanguage(userId)
                            )
                        ).build()
                )
                return ApiFilterFlowState.BREAK
            }
        } else {
            requestContext.requestContext.headers.putSingle(AUTH_HEADER_USER_ID, appManagerUser)
        }
        return ApiFilterFlowState.CONTINUE
    }
}
