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

package com.tencent.devops.common.pipeline.pojo.transfer

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "yaml定位")
data class PositionResponse(
    @get:Schema(title = "定位类型，非error时应当必有")
    val type: PositionType? = null,
    @get:Schema(title = "当定位到JOB,STEP时有效，表示当前stage的os类型")
    var jobBaseOs: TransferVMBaseOS? = null,
    @get:Schema(title = "当定位到STAGE,JOB,STEP时有效，表示stage下标, -1 表示finally stage")
    var stageIndex: Int? = null,
    @get:Schema(title = "当定位到JOB,STEP时有效，表示container下标")
    var containerIndex: Int? = null,
    @get:Schema(title = "当定位到JOB,STEP时有效，表示job的id")
    var jobId: String? = null,
    @get:Schema(title = "当定位到STEP时有效，表示step下标")
    var stepIndex: Int? = null,
    @get:Schema(title = "当定位到STEP时有效，拿到对应的element元素")
    var element: Element? = null,
    @get:Schema(title = "转换错误")
    val error: String? = null
) {
    enum class PositionType {
        SETTING,
        STAGE,
        JOB,
        STEP
    }
}
