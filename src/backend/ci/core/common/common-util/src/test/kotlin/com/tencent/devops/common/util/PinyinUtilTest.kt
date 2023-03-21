package com.tencent.devops.common.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.text.Collator
import java.util.Locale

class PinyinUtilTest {

    @Test
    fun test() {
        val list = listOf("a", "啊", "刺", "c", "博", "b")
        val res = list.sortedWith(
            Comparator { a, b ->
                Collator.getInstance(Locale.CHINESE).compare(a, b)
            }
        )
        Assertions.assertEquals(res.joinToString(separator = ""), "abc啊博刺")
    }
}
