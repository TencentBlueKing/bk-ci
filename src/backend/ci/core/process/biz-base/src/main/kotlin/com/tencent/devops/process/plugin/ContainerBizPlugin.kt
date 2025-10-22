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

package com.tencent.devops.process.plugin

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.ChannelCode

/**
 * 流水线的Container的编排插件处理器
 */
@Suppress("ALL")
interface ContainerBizPlugin<T : Container> {

    /**
     * 取当前泛型Container的类
     */
    fun containerClass(): Class<T>

    /**
     * 创建Job容器[container]后,根据项目ID[projectId]，流水线ID[pipelineId]
     * 流水线名称[pipelineName],操作人[userId],还有渠道[channelCode]
     * 进行创建后的处理
     */
    fun afterCreate(
        container: T,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode = ChannelCode.BS
    )

    /**
     * 删除Job容器[container]之前,根据操作人[userId]以及流水线ID[pipelineId]进行预处理
     */
    fun beforeDelete(container: T, userId: String, pipelineId: String?)

    /**
     * 检查Job是否符合出现次数[appearedCnt]的要求
     */
    fun check(container: T, appearedCnt: Int)
}
