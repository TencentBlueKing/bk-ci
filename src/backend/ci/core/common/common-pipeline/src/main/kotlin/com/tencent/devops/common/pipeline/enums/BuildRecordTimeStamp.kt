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

package com.tencent.devops.common.pipeline.enums

import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeLine
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "构建详情记录-事件时间戳")
data class BuildRecordTimeStamp(
    @get:Schema(title = "开始时间", required = true)
    val startTime: Long?,
    @get:Schema(title = "结束时间", required = true)
    val endTime: Long?
) {
    // 开始时刻缺失时无法计算区间，返回 0，不能拿结束时刻的时间戳当时长
    fun between() = if (startTime == null) 0 else (endTime ?: startTime) - startTime

    /**
     * 补齐没有收尾的时间戳：等待/排队期间构建直接结束（审核超时、Job 超时、取消）时，
     * 只有开始时刻没有结束时刻。按所属层级的结束时刻 [defaultEndTime] 补齐，
     * 否则 [between] 会把这段等待算成 0，等待时长会落进用户耗时或系统耗时。
     *
     * 只补开始时刻已有、结束时刻为空的情况，其余原样返回。
     */
    fun fixEndTime(defaultEndTime: Long): BuildRecordTimeStamp {
        if (startTime == null || endTime != null) {
            return this
        }
        return copy(endTime = defaultEndTime.coerceAtLeast(startTime))
    }

    fun insert2TimeLine(timeline: MutableList<BuildRecordTimeLine.Moment>) {
        if (startTime != null && endTime != null) {
            timeline.add(BuildRecordTimeLine.Moment(startTime, endTime))
        }
    }
}
