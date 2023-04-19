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

package com.tencent.devops.process.engine.common

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeCost
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeLine
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordModel
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

object BuildTimeCostUtils {
    private val logger = LoggerFactory.getLogger(BuildTimeCostUtils::class.java)

    fun BuildRecordModel.generateBuildTimeCost(stageRecords: List<BuildRecordStage>): BuildRecordTimeCost {
        val startTime = startTime ?: return BuildRecordTimeCost()
        val endTime = endTime ?: LocalDateTime.now()
        val totalCost = Duration.between(startTime, endTime).toMillis()
        var executeCost = 0L
        var waitCost = 0L
        var queueCost = 0L
        stageRecords.forEach { record ->
            val stageCost = JsonUtil.anyTo(
                record.stageVar[Stage::timeCost.name] ?: return@forEach,
                object : TypeReference<BuildRecordTimeCost>() {}
            )
            executeCost += stageCost.executeCost
            waitCost += stageCost.waitCost
            queueCost += stageCost.queueCost
        }
        val systemCost = totalCost - executeCost - queueCost - waitCost
        return BuildRecordTimeCost(
            totalCost = totalCost,
            executeCost = executeCost,
            waitCost = waitCost,
            queueCost = queueCost,
            systemCost = systemCost.notNegative()
        )
    }

    fun BuildRecordStage.generateStageTimeCost(
        containerRecords: List<BuildRecordContainer>
    ): BuildRecordTimeCost? {
        val startTime = startTime ?: return null
        val endTime = endTime ?: LocalDateTime.now()
        val totalCost = Duration.between(startTime, endTime).toMillis()
        var containerExecuteCost = emptyList<BuildRecordTimeLine.Moment>()
        var containerWaitCost = listOf(
            BuildRecordTimeLine.Moment(startTime.timestampmilli(), endTime.timestampmilli())
        )
        var containerQueueCost = listOf(
            BuildRecordTimeLine.Moment(startTime.timestampmilli(), endTime.timestampmilli())
        )
        containerRecords.forEach { record ->
            val containerTimeLine = JsonUtil.anyTo(
                record.containerVar[BuildRecordTimeLine::class.java.simpleName] ?: return@forEach,
                object : TypeReference<BuildRecordTimeLine>() {}
            )
            // 计算等到耗时需要将率先执行完毕的container追加无状态区间
            record.endTime?.let {
                val fixedMoment = BuildRecordTimeLine.Moment(it.timestampmilli(), endTime.timestampmilli())
                containerTimeLine.waitCostMoments.add(fixedMoment)
                containerTimeLine.queueCostMoments.add(fixedMoment)
            }
            // 执行时间取并集
            containerExecuteCost = mergeTimeLine(containerExecuteCost, containerTimeLine.executeCostMoments)
            val mergedWaitCost = mergeTimeLine(containerTimeLine.waitCostMoments, containerTimeLine.queueCostMoments)
            // 等待时间取交集
            containerWaitCost = intersectionTimeLine(containerWaitCost, mergedWaitCost)
            // 排队时间取交集
            containerQueueCost = intersectionTimeLine(containerQueueCost, containerTimeLine.queueCostMoments)
        }
        val executeCost = containerExecuteCost.sumOf { it.endTime - it.startTime }
        val queueCost = containerQueueCost.sumOf { it.endTime - it.startTime }
        val waitCost = timestamps.toList().sumOf { (type, time) ->
            if (!type.stageCheckWait()) return@sumOf 0L
            logWhenNull(
                time, "$buildId|STAGE|$stageId|${type.name}"
            )
            return@sumOf time.between()
        } + containerWaitCost.sumOf { it.endTime - it.startTime }
        val systemCost = totalCost - executeCost - waitCost
        return BuildRecordTimeCost(
            totalCost = totalCost,
            executeCost = executeCost,
            waitCost = waitCost,
            queueCost = queueCost,
            systemCost = systemCost.notNegative()
        )
    }

    fun BuildRecordContainer.generateMatrixTimeCost(
        containerRecords: List<BuildRecordContainer>
    ): BuildRecordTimeCost? {
        val startTime = startTime ?: return null
        val endTime = endTime ?: LocalDateTime.now()
        val totalCost = Duration.between(startTime, endTime).toMillis()
        var containerExecuteCost = emptyList<BuildRecordTimeLine.Moment>()
        var containerWaitCost = listOf(
            BuildRecordTimeLine.Moment(startTime.timestampmilli(), endTime.timestampmilli())
        )
        var containerQueueCost = listOf(
            BuildRecordTimeLine.Moment(startTime.timestampmilli(), endTime.timestampmilli())
        )
        containerRecords.forEach { record ->
            val containerTimeLine = JsonUtil.anyTo(
                record.containerVar[BuildRecordTimeLine::class.java.simpleName] ?: return@forEach,
                object : TypeReference<BuildRecordTimeLine>() {}
            )
            // 执行时间取并集
            containerExecuteCost = mergeTimeLine(containerExecuteCost, containerTimeLine.executeCostMoments)
            // 等待时间取交集
            containerWaitCost = intersectionTimeLine(containerWaitCost, containerTimeLine.waitCostMoments)
            // 排队时间取交集
            containerQueueCost = intersectionTimeLine(containerQueueCost, containerTimeLine.queueCostMoments)
        }
        val executeCost = containerExecuteCost.sumOf { it.endTime - it.startTime }
        val queueCost = containerQueueCost.sumOf { it.endTime - it.startTime }
        val waitCost = containerWaitCost.sumOf { it.endTime - it.startTime }
        val systemCost = totalCost - executeCost - queueCost - waitCost
        return BuildRecordTimeCost(
            totalCost = totalCost,
            executeCost = executeCost,
            waitCost = waitCost,
            queueCost = queueCost,
            systemCost = systemCost.notNegative()
        )
    }

    /**
     * 计算Container级别的所有时间消耗
     * queueCost、 systemCost 保持为 0
     * @return Pair(该Container耗时概览, 该Container耗时细则)
     */
    fun BuildRecordContainer.generateContainerTimeCost(
        taskRecords: List<BuildRecordTask>
    ): Pair<BuildRecordTimeCost?, BuildRecordTimeLine> {
        val containerTimeLine = BuildRecordTimeLine()
        val startTime = startTime ?: return Pair(null, containerTimeLine)
        val endTime = endTime ?: LocalDateTime.now()
        val totalCost = Duration.between(startTime, endTime).toMillis()
        var executeCost = 0L
        var waitCost = 0L
        val queueCost = timestamps.toList().sumOf { (type, time) ->
            if (!type.containerCheckQueue()) return@sumOf 0L
            logWhenNull(
                time, "$buildId|CONTAINER|$containerId|${type.name}"
            )
            time.insert2TimeLine(containerTimeLine.queueCostMoments)
            return@sumOf time.between()
        }
        taskRecords.forEach { record ->
            val taskTimeLine = BuildRecordTimeLine()
            val cost = record.generateTaskTimeCost(taskTimeLine)
            containerTimeLine.queueCostMoments.addAll(taskTimeLine.queueCostMoments)
            containerTimeLine.waitCostMoments.addAll(taskTimeLine.waitCostMoments)
            containerTimeLine.executeCostMoments.addAll(taskTimeLine.executeCostMoments)
            cost?.executeCost?.let { executeCost += it }
            cost?.waitCost?.let { waitCost += it }
        }
        val systemCost = totalCost - executeCost - waitCost - queueCost
        return Pair(
            BuildRecordTimeCost(
                totalCost = totalCost,
                executeCost = executeCost,
                waitCost = waitCost,
                queueCost = queueCost,
                systemCost = systemCost.notNegative()
            ),
            containerTimeLine
        )
    }

    /**
     * 计算Task级别的所有时间消耗
     * queueCost、 systemCost 保持为 0
     * @param timeLine 计算task时为null, 计算container时会传入以记录具体时刻
     */
    fun BuildRecordTask.generateTaskTimeCost(timeLine: BuildRecordTimeLine? = null): BuildRecordTimeCost? {
        val startTime = startTime ?: return null
        val endTime = endTime ?: LocalDateTime.now()
        val totalCost = Duration.between(startTime, endTime).toMillis()
        val waitCost = timestamps.toList().sumOf { (type, time) ->
            if (!type.taskCheckWait()) return@sumOf 0L
            logWhenNull(
                time, "$buildId|TASK|$taskId|${type.name}"
            )
            timeLine?.let {
                time.insert2TimeLine(timeLine.waitCostMoments)
            }
            return@sumOf time.between()
        }
        val executeCost = totalCost - waitCost
        timeLine?.let {
            timeLine.executeCostMoments.addAll(
                differenceTimeLine(
                    listOf(
                        BuildRecordTimeLine.Moment(
                            startTime.timestampmilli(),
                            endTime.timestampmilli()
                        )
                    ),
                    timeLine.waitCostMoments
                )
            )
        }

        return BuildRecordTimeCost(
            totalCost = totalCost,
            waitCost = waitCost,
            executeCost = executeCost.notNegative()
        )
    }

    /**
     * 区间求差集 left - right
     */
    fun differenceTimeLine(
        left: List<BuildRecordTimeLine.Moment>,
        right: List<BuildRecordTimeLine.Moment>
    ): List<BuildRecordTimeLine.Moment> {
        val line: MutableList<Pair<Long, Char>> = mutableListOf()
        val ans: MutableList<BuildRecordTimeLine.Moment> = mutableListOf()
        left.forEach {
            line.add(Pair(it.startTime, 'L'))
            line.add(Pair(it.endTime, 'L'))
        }
        right.forEach {
            line.add(Pair(it.startTime, 'R'))
            line.add(Pair(it.endTime, 'R'))
        }
        line.sortBy { it.first }
        var cnt = true
        var index = 0
        while (index < line.size) {
            if (line[index].second == 'R') cnt = !cnt
            if (cnt && index < line.size - 1) ans.add(
                BuildRecordTimeLine.Moment(
                    line[index].first,
                    line[index + 1].first
                )
            )
            index++
        }
        return ans
    }

    /**
     * 区间求交集 left ∩ right
     */
    fun intersectionTimeLine(
        left: List<BuildRecordTimeLine.Moment>,
        right: List<BuildRecordTimeLine.Moment>
    ): List<BuildRecordTimeLine.Moment> {
        val ans: MutableList<BuildRecordTimeLine.Moment> = mutableListOf()
        var i = 0
        var j = 0
        while (i < left.size && j < right.size) {
            // 我们来检查A[i]是否与B[j]相交。
            // lo -- 相交的起始点
            // hi -- 交点的端点
            val lo = left[i].startTime.coerceAtLeast(right[j].startTime)
            val hi = left[i].endTime.coerceAtMost(right[j].endTime)
            if (lo <= hi) ans.add(BuildRecordTimeLine.Moment(lo, hi))
            // 移除具有最小端点的区间
            if (left[i].endTime < right[j].endTime) i++ else j++
        }
        return ans
    }

    /**
     * 区间求并集 left ∪ right
     */
    fun mergeTimeLine(
        left: List<BuildRecordTimeLine.Moment>,
        right: List<BuildRecordTimeLine.Moment>
    ): List<BuildRecordTimeLine.Moment> {
        val intervals = left.plus(right).sortedBy { it.startTime }
        val res = mutableListOf<BuildRecordTimeLine.Moment>()
        for (interval in intervals) {
            // 如果列表为空,或者当前区间与上一区间不重合,直接添加
            if (res.size == 0 || res[res.size - 1].endTime < interval.startTime) {
                res.add(interval)
            } else {
                // 否则的话,我们就可以与上一区间进行合并
                val m = res.removeLast()
                res.add(BuildRecordTimeLine.Moment(m.startTime, m.endTime.coerceAtLeast(interval.endTime)))
            }
        }
        return res
    }

    private fun logWhenNull(time: BuildRecordTimeStamp, logInfo: String) {
        if (time.startTime == null) {
            logger.warn("$logInfo|warning! start time is null.")
        }
        if (time.endTime == null) {
            logger.warn("$logInfo|warning! end time is null.")
        }
    }

    private fun Long.notNegative() = if (this < 0) 0 else this
}
