package com.tencent.devops.common.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.text.Collator

class PinyinUtilTest {

    @Test
    fun test() {
        val list = listOf("a", "啊", "刺", "c", "博", "b", "にっ", "русский язык")
        val res = list.sortedWith(
            Comparator { a, b ->
                Collator.getInstance().compare(a, b)
            }
        )
        Assertions.assertEquals(res.joinToString(separator = ""), "abcにっрусский язык啊博刺")
    }
}
