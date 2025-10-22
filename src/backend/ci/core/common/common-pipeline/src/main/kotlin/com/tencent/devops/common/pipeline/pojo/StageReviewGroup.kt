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

import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "Stage审核组信息")
data class StageReviewGroup(
    @get:Schema(title = "审核组ID(后台生成)", required = false)
    var id: String? = null,
    @get:Schema(title = "审核组名称", required = true)
    val name: String = "Flow 1",
    @get:Schema(title = "审核人员", required = true)
    var reviewers: List<String> = listOf(),
    @get:Schema(title = "审核用户组", required = false)
    var groups: List<String> = listOf(),
    @get:Schema(title = "审核结果（枚举）", required = false)
    var status: String? = null,
    @get:Schema(title = "审核操作人", required = false)
    var operator: String? = null,
    @get:Schema(title = "审核操作时间", required = false)
    var reviewTime: Long? = null,
    @get:Schema(title = "审核建议", required = false)
    var suggest: String? = null,
    @get:Schema(title = "审核传入变量", required = false)
    var params: List<ManualReviewParam>? = null
)
