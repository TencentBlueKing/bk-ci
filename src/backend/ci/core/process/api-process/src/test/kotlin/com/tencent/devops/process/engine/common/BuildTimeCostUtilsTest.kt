package com.tencent.devops.process.engine.common

import com.tencent.devops.common.pipeline.pojo.time.BuildRecordTimeLine
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BuildTimeCostUtilsTest {

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
            listOf(BuildRecordTimeLine.Moment(0L, 100L), BuildRecordTimeLine.Moment(500L, 600L)),
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
}
