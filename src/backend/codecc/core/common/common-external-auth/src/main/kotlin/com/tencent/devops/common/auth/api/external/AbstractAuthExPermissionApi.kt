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

package com.tencent.devops.common.auth.api.external

import com.tencent.devops.common.auth.api.pojo.external.KEY_ADMIN_MEMBER
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.util.List2StrUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate

abstract class AbstractAuthExPermissionApi @Autowired constructor(
        val client: Client,
        val authPropertiesData: AuthExPropertiesData,
        val redisTemplate: RedisTemplate<String, String>
) : AuthExPermissionApi {

    /**
     * 校验用户是否是管理员
     */
    override fun isAdminMember(
            user: String
    ): Boolean {
        logger.debug("judge user is admin member: {}", user)
        val adminMemberStr = redisTemplate.opsForValue().get(KEY_ADMIN_MEMBER)
        val adminMembers = List2StrUtil.fromString(adminMemberStr, ComConstants.SEMICOLON)
        return if (adminMembers.contains(user)) {
            logger.debug("Is admin member: {}", user)
            true
        } else {
            logger.debug("Not admin member: {}", user)
            false
        }
    }

    /**
     * 查询任务创建来源
     */
    override fun getTaskCreateFrom(
            taskId: Long
    ): String {
        val authTaskService = SpringContextUtil.getBean(AuthTaskService::class.java)
        return authTaskService.getTaskCreateFrom(taskId)
    }


    /**
     * 查询任务所属流水线ID
     */
    override fun getTaskPipelineId(
            taskId: Long
    ): String {
        val authTaskService = SpringContextUtil.getBean(AuthTaskService::class.java)
        return authTaskService.getTaskPipelineId(taskId)
    }


    companion object {
        private val logger = LoggerFactory.getLogger(AbstractAuthExPermissionApi::class.java)

        @Value("\${common.codecc.env:#{null}}")
        val env: String? = null
    }
}