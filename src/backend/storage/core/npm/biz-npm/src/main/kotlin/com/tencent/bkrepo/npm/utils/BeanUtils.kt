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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.npm.utils

import com.google.common.collect.Maps
import com.google.gson.JsonArray
import org.springframework.cglib.beans.BeanMap

object BeanUtils {
    /**
     * 将对象转换为map
     *
     * @param bean
     * @return
     */
    fun <T> beanToMap(bean: T?): Map<String, String> {
        val map = Maps.newHashMap<String, String>()
        if (bean != null) {
            val beanMap = BeanMap.create(bean)
            for (key in beanMap.keys) {
                var value = beanMap[key] ?: continue
                // if(StringUtils.isEmpty(value as String)) continue
                if (value is JsonArray) {
                    value = GsonUtils.gson.toJson(value)
                }
                map[key.toString()] = value as String
            }
        }
        return map
    }

    /**
     * 将map转换为javabean对象
     *
     * @param map
     * @param bean
     * @return
     */
    fun <T> mapToBean(map: Map<String, Any>, bean: T): T {
        val beanMap = BeanMap.create(bean)
        beanMap.putAll(map)
        return bean
    }
}
