/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.pojo.configuration

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import io.swagger.annotations.ApiModelProperty

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    defaultImpl = LocalConfiguration::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = LocalConfiguration::class, name = "rpm-local"), // 兼容处理
    JsonSubTypes.Type(value = LocalConfiguration::class, name = LocalConfiguration.type),
    JsonSubTypes.Type(value = RemoteConfiguration::class, name = RemoteConfiguration.type),
    JsonSubTypes.Type(value = VirtualConfiguration::class, name = VirtualConfiguration.type),
    JsonSubTypes.Type(value = CompositeConfiguration::class, name = CompositeConfiguration.type)
)
open class RepositoryConfiguration {
    /**
     * 设置项
     * 不同类型仓库可以通过该字段进行差异化配置
     */
    @ApiModelProperty("设置项", required = false)
    val settings: MutableMap<String, Any> = mutableMapOf()

    /**
     * 根据属性名[key]获取自定义context属性
     */
    @JsonIgnore
    inline fun <reified T> getSetting(key: String): T? {
        val value = settings[key]
        require(value is T?)
        return value
    }

    /**
     * 根据属性名[key]获取字符串类型设置项
     */
    @JsonIgnore
    fun getSetting(key: String): String? {
        return settings[key]?.toString()
    }

    /**
     * 根据属性名[key]获取字符串类型设置项
     */
    @JsonIgnore
    fun getStringSetting(key: String): String? {
        return settings[key]?.toString()
    }

    /**
     * 根据属性名[key]获取Boolean类型设置项
     */
    @JsonIgnore
    fun getBooleanSetting(key: String): Boolean? {
        return settings[key]?.toString()?.toBoolean()
    }

    /**
     * 获取整数类型设置项
     */
    @JsonIgnore
    fun getIntegerSetting(key: String): Int? {
        return settings[key]?.toString()?.toInt()
    }
}
