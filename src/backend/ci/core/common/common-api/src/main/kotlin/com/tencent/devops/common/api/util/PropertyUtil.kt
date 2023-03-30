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

package com.tencent.devops.common.api.util

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

object PropertyUtil {

    private val propertiesMap = ConcurrentHashMap<String, Properties>()

    /**
     * 获取配置项的值
     * @param propertyKey 配置项KEY
     * @param propertyFileName 配置文件名
     * @return 配置项的值
     */
    fun getPropertyValue(propertyKey: String, propertyFileName: String): String {
        // 从缓存中获取配置项的值
        var properties = propertiesMap[propertyFileName]
        if (properties == null) {
            // 缓存中如果没有该配置文件的值则实时去加载配置文件去获取
            val fileInputStream = PropertyUtil::class.java.getResourceAsStream(propertyFileName)
            properties = Properties()
            properties.load(fileInputStream)
            // 将该配置文件的properties信息存入缓存
            propertiesMap[propertyFileName] = properties
        }
        val propertyValue = properties[propertyKey]
            ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(propertyKey)
            )
        return propertyValue.toString()
    }

    /**
     * 基于资源文件内容获取配置项的值
     * @param propertyKey 配置项KEY
     * @param propertyFileContent 资源文件内容
     * @return 配置项的值
     */
    fun getPropertyValueByContent(propertyKey: String, propertyFileContent: String): String? {
        val properties = Properties()
        properties.load(propertyFileContent.reader())
        return properties[propertyKey]?.toString()
    }
}
