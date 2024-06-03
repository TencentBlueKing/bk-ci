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

package com.tencent.devops.common.web.form.models.ui.components

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * 表单项中的组件项接口类，不同组件自己实现
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "name",
    defaultImpl = UiComponentCommon::class,
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CheckboxComponent::class, name = CheckboxComponent.classType),
    JsonSubTypes.Type(value = InputComponent::class, name = InputComponent.classType),
    JsonSubTypes.Type(value = RadioComponent::class, name = RadioComponent.classType),
    JsonSubTypes.Type(value = SelectComponent::class, name = SelectComponent.classType),
    JsonSubTypes.Type(value = TimeComponent::class, name = TimeComponent.classType),
    JsonSubTypes.Type(value = TipComponent::class, name = TipComponent.classType)
)
open class UiComponent(
    // 必需，所需渲染组件的名称，例如组件库里的bk-input、bk-table，也可以是通过插件注册的自定义组件
    open val name: String,
    // 可选，透传到组件的属性，可传入的属性由渲染的组件决定，组件支持的属性都可传入
    open val props: Map<String, Any>?
)

data class UiComponentCommon(
    override val name: String,
    override val props: Map<String, Any>?
) : UiComponent(name, props)
