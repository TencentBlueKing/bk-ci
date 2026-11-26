package com.tencent.devops.worker.common.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MergedOutputTailTest {

    @Test
    fun keepsOnlyTheLatestChars() {
        val tail = MergedOutputTail(8)
        tail.append("abcdefghij")
        Assertions.assertEquals("cdefghij", tail.content())
        tail.append("xyz")
        Assertions.assertEquals("fghijxyz", tail.content())
    }

    @Test
    fun singleLineLongerThanCapKeepsItsOwnTail() {
        val tail = MergedOutputTail(4)
        tail.append("hello")
        tail.append("world!!")
        Assertions.assertEquals("ld!!", tail.content())
    }

    @Test
    fun newlineCountsTowardTheCap() {
        val tail = MergedOutputTail(6)
        tail.append("12345")
        tail.append("\n")
        tail.append("ab")
        tail.append("\n")
        Assertions.assertEquals("45\nab\n", tail.content())
    }

    @Test
    fun nonPositiveCapStaysEmpty() {
        val tail = MergedOutputTail(0)
        tail.append("abc")
        Assertions.assertEquals("", tail.content())
    }
}
