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
package com.tencent.devops.store.config

import com.tencent.devops.common.client.consul.ConsulConstants.PROJECT_TAG_REDIS_KEY
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class StoreApiAspect(
    private val redisOperation: RedisOperation,
    private val bkTag: BkTag
) {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreApiAspect::class.java)
    }

    /**
     * 前置增强：目标方法执行之前执行
     *
     * @param jp
     */
    @Before("within(com.tencent.devops.store.resources..*)") // 所有controller包下面的所有方法的所有参数
    @Suppress("ComplexMethod")
    fun beforeMethod(jp: JoinPoint) {
        // 参数value
        val parameterValue = jp.args
        // 参数key
        val parameterNames = (jp.signature as MethodSignature).parameterNames
        var projectId: String? = null

        for (index in parameterValue.indices) {
            when (parameterNames[index]) {
                "projectId" -> projectId = parameterValue[index]?.toString()
                "projectCode" -> projectId = parameterValue[index]?.toString()
                else -> Unit
            }
        }
        if (projectId != null) {
            logger.info("Store Aspect $projectId ")
            // 网关无法判别项目信息, 切面捕获project信息。
            val projectConsulTag = redisOperation.hget(PROJECT_TAG_REDIS_KEY, projectId)
            if (!projectConsulTag.isNullOrEmpty()) {
                bkTag.setGatewayTag(projectConsulTag)
            }
        }
    }

    /**
     * 后置增强：目标方法执行之前执行
     *
     */
    @After("within(com.tencent.devops.store.resources..*)") // 所有controller包下面的所有方法的所有参数
    fun afterMethod() {
        // 删除线程ThreadLocal数据,防止线程池复用。导致流量指向被污染
        bkTag.removeGatewayTag()
    }
}
