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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.UpdateStorePipelineModelRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreRunPipelineParam

interface StorePipelineService {

    /**
     * @param userId 用户ID
     * @param updateStorePipelineModelRequest 更新研发商店流水线模型请求报文
     */
    fun updatePipelineModel(
        userId: String,
        updateStorePipelineModelRequest: UpdateStorePipelineModelRequest
    ): Result<Boolean>

    /**
     * 运行流水线
     * @param storeRunPipelineParam 运行流水线参数
     * @return 布尔值
     */
    fun runPipeline(
        storeRunPipelineParam: StoreRunPipelineParam
    ): Boolean

    // 创建组件内置流水线
    fun creatStorePipelineByStoreCode(
        storeCode: String? = null,
        storeType: String
    ): String

    /**
     * 删除组件内置流水线
     * @param userId 用户ID
     * @param storeType 组件类型
     * @param storeCode 组件标识
     * @param excludeProjectCode 需排除的项目
     * @return 布尔值
     */
    fun deleteStoreInnerPipeline(
        userId: String,
        storeType: StoreTypeEnum? = null,
        storeCode: String? = null,
        excludeProjectCode: String? = null
    ): Boolean
}
