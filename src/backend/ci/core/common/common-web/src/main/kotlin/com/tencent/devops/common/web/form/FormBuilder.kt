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

package com.tencent.devops.common.web.form

import com.tencent.devops.common.web.form.data.CheckboxPropData
import com.tencent.devops.common.web.form.data.FormDataType
import com.tencent.devops.common.web.form.data.FormPropData
import com.tencent.devops.common.web.form.models.Form
import com.tencent.devops.common.web.form.models.FormProp

/**
 * 表单对象的建造者类
 */
class FormBuilder {
    private val form = Form("", required = mutableListOf(), properties = mutableMapOf())

    fun setTitle(title: String): FormBuilder {
        form.title = title
        return this
    }

    fun setDescription(description: String): FormBuilder {
        form.description = description
        return this
    }

    fun setProp(prop: FormPropData): FormBuilder {

        val type = when (prop) {
            // checkbox只能是array才能成立
            is CheckboxPropData -> FormDataType.ARRAY.value
            else -> prop.type.value
        }

        form.properties[prop.id] = FormProp(
            type = type,
            title = prop.title,
            default = prop.default,
            description = prop.description,
            uiComponent = prop.buildComponent(),
            uiProps = prop.buildUiProps(),
            uiRules = if (prop.required == true) {
                listOf("required")
            } else {
                null
            }
        )

        // 添加必填选项
        if (prop.required == true) {
            form.required.add(prop.id)
        }

        return this
    }

    fun build(): Form {
        return form
    }
}
