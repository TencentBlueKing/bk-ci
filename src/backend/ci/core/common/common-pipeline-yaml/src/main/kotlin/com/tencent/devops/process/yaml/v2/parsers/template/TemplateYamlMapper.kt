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

package com.tencent.devops.process.yaml.v2.parsers.template

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tencent.devops.common.api.util.ReflectUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.process.yaml.v2.models.YAME_META_DATA_JSON_FILTER

/**
 * 部分yaml转换过程与公共的存在区别，template转换时需要保留meta信息字段
 */
object TemplateYamlMapper {
    private val objectMapper = ObjectMapper(
        YAMLFactory().disable(YAMLGenerator.Feature.SPLIT_LINES)
    ).registerKotlinModule().setFilterProvider(
        SimpleFilterProvider().addFilter(
            YAME_META_DATA_JSON_FILTER, SimpleBeanPropertyFilter.serializeAll()
        )
    )

    fun getObjectMapper() = objectMapper

    fun toYaml(bean: Any): String {
        if (ReflectUtil.isNativeType(bean) || bean is String) {
            return bean.toString()
        }
        return getObjectMapper().writeValueAsString(bean)!!
    }

    fun <T> to(yamlStr: String): T {
        val obj = YamlUtil.loadYamlRetryOnAccident(yamlStr)
        return getObjectMapper().readValue(obj, object : TypeReference<T>() {})
    }
}
