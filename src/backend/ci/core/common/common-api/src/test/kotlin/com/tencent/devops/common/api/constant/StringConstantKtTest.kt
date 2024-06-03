package com.tencent.devops.common.api.constant

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringConstantKtTest {

    @Test
    fun coerceAtMaxLength() {
        val expect = "12345678"
        val s10 = "1234567890"
        assertEquals(expect, s10.coerceAtMaxLength(expect.length))
        assertEquals(s10, s10.coerceAtMaxLength(s10.length))
        assertEquals(s10, s10.coerceAtMaxLength(s10.length + 100))
    }
}
