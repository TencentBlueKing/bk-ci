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

package com.tencent.devops.common.pipeline.container

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel

@ApiModel("流水线模型-多态基类")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes(
    JsonSubTypes.Type(value = TriggerContainer::class, name = TriggerContainer.classType),
    JsonSubTypes.Type(value = NormalContainer::class, name = NormalContainer.classType),
    JsonSubTypes.Type(value = VMBuildContainer::class, name = VMBuildContainer.classType)
)
interface Container {
    var id: String? // seq id
    val name: String
    val elements: List<Element>
    var status: String?
    var startEpoch: Long?
    var systemElapsed: Long? // 系统耗时（开机时间）
    var elementElapsed: Long? // 插件执行耗时
    var canRetry: Boolean? // 当前job是否能重试
    var containerId: String? // container 唯一ID

    /**
     * 只存储Container相关的配置，elements不会存储。
     */
    fun genTaskParams(): MutableMap<String, Any> {
        val configParams = JsonUtil.toMutableMapSkipEmpty(this)
        if (elements.isNotEmpty()) {
            configParams["elements"] = listOf<Element>() // ignore elements storage
        }
        return configParams
    }

    fun getClassType(): String
}
