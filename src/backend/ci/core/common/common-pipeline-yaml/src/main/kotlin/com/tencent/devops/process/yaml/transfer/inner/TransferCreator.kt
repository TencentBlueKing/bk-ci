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

package com.tencent.devops.process.yaml.transfer.inner

import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.process.yaml.v3.models.step.Step

/**
 * ModelCreate的内部类，用来放一些不同使用者的不同方法和参数
 */
interface TransferCreator {
    // 控制run插件是否是研发商店插件
    val marketRunTask: Boolean

    // 研发商店的run插件的code
    val runPlugInAtomCode: String?

    // 研发商店的run插件的版本
    val runPlugInVersion: String?

    // 默认的公共镜像
    val defaultImageCode: String

    // 默认的公共镜像版本
    val defaultImageVersion: String

    /**
     * 构造具有特殊语法的checkout插件
     * @param step 当前step对象
     * @param event model创建的总事件
     * @param additionalOptions 插件的控制参数
     */
    fun transferCheckoutElement(
        step: Step
    ): MarketBuildAtomElement

    /**
     * 构造编译类的插件
     */
    fun transferMarketBuildAtomElement(
        step: Step
    ): MarketBuildAtomElement

    /**
     * 构造编译类的插件
     */
    fun transferMarketBuildLessAtomElement(
        step: Step
    ): MarketBuildLessAtomElement

    fun defaultLinuxDispatchType(): BuildType
}
