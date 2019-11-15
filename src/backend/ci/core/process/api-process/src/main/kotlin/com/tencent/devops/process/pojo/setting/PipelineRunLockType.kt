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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.pojo.setting

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("流水线运行锁定方式")
enum class PipelineRunLockType {
    @ApiModelProperty("可同时运行多个构建任务（默认）")
    MULTIPLE,
    @ApiModelProperty("同一时间最多只能运行一个构建任务")
    SINGLE,
    @ApiModelProperty("最多只能运行一个构建任务，且失败时锁定")
    SINGLE_LOCK,
    @ApiModelProperty("锁定流水线，任何触发方式都无法运行")
    LOCK;

    companion object {
        fun toValue(type: PipelineRunLockType?): Int {
            return when (type) {
                null -> 1
                MULTIPLE -> 1
                SINGLE -> 2
                SINGLE_LOCK -> 3
                LOCK -> 4
            }
        }

        fun valueOf(value: Int): PipelineRunLockType {
            return when (value) {
                1 -> MULTIPLE
                2 -> SINGLE
                3 -> SINGLE_LOCK
                4 -> LOCK
                else -> MULTIPLE
            }
        }
    }
}
