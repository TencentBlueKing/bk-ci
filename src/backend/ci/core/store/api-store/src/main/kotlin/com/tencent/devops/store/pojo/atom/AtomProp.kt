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

package com.tencent.devops.store.pojo.atom

import io.swagger.v3.oas.annotations.media.Schema

data class AtomProp(
    @get:Schema(title = "插件代码", required = true)
    val atomCode: String,
    @get:Schema(title = "最新版本在AGENT编译环境下支持的操作系统", required = true)
    val os: List<String>,
    @get:Schema(title = "插件logo地址", required = false)
    val logoUrl: String?,
    @get:Schema(title = "无构建环境插件是否可以在有构建环境运行标识", required = false)
    var buildLessRunFlag: Boolean?,
    /**
     * key 为编排中该插件声明的版本号(可能是 1.* 这类浮动版本)，value 为该版本适用的操作系统，
     * value 为空表示插件未声明适用范围，即不做限制。
     *
     * 与 [os] 的区别：[os] 只取插件最新版本在 AGENT(普通流水线编译环境)下的声明，而插件的适用操作系统
     * 按 jobType 分别声明、不同版本的声明也可能不同。该字段按「流水线所属渠道对应的 jobType」+
     * 「编排中该插件的版本」逐个解析，与保存时的校验口径一致，因而是前端做适配提示时应当依据的字段。
     */
    @get:Schema(title = "编排中各版本适用的运行环境操作系统", required = false)
    val versionOsMap: Map<String, List<String>>? = null
)
