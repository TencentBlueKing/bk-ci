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

package com.tencent.devops.store.common.handler

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.constant.KEY_VALID_OS_ARCH_FLAG
import com.tencent.devops.common.api.constant.KEY_VALID_OS_NAME_FLAG
import com.tencent.devops.store.common.service.StorePipelineService
import com.tencent.devops.store.pojo.common.KEY_STORE_ID
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.handler.Handler
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import org.springframework.stereotype.Service

@Service
class StoreUpdateRunPipelineHandler(
    private val storePipelineService: StorePipelineService
) : Handler<StoreUpdateRequest> {

    override fun canExecute(handlerRequest: StoreUpdateRequest): Boolean {
        return handlerRequest.baseInfo.storeType != StoreTypeEnum.TEMPLATE
    }

    override fun execute(handlerRequest: StoreUpdateRequest) {
        // 运行组件内置流水线
        val bkStoreContext = handlerRequest.bkStoreContext
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        // 生成运行流水线参数
        val storeRunPipelineParam = StoreRunPipelineParam(
            userId = userId,
            storeId = bkStoreContext[KEY_STORE_ID].toString(),
            validOsNameFlag = bkStoreContext[KEY_VALID_OS_NAME_FLAG] as? Boolean,
            validOsArchFlag = bkStoreContext[KEY_VALID_OS_ARCH_FLAG] as? Boolean
        )
        storePipelineService.runPipeline(storeRunPipelineParam)
    }
}
