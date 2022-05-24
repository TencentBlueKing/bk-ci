/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.bk.codecc.codeccjob.websocket

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.auth.api.external.AuthExPermissionApi
import com.tencent.devops.common.auth.api.external.AuthTaskService
import com.tencent.devops.common.auth.api.pojo.external.CodeCCAuthAction
import com.tencent.devops.common.auth.api.pojo.external.KEY_CREATE_FROM
import com.tencent.devops.common.auth.api.pojo.external.PREFIX_TASK_INFO
import com.tencent.devops.common.auth.api.pojo.external.PipelineAuthAction
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.constant.RedisKeyConstants
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.concurrent.TimeUnit

@Configuration
@EnableWebSocketMessageBroker
open class WebSocketConfig @Autowired constructor(
        private val bkAuthExPermissionApi: AuthExPermissionApi,
        private val authTaskService: AuthTaskService,
        private val client : Client,
        private val redisTemplate: RedisTemplate<String, String>
) : AbstractWebSocketMessageBrokerConfigurer() {

    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/topic")
        config.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/websocket/user/taskLog/analysisInfo").addInterceptors(
                object : HandshakeInterceptor {
                    override fun afterHandshake(request: ServerHttpRequest,
                                                response: ServerHttpResponse,
                                                wsHandler: WebSocketHandler,
                                                exception: java.lang.Exception?) {
                        logger.info("after hand shake, end point established")
                        val req = request as ServletServerHttpRequest
                        val taskId = req.servletRequest.getParameter(AUTH_HEADER_DEVOPS_TASK_ID)
                        if(!taskId.isNullOrBlank()){
                            //先看是否是开源扫描项目
                            val createFrom = redisTemplate.opsForHash<String, String>().get(PREFIX_TASK_INFO + taskId, KEY_CREATE_FROM)
                            //如果是的话，将task_id设置到缓存中，用于推送websocket
                            if(!createFrom.isNullOrBlank() && ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == createFrom){
                                redisTemplate.opsForValue().set("${RedisKeyConstants.TASK_WEBSOCKET_SESSION_PREFIX}$taskId", "1", TimeUnit.MINUTES.toSeconds(30), TimeUnit.SECONDS)
                            }
                        }
                    }

                    override fun beforeHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Boolean {
                        val req = request as ServletServerHttpRequest
                        val user = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_USER_ID)
                        val taskId = req.servletRequest.getParameter(AUTH_HEADER_DEVOPS_TASK_ID)
                        val projectId = req.servletRequest.getParameter(AUTH_HEADER_DEVOPS_PROJECT_ID)
                        // 如果是管理员就直接校验通过
                        if (bkAuthExPermissionApi.isAdminMember(user)) {
                            logger.info("current user is admin: $user")
                            return true
                        }
                        logger.info("before hand shake, end point establishing start! user: {}, task id: {}", user, taskId)
                        if (user.isNullOrBlank() || projectId.isNullOrBlank()) {
                            logger.error("insufficient param info! user: $user, taskId: $taskId, projectId: $projectId")
                            return false
                        }
                        if (taskId.isNullOrBlank()) {
                            val accessToken = req.servletRequest.getHeader(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
                            if (accessToken.isNullOrBlank()) {
                                logger.error("access token is null!")
                                return false
                            }
                            val verifyResult = client.getDevopsService(ServiceProjectResource::class.java)
                                .verifyUserProjectPermission(accessToken, projectId, user)
                            return verifyResult.isOk() && verifyResult.data ?: false
                        }
                        val taskCreateFrom = authTaskService.getTaskCreateFrom(taskId.toLong())
                        if(ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value() == taskCreateFrom) {
                            return if (!bkAuthExPermissionApi.validateGongfengPermission(user, taskId, projectId, listOf(CodeCCAuthAction.REPORT_VIEW))) {
                                logger.error("empty validate result: $user")
                                false
                            } else {
                                logger.info("gongfeng authorization pass, task id: $taskId")
                                true
                            }
                        }
                        val result = if (ComConstants.BsTaskCreateFrom.BS_PIPELINE.value() == taskCreateFrom) {
                            bkAuthExPermissionApi.validatePipelineBatchPermission(user,
                                taskId, projectId, mutableSetOf(PipelineAuthAction.VIEW.actionName))
                        } else {
                            bkAuthExPermissionApi.validateTaskBatchPermission(user,
                                taskId, projectId, mutableSetOf(CodeCCAuthAction.REPORT_VIEW.actionName))
                        }
                        if (result.isNullOrEmpty()) {
                            logger.error("empty validate result: $user")
                            return false
                        }
                        result.forEach {
                            if (it.isPass == true) {
                                logger.info("before hand shake validation finished!")
                                return true
                            }
                        }
                        logger.error("validate permission fail! user: $user")
                        return false
                    }
                }
        ).setAllowedOriginPatterns("*").withSockJS()
    }


}