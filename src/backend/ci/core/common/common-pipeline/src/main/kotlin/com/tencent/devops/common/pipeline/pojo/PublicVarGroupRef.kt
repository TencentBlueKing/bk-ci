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

package com.tencent.devops.common.pipeline.pojo

import io.swagger.v3.oas.annotations.media.Schema

data class PublicVarGroupRef(
    @get:Schema(title = "变量组名称", required = true)
    val groupName: String,
    @get:Schema(title = "版本号", required = false)
    val version: Int? = null,
    @get:Schema(title = "版本名称", required = false)
    val versionName: String? = null,
    @get:Schema(title = "回显的历史变量列表", required = false)
    var variables: List<BuildFormProperty>? = null
) {
    companion object {
        /**
         * 版本名称到版本号的映射规则。
         *
         * 同时覆盖 `v3` 与裸数字 `3` 两种写法：YAML 里 `version: 1` 会被解析成数字，
         * 只认 `vN` 会让它落到 null，被当成动态版本（始终跟随最新），导致固定版本引用被静默改成动态引用。
         * 其余写法（如 latest、空值）按约定表示动态版本。
         */
        private val VERSION_NAME_REGEX = Regex("^v?(\\d+)$", RegexOption.IGNORE_CASE)

        fun create(
            groupName: String,
            version: Int? = null,
            versionName: String? = null,
            variables: List<BuildFormProperty>? = null
        ): PublicVarGroupRef {
            val resolvedVersion = version ?: versionName?.let {
                VERSION_NAME_REGEX.matchEntire(it.trim())?.groupValues?.get(1)?.toIntOrNull()
            }
            return PublicVarGroupRef(
                groupName = groupName,
                version = resolvedVersion,
                versionName = versionName,
                variables = variables
            )
        }
    }
}
