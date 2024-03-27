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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.handler.StoreUpdateDataPersistHandler
import com.tencent.devops.store.common.handler.StoreUpdateHandlerChain
import com.tencent.devops.store.common.handler.StoreUpdateParamCheckHandler
import com.tencent.devops.store.common.handler.StoreUpdateParamI18nConvertHandler
import com.tencent.devops.store.common.handler.StoreUpdateRunPipelineHandler
import com.tencent.devops.store.common.service.StoreReleaseService
import com.tencent.devops.store.pojo.common.KEY_STORE_ID
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import com.tencent.devops.store.pojo.common.publication.StoreUpdateResponse
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreReleaseServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeUpdateParamI18nConvertHandler: StoreUpdateParamI18nConvertHandler,
    private val storeUpdateParamCheckHandler: StoreUpdateParamCheckHandler,
    private val storeUpdateDataPersistHandler: StoreUpdateDataPersistHandler,
    private val storeUpdateRunPipelineHandler: StoreUpdateRunPipelineHandler
) : StoreReleaseService {

    private val logger = LoggerFactory.getLogger(StoreReleaseServiceImpl::class.java)
    override fun createComponent(userId: String, storeCreateRequest: StoreCreateRequest): StoreUpdateResponse? {
        TODO("Not yet implemented")
    }

    override fun updateComponent(userId: String, storeUpdateRequest: StoreUpdateRequest): StoreUpdateResponse? {
        val handlerList = mutableListOf(
            storeUpdateParamI18nConvertHandler, // 参数国际化处理
            storeUpdateParamCheckHandler, // 参数检查处理
            storeUpdateDataPersistHandler, // 数据持久化处理
            storeUpdateRunPipelineHandler // 运行内置流水线
        )
        StoreUpdateHandlerChain(handlerList).handleRequest(storeUpdateRequest)
        val bkStoreContext = storeUpdateRequest.bkStoreContext
        val storeId = bkStoreContext[KEY_STORE_ID]?.toString()
        return if (!storeId.isNullOrBlank()) {
            StoreUpdateResponse(storeId = storeId)
        } else {
            null
        }
    }
}
