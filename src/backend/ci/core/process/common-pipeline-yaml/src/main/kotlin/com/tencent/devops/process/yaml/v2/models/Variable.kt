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

package com.tencent.devops.process.yaml.v2.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Variable model
 * @param allowModifyAtStartup 手动触发/openapi触发时生效
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Variable(
    val value: String?,
    val readonly: Boolean? = false,
    @JsonProperty("allow-modify-at-startup")
    val allowModifyAtStartup: Boolean? = false,
    val props: VariableProps? = null
)

/**
 * Variable 属性变量
 * @param label 可选, 预定义下拉可选值的字段
 * @param type 类型
 * @param options 下拉列表可选值，和 datasource 二选一
 * @param datasource 下拉列表数据源，和 values 二选一
 * @param multiple 是否允许多选，缺省时为 false（type=selector时生效）
 * @param description 可选，描述
 * @param required 可选，是否必填
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VariableProps(
    val label: String? = null,
    val type: String,
    val options: List<VariablePropOption>? = null,
    val datasource: VariableDatasource? = null,
    val description: String? = null,
    val multiple: Boolean? = false,
    val required: Boolean? = false
)

/**
 * Variable 属性中的选项对象
 * @param id 预定义下拉可选值的字段
 * @param label 可选, 选项说明
 * @param description 可选, 选项描述
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VariablePropOption(
    val id: Any,
    val label: String? = null,
    val description: String? = null
)

/**
 * Variable Url子属性
 * @param url 请求数据的地址
 * @param dataPath 可选，选项列表数据所在的、API返回体json中的路径，没有此字段则默认为data， 示例：data.detail.list。配合url使用
 * @param paramId 可选，url返回规范中，用于下拉列表选项key的字段名，配合url使用
 * @param paramName 可选，url返回规范中，用于下拉列表选项label的字段名，配合url使用
 * @param hasAddItem 可选，是否有新增按钮
 * @param itemText 可选，新增按钮文字描述
 * @param itemTargetUrl 可选，点击新增按钮的跳转地址
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class VariableDatasource(
    val url: String,
    @JsonProperty("data-path")
    val dataPath: String? = null,
    @JsonProperty("param-id")
    val paramId: String? = null,
    @JsonProperty("param-name")
    val paramName: String? = null,
    @JsonProperty("has-add-item")
    val hasAddItem: Boolean? = true,
    @JsonProperty("item-text")
    val itemText: String? = null,
    @JsonProperty("item-target-url")
    val itemTargetUrl: String? = null
)

enum class VariablePropType(val value: String) {
    VUEX_INPUT("vuex-input"),
    VUEX_TEXTAREA("vuex-textarea"),
    SELECTOR("selector"),
    CHECKBOX("checkbox"),
    BOOLEAN("boolean"),
    TIME_PICKER("time-picker"),
    COMPANY_STAFF_INPUT("company-staff-input"),
    TIPS("tips");

    companion object {
        fun findType(value: String?): VariablePropType? {
            if (value.isNullOrBlank()) {
                return null
            }

            values().forEach {
                if (it.value == value) {
                    return it
                }
            }

            return null
        }
    }
}
