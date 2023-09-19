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

package com.tencent.devops.process.pojo.transfer

import com.tencent.devops.common.pipeline.enums.VMBaseOS
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("yaml定位")
data class PositionResponse(
    @ApiModelProperty("定位类型，非error时应当必有")
    val type: PositionType? = null,
    @ApiModelProperty("当定位到JOB,STEP时有效，表示当前stage的os类型")
    var jobBaseOs: VMBaseOS? = null,
    @ApiModelProperty("当定位到STAGE,JOB,STEP时有效，表示stage下标")
    var stageIndex: Int? = null,
    @ApiModelProperty("当定位到JOB,STEP时有效，表示stage下标")
    var jobId: String? = null,
    @ApiModelProperty("当定位到STEP时有效，表示stage下标")
    var stepIndex: Int? = null,
    @ApiModelProperty("转换错误")
    val error: String? = null
) {
    enum class PositionType {
        SETTING,
        STAGE,
        JOB,
        STEP
    }
}
