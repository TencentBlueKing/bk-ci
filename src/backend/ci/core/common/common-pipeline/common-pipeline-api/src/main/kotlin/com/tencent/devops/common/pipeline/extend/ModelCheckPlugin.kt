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

package com.tencent.devops.common.pipeline.extend

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container

/**
 * 对流水线模型中的设置的agent进行检查的扩展点
 * @version 1.0
 */
interface ModelCheckPlugin {

    /**
     * 检查model完整性
     * @param
     * @throws RuntimeException 子类  将检查失败或异常的以RuntimeException子类抛出
     */
    fun checkModelIntegrity(model: Model)

    fun checkJob(jobContainer: Container, projectId: String, pipelineId: String, userId: String)

    /**
     * 清理Model--不删除里面的Element内的逻辑
     */
    fun clearUpModel(model: Model)

    /**
     * 在删除lement前做的一些处理
     * 对比sourceModel，并清理model中与之不同的Element
     * @param userId 操作人
     * @param existModel 目标Model（要清理的Model)
     * @param sourceModel 源要比较的Model
     * @param pipelineId 流水线id
     */
    fun beforeDeleteElementInExistsModel(
        userId: String,
        existModel: Model,
        sourceModel: Model? = null,
        pipelineId: String?
    )
}
