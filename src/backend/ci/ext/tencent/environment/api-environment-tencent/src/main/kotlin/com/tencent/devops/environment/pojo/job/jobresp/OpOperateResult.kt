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

package com.tencent.devops.environment.pojo.job.jobresp

import io.swagger.v3.oas.annotations.media.Schema

data class OpOperateResult(
    @get:Schema(title = "状态码：0 - 成功，其他 - 失败", required = true)
    val code: Int,
    @get:Schema(title = "结果布尔值：true - 操作执行成功，false - 操作执行失败", required = true)
    val result: Boolean = false,
    @get:Schema(title = "结果提示消息", required = true)
    val msg: String,
    @get:Schema(title = "灰度项目的总数量", required = true)
    val grayProjNumber: Long,
    @get:Schema(title = "灰度项目englishName集合")
    val grayProjList: Set<String>?,
    @get:Schema(title = "项目灰度状态集合")
    val projGrayStatus: List<ProjectOpInfo>?
) {
    constructor(code: Int, result: Boolean, msg: String, grayProjNumber: Long) : this(
        code = code, result = result, msg = msg, grayProjNumber = grayProjNumber,
        grayProjList = null, projGrayStatus = null
    )

    constructor(code: Int, result: Boolean, msg: String, grayProjNumber: Long, grayProjList: Set<String>?) : this(
        code = code, result = result, msg = msg, grayProjNumber = grayProjNumber,
        grayProjList = grayProjList, projGrayStatus = null
    )

    constructor(
        code: Int,
        result: Boolean,
        msg: String,
        grayProjNumber: Long,
        projGrayStatus: List<ProjectOpInfo>?
    ) : this(
        code = code, result = result, msg = msg, grayProjNumber = grayProjNumber,
        grayProjList = null, projGrayStatus = projGrayStatus
    )
}