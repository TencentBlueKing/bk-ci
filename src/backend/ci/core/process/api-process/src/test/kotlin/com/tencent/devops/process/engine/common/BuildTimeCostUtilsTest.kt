package com.tencent.devops.process.engine.common

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class BuildTimeCostUtilsTest {

    @Test
    fun differenceTimeLine() {
        val left = listOf(Pair(1L, 10L))
        val right = listOf(Pair(2L, 4L), Pair(6L, 9L))
        val ans = BuildTimeCostUtils.differenceTimeLine(left, right)
        Assertions.assertEquals(ans, listOf(Pair(1L, 2L), Pair(4L, 6L), Pair(9L, 10L)))
    }

    @Test
    fun intervalIntersection() {
        val left = listOf(Pair(1L, 3L), Pair(9L, 10L))
        val right = listOf(Pair(2L, 4L), Pair(6L, 9L))
        val ans = BuildTimeCostUtils.intersectionTimeLine(left, right)
        Assertions.assertEquals(ans, listOf(Pair(2L, 3L), Pair(9L, 9L)))
    }

    @Test
    fun mergeTimeLine() {
        val left = listOf(Pair(1L, 3L), Pair(9L, 10L))
        val right = listOf(Pair(2L, 4L), Pair(6L, 9L))
        val ans = BuildTimeCostUtils.mergeTimeLine(left, right)
        Assertions.assertEquals(ans, listOf(Pair(1L, 4L), Pair(6L, 10L)))
    }

    @Test
    fun mergeTimeLine2() {
        val left = emptyList<Pair<Long, Long>>()
        val right = listOf(Pair(2L, 4L), Pair(6L, 9L))
        val ans = BuildTimeCostUtils.mergeTimeLine(left, right)
        Assertions.assertEquals(ans, listOf(Pair(2L, 4L), Pair(6L, 9L)))
    }
}
