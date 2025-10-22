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

package com.tencent.devops.common.api.pojo

import com.tencent.devops.common.api.enums.I18nSourceEnum
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.lang.reflect.Field

data class I18nFieldInfo(
    @get:Schema(title = "字段")
    val field: Field,
    @get:Schema(title = "字段所属对象")
    val entity: Any,
    @get:Schema(title = "翻译信息来源")
    val source: I18nSourceEnum = I18nSourceEnum.PROPERTIES,
    @get:Schema(title = "翻译类型")
    val translateType: I18nTranslateTypeEnum = I18nTranslateTypeEnum.NAME,
    @get:Schema(title = "字段前缀名称")
    val keyPrefixName: String = "",
    @get:Schema(title = "是否复用接口定义的公共前缀")
    val reusePrefixFlag: Boolean = true,
    @get:Schema(title = "字段转换名称")
    val convertName: String = ""
)
