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

import com.tencent.devops.common.web.form.models.ui.components.SelectComponent
import com.tencent.devops.common.web.form.models.ui.props.UiProps
import com.tencent.devops.common.web.form.models.ui.props.UiPropsCommon

data class SelectPropData(
    override val id: String,
    override val type: FormDataType,
    override val title: String,
    override val default: Any? = null,
    override val required: Boolean? = false,
    override val description: String?,
    // select 组件名字有不同的
    val componentName: String? = null,
    val option: SelectPropOption? = null
) : FormPropData {
    override fun buildComponent(): SelectComponent {
        return SelectComponent(
            name = componentName ?: "select",
            props = when {
                option != null -> this.buildProps(
                    mapOf(
                        "options" to option.items,
                        "optionsConf" to option.conf
                    )
                )
                else -> null
            }
        )
    }

    override fun buildUiProps(): UiProps {
        return UiPropsCommon(this.titleWidth)
    }
}

data class SelectPropOption(
    val items: List<SelectPropOptionItem>?,
    val conf: SelectPropOptionConf
)

data class SelectPropOptionItem(
    val id: String,
    val name: String,
    val description: String?
)

data class SelectPropOptionConf(
    val url: String? = null,
    val paramId: String? = null,
    val paramName: String? = null,
    val dataPath: String? = null,
    val itemTargetUrl: String? = null,
    val hasAddItem: Boolean? = null,
    val itemText: String? = null,
    val searchable: Boolean? = null,
    val multiple: Boolean? = null,
    val clearable: Boolean? = null
)
