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

package com.tencent.devops.common.api.pojo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.api.util.MessageUtil

enum class ErrorType(
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "errorType", reusePrefixFlag = false)
    val typeName: String,
    val num: Int
) {
    SYSTEM("system", 0), // 0 系统运行报错
    USER("user", 1), // 1 用户配置报错
    THIRD_PARTY("thirdParty", 2), // 2 第三方系统接入错误
    PLUGIN("plugin", 3); // 3 插件执行错误

    companion object {

        fun getErrorType(name: String?): ErrorType? {
            values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getErrorType(ordinal: Int?): ErrorType {
            return when (ordinal) {
                0 -> SYSTEM
                1 -> USER
                2 -> THIRD_PARTY
                else -> PLUGIN
            }
        }
    }

    fun getI18n(language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = "errorType.${this.typeName}",
            language = language
        )
    }
}
