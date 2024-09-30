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

package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建模型-构建参数")
data class BuildParameters(
    @get:Schema(title = "元素值ID-标识符", required = true)
    var key: String,
    @get:Schema(title = "元素值名称-显示用", required = true)
    var value: Any,
    @get:Schema(title = "元素值类型", required = false)
    val valueType: BuildFormPropertyType? = null,
    @get:Schema(title = "是否只读", required = false)
    val readOnly: Boolean? = false,
    @get:Schema(title = "描述", required = false)
    var desc: String? = null,
    @get:Schema(title = "默认值", required = false)
    var defaultValue: Any? = null,
    @get:Schema(title = "元素对应代码库", required = false)
    var repoHashId: String? = null,
    @get:Schema(title = "实际参数名（REPO_REF 类型变量会拆分成两个变量）", required = false)
    var relKey: String? = null
) {
    companion object {
        private const val REPO_REF_SUFFIX_BRANCH = ".branch"
        private const val REPO_REF_SUFFIX_REPO_NAME = ".repo-name"

        fun getRepoRefVariableName(key: String) = Pair("$key$REPO_REF_SUFFIX_REPO_NAME", "$key$REPO_REF_SUFFIX_BRANCH")
    }
}
