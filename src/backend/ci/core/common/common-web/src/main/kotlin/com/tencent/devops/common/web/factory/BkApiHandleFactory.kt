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

package com.tencent.devops.common.web.factory

import com.tencent.devops.common.web.constant.BkApiHandleType
import com.tencent.devops.common.web.service.BkApiHandleService
import com.tencent.devops.common.web.service.impl.BkApiHandleBuildAuthServiceImpl
import com.tencent.devops.common.web.service.impl.BkApiHandleOpenAccessServiceImpl
import com.tencent.devops.common.web.service.impl.BkApiHandlePipelineAccessServiceImpl
import com.tencent.devops.common.web.service.impl.BkApiHandleProjectAccessServiceImpl
import com.tencent.devops.common.web.service.impl.BkApiHandleProjectMemberCheckServiceImpl
import java.util.concurrent.ConcurrentHashMap

object BkApiHandleFactory {

    private val bkApiHandleMap = ConcurrentHashMap<String, BkApiHandleService>()

    fun createBuildApiHandleService(
        type: BkApiHandleType
    ): BkApiHandleService? {
        var bkApiHandleService = bkApiHandleMap[type.name]
        when (type) {
            BkApiHandleType.BUILD_API_AUTH_CHECK -> {
                if (bkApiHandleService == null) {
                    bkApiHandleService = BkApiHandleBuildAuthServiceImpl()
                    bkApiHandleMap[type.name] = bkApiHandleService
                }
            }

            BkApiHandleType.PROJECT_API_ACCESS_LIMIT -> {
                if (bkApiHandleService == null) {
                    bkApiHandleService = BkApiHandleProjectAccessServiceImpl()
                    bkApiHandleMap[type.name] = bkApiHandleService
                }
            }

            BkApiHandleType.PIPELINE_API_ACCESS_LIMIT -> {
                if (bkApiHandleService == null) {
                    bkApiHandleService = BkApiHandlePipelineAccessServiceImpl()
                    bkApiHandleMap[type.name] = bkApiHandleService
                }
            }

            BkApiHandleType.API_OPEN_TOKEN_CHECK -> {
                if (bkApiHandleService == null) {
                    bkApiHandleService = BkApiHandleOpenAccessServiceImpl()
                    bkApiHandleMap[type.name] = bkApiHandleService
                }
            }
            BkApiHandleType.PROJECT_MEMBER_CHECK -> {
                if (bkApiHandleService == null) {
                    bkApiHandleService = BkApiHandleProjectMemberCheckServiceImpl()
                    bkApiHandleMap[type.name] = bkApiHandleService
                }
            }
            else -> {}
        }
        return bkApiHandleService
    }
}
