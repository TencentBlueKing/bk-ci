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

package com.tencent.bkrepo.common.api.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.InputStream

/**
 * yaml工具类
 */
object YamlUtils {
    val objectMapper: ObjectMapper = YAMLMapper().apply {
        registerKotlinModule()
        enable(SerializationFeature.INDENT_OUTPUT)
        // 缺省文件以三个横杠开头 禁用该属性
        disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
    }
}

/**
 * 将对象序列化为yaml字符串
 */
fun Any.toYamlString() = YamlUtils.objectMapper.writeValueAsString(this).orEmpty()

/**
 * 将yaml字符串反序列化为对象
 */
inline fun <reified T> String.readYamlString(): T = YamlUtils.objectMapper.readValue(this, jacksonTypeRef<T>())

/**
 * 将yaml字符串流反序列化为对象
 */
inline fun <reified T> InputStream.readYamlString(): T = YamlUtils.objectMapper.readValue(this, jacksonTypeRef<T>())
