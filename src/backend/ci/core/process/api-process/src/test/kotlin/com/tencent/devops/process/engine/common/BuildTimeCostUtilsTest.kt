package com.tencent.devops.process.engine.common

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeLine
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.process.engine.common.BuildTimeCostUtils.generateTaskTimeCost
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class BuildTimeCostUtilsTest {

    /**
     * 人工审核期间 Job 被这个插件占着，整段算用户耗时。
     * 审核通过、驳回会在插件结束前给等待时间戳收尾，超时、Job超时、构建取消不会收尾，
     * 三种结束方式的用户耗时必须一致，都等于插件的起止区间
     */
    @Test
    fun reviewWaitAlwaysCountsAsExecuteCost() {
        val start = LocalDateTime.now().minusSeconds(61)
        val end = start.plusSeconds(61)
        val reviewStart = start.plusSeconds(1).timestampmilli()
        val cases = mapOf(
            "审核通过或驳回" to BuildRecordTimeStamp(reviewStart, end.timestampmilli()),
            "审核超时、Job超时、构建取消" to BuildRecordTimeStamp(reviewStart, null),
            "没有等待时间戳" to BuildRecordTimeStamp(null, null)
        )
        cases.forEach { (desc, reviewWait) ->
            val cost = genReviewTask(start = start, end = end, reviewWait = reviewWait)
                .generateTaskTimeCost()
            Assertions.assertEquals(61000L, cost?.totalCost, desc)
            Assertions.assertEquals(61000L, cost?.executeCost, desc)
            Assertions.assertEquals(0L, cost?.waitCost, desc)
        }
    }

    /**
     * 插件还在跑时按当前时刻算，用户耗时同样是整段区间
     */
    @Test
    fun runningReviewTaskTimeCost() {
        val start = LocalDateTime.now().minusSeconds(10)
        val cost = genReviewTask(
            start = start,
            end = null,
            reviewWait = BuildRecordTimeStamp(start.timestampmilli(), null)
        ).generateTaskTimeCost()
        Assertions.assertEquals(0L, cost?.waitCost)
        Assertions.assertEquals(cost?.totalCost, cost?.executeCost)
        Assertions.assertTrue((cost?.executeCost ?: 0) >= 10000L)
    }

    /**
     * 算 Job 耗时时，插件的整段区间（含审核）都进执行时间线，不再往等待时间线里塞
     */
    @Test
    fun reviewWaitGoesToExecuteTimeLine() {
        val start = LocalDateTime.now().minusSeconds(61)
        val end = start.plusSeconds(61)
        val timeLine = BuildRecordTimeLine()
        genReviewTask(
            start = start,
            end = end,
            reviewWait = BuildRecordTimeStamp(start.plusSeconds(1).timestampmilli(), null)
        ).generateTaskTimeCost(timeLine)
        Assertions.assertEquals(
            listOf(BuildRecordTimeLine.Moment(start.timestampmilli(), end.timestampmilli())),
            timeLine.executeCostMoments
        )
        Assertions.assertEquals(emptyList<BuildRecordTimeLine.Moment>(), timeLine.waitCostMoments)
    }

    private fun genReviewTask(
        start: LocalDateTime,
        end: LocalDateTime?,
        reviewWait: BuildRecordTimeStamp
    ) = BuildRecordTask(
        buildId = "b-1", projectId = "p-1", pipelineId = "pl-1", resourceVersion = 1,
        stageId = "stage-1", containerId = "1", taskId = "t-1", taskSeq = 1, executeCount = 1,
        taskVar = mutableMapOf(), classType = "manualReviewUserTask", atomCode = "manualReviewUserTask",
        startTime = start, endTime = end,
        timestamps = mapOf(BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to reviewWait)
    )

    @Test
    fun differenceTimeLine() {
        val left = listOf(BuildRecordTimeLine.Moment(1L, 10L))
        val right = listOf(BuildRecordTimeLine.Moment(2L, 4L), BuildRecordTimeLine.Moment(6L, 9L))
        val ans = BuildTimeCostUtils.differenceTimeLine(left, right)
        Assertions.assertEquals(
            listOf(
                BuildRecordTimeLine.Moment(1L, 2L),
                BuildRecordTimeLine.Moment(4L, 6L),
                BuildRecordTimeLine.Moment(9L, 10L)
            ),
            ans
        )
    }

    @Test
    fun differenceTimeLineMultiLeft() {
        val left = listOf(BuildRecordTimeLine.Moment(0L, 100L), BuildRecordTimeLine.Moment(500L, 600L))
        val right = listOf(BuildRecordTimeLine.Moment(200L, 400L))
        val ans = BuildTimeCostUtils.differenceTimeLine(left, right)
        Assertions.assertEquals(
            left,
            ans
        )
    }

    @Test
    fun differenceTimeLineEmptyRight() {
        val left = listOf(BuildRecordTimeLine.Moment(0L, 50L), BuildRecordTimeLine.Moment(70L, 100L))
        val ans = BuildTimeCostUtils.differenceTimeLine(left, emptyList())
        Assertions.assertEquals(left, ans)
    }

    @Test
    fun differenceTimeLineFullOverlap() {
        val left = listOf(BuildRecordTimeLine.Moment(0L, 10L))
        val right = listOf(BuildRecordTimeLine.Moment(0L, 10L))
        val ans = BuildTimeCostUtils.differenceTimeLine(left, right)
        Assertions.assertEquals(emptyList<BuildRecordTimeLine.Moment>(), ans)
    }

    @Test
    fun differenceTimeLineMultiLeftPartialOverlap() {
        val left = listOf(BuildRecordTimeLine.Moment(0L, 50L), BuildRecordTimeLine.Moment(80L, 120L))
        val right = listOf(BuildRecordTimeLine.Moment(30L, 90L))
        val ans = BuildTimeCostUtils.differenceTimeLine(left, right)
        Assertions.assertEquals(
            listOf(BuildRecordTimeLine.Moment(0L, 30L), BuildRecordTimeLine.Moment(90L, 120L)),
            ans
        )
    }

    @Test
    fun intervalIntersection() {
        val left = listOf(BuildRecordTimeLine.Moment(1L, 3L), BuildRecordTimeLine.Moment(9L, 10L))
        val right = listOf(BuildRecordTimeLine.Moment(2L, 4L), BuildRecordTimeLine.Moment(6L, 9L))
        val ans = BuildTimeCostUtils.intersectionTimeLine(left, right)
        Assertions.assertEquals(ans, listOf(BuildRecordTimeLine.Moment(2L, 3L), BuildRecordTimeLine.Moment(9L, 9L)))
    }

    @Test
    fun mergeTimeLine() {
        val left = listOf(BuildRecordTimeLine.Moment(1L, 3L), BuildRecordTimeLine.Moment(9L, 10L))
        val right = listOf(BuildRecordTimeLine.Moment(2L, 4L), BuildRecordTimeLine.Moment(6L, 9L))
        val ans = BuildTimeCostUtils.mergeTimeLine(left, right)
        Assertions.assertEquals(ans, listOf(BuildRecordTimeLine.Moment(1L, 4L), BuildRecordTimeLine.Moment(6L, 10L)))
    }

    @Test
    fun mergeTimeLine2() {
        val left = emptyList<BuildRecordTimeLine.Moment>()
        val right = listOf(BuildRecordTimeLine.Moment(2L, 4L), BuildRecordTimeLine.Moment(6L, 9L))
        val ans = BuildTimeCostUtils.mergeTimeLine(left, right)
        Assertions.assertEquals(ans, listOf(BuildRecordTimeLine.Moment(2L, 4L), BuildRecordTimeLine.Moment(6L, 9L)))
    }

    @Test
    fun partitionOverlappingExecuteUsesUnion() {
        val cost = BuildTimeCostUtils.partitionTimeCost(
            totalCost = 100,
            executeMoments = listOf(
                BuildRecordTimeLine.Moment(0, 80),
                BuildRecordTimeLine.Moment(20, 60)
            ),
            waitMoments = emptyList(),
            queueMoments = emptyList(),
            queueInWait = false
        )
        Assertions.assertEquals(80, cost.executeCost)
        Assertions.assertEquals(20, cost.systemCost)
        Assertions.assertEquals(100, cost.executeCost + cost.systemCost + cost.waitCost + cost.queueCost)
    }

    @Test
    fun partitionSerialExecuteSums() {
        val cost = BuildTimeCostUtils.partitionTimeCost(
            totalCost = 30,
            executeMoments = listOf(
                BuildRecordTimeLine.Moment(0, 10),
                BuildRecordTimeLine.Moment(10, 30)
            ),
            waitMoments = emptyList(),
            queueMoments = emptyList(),
            queueInWait = false
        )
        Assertions.assertEquals(30, cost.executeCost)
        Assertions.assertEquals(0, cost.systemCost)
    }

    @Test
    fun partitionQueueNotCoveredByExecuteGoesToWaitWhenQueuedInWait() {
        val cost = BuildTimeCostUtils.partitionTimeCost(
            totalCost = 50,
            executeMoments = listOf(BuildRecordTimeLine.Moment(20, 50)),
            waitMoments = emptyList(),
            queueMoments = listOf(BuildRecordTimeLine.Moment(0, 20)),
            queueInWait = true
        )
        Assertions.assertEquals(30, cost.executeCost)
        Assertions.assertEquals(20, cost.waitCost)
        Assertions.assertEquals(20, cost.queueCost)
        Assertions.assertEquals(0, cost.systemCost)
    }

    @Test
    fun partitionQueueOverlappingExecuteIsNotDoubleCounted() {
        val cost = BuildTimeCostUtils.partitionTimeCost(
            totalCost = 40,
            executeMoments = listOf(BuildRecordTimeLine.Moment(0, 40)),
            waitMoments = emptyList(),
            queueMoments = listOf(BuildRecordTimeLine.Moment(0, 15)),
            queueInWait = true
        )
        Assertions.assertEquals(40, cost.executeCost)
        Assertions.assertEquals(0, cost.waitCost)
        Assertions.assertEquals(0, cost.systemCost)
    }

    @Test
    fun criticalPathParallelIsMax() {
        val cost = BuildTimeCostUtils.criticalPathCost(
            jobCosts = mapOf("a" to 10L, "b" to 30L),
            dependencies = emptyMap()
        )
        Assertions.assertEquals(30L, cost)
    }

    @Test
    fun criticalPathSerialChainSums() {
        val cost = BuildTimeCostUtils.criticalPathCost(
            jobCosts = mapOf("a" to 10L, "b" to 15L),
            dependencies = mapOf("b" to setOf("a"))
        )
        Assertions.assertEquals(25L, cost)
    }

    @Test
    fun criticalPathDiamondUsesLongestBranch() {
        val cost = BuildTimeCostUtils.criticalPathCost(
            jobCosts = mapOf("a" to 10L, "b" to 4L, "c" to 6L),
            dependencies = mapOf("c" to setOf("a", "b"))
        )
        Assertions.assertEquals(16L, cost)
    }

    @Test
    fun criticalPathCycleDoesNotLoop() {
        val cost = BuildTimeCostUtils.criticalPathCost(
            jobCosts = mapOf("a" to 10L, "b" to 5L),
            dependencies = mapOf("a" to setOf("b"), "b" to setOf("a"))
        )
        Assertions.assertEquals(15L, cost)
    }
}
