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

package com.tencent.devops.common.web.form.data

import com.tencent.devops.common.web.form.models.ui.components.UiComponent
import com.tencent.devops.common.web.form.models.ui.props.UiProps

/**
 * 给构造器使用的，用户填写的表单项数据
 */
interface FormPropData {
    // 必需， 单个表单组件的key，根据业务需求自定义
    val id: String

    // 必需，表单组件value的数据类型
    val type: FormDataType

    // 必需，表单组件的label
    val title: String

    // label宽度,默认500
    val titleWidth: Int
        get() = 500

    // 可选，表单组件的默认值
    val default: Any?

    // 可选，是否为必填项，默认为false
    val required: Boolean?

    // 可选，组件描述
    val description: String?

    // 构造表单对象
    fun buildComponent(): UiComponent

    // 构造UI对象
    fun buildUiProps(): UiProps
}

@Suppress("ComplexCondition")
fun FormPropData.buildProps(props: Map<String, Any?>?): Map<String, Any>? {
    if (props == null) {
        return null
    }

    val result = mutableMapOf<String, Any>()

    props.forEach { (name, value) ->
        if (value == null || (value is Iterable<*> && value.count() == 0) || (value is Map<*, *> && value.isEmpty())) {
            return@forEach
        }

        result[name] = value
    }

    return if (result.isEmpty()) {
        null
    } else {
        result
    }
}
