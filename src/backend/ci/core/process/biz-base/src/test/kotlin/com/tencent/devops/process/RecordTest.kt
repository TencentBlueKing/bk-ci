package com.tencent.devops.process

import org.junit.jupiter.api.Test

/**
 * @ Author     ：Royal Huang
 * @ Date       ：Created in 21:21 2022/11/25
 */

class RecordTest {

    @Test
    fun getVarName() {
        val myA = B("xxx")
        println(myA::a.name)
        println(myA::b.name)
    }
}

abstract class A(
    val a: String
)

data class B(
    val b: String
) : A(b)
