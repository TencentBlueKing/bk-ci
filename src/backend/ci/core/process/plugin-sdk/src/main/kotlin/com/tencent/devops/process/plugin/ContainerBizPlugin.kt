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

package com.tencent.devops.process.plugin

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.enums.ChannelCode

/**
 * 对流水线的Container的业务处理扩展点
 */
interface ContainerBizPlugin<T : Container> {

    /**
     * 取当前泛型Container的类
     */
    fun containerClass(): Class<T>

    /**
     * 创建Container后调用针对该Container的业务处理
     * @param container Container泛型
     * @param projectId 项目Code
     * @param pipelineId 流水线Id
     * @param pipelineName 流水线名称
     * @param userId 操作人
     * @param channelCode 渠道
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
     * 删除Container之前调用的业务处理
     * @param container Container泛型
     * @param userId 操作人
     * @param pipelineId 流水线ID
     */
    fun beforeDelete(container: T, userId: String, pipelineId: String?)

    /**
     * 检查Container是否符合自己的要求
     * @param container container
     * @param appearedCnt 出现次数
     */
    fun check(container: T, appearedCnt: Int)
}
