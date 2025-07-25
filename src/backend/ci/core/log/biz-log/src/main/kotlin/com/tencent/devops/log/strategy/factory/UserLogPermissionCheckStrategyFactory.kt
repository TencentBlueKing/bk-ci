/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.log.strategy.factory

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.log.strategy.bus.IUserLogPermissionCheckStrategy
import com.tencent.devops.log.strategy.bus.impl.UserArchivedLogPermissionCheckStrategy
import com.tencent.devops.log.strategy.bus.impl.UserNormalLogPermissionCheckStrategy

object UserLogPermissionCheckStrategyFactory {

    private const val ARCHIVED_LOG_STRATEGY = "archivedLogStrategy"

    private const val NORMAL_LOG_STRATEGY = "normalLogStrategy"

    private val logStrategyMap = HashMap<String, IUserLogPermissionCheckStrategy>()

    init {
        // 初始化策略
        logStrategyMap[ARCHIVED_LOG_STRATEGY] =
            SpringContextUtil.getBean(UserArchivedLogPermissionCheckStrategy::class.java)
        logStrategyMap[NORMAL_LOG_STRATEGY] =
            SpringContextUtil.getBean(UserNormalLogPermissionCheckStrategy::class.java)
    }

    fun createUserLogPermissionCheckStrategy(
        archiveFlag: Boolean? = null
    ): IUserLogPermissionCheckStrategy {
        val key = if (archiveFlag == true) {
            ARCHIVED_LOG_STRATEGY
        } else {
            NORMAL_LOG_STRATEGY
        }
        return logStrategyMap[key] ?: throw ErrorCodeException(errorCode = CommonMessageCode.SYSTEM_ERROR)
    }
}
