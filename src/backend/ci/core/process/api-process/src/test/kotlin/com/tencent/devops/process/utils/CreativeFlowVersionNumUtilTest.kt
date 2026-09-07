package com.tencent.devops.process.utils

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.process.constant.ProcessMessageCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CreativeFlowVersionNumUtilTest {

    @Test
    fun `parse standard V208`() {
        assertEquals(208, CreativeFlowVersionNumUtil.parse("V208"))
    }

    @Test
    fun `parse bare number with warn`() {
        assertEquals(208, CreativeFlowVersionNumUtil.parse("208"))
    }

    @Test
    fun `parse lowercase v208`() {
        assertEquals(208, CreativeFlowVersionNumUtil.parse("v208"))
    }

    @Test
    fun `parse rejects P208`() {
        val ex = assertThrows(ErrorCodeException::class.java) {
            CreativeFlowVersionNumUtil.parse("P208")
        }
        assertEquals(ProcessMessageCode.ERROR_CREATIVE_FLOW_VERSION_NUM_INVALID, ex.errorCode)
    }

    @Test
    fun `parse rejects empty`() {
        assertThrows(ErrorCodeException::class.java) {
            CreativeFlowVersionNumUtil.parse("")
        }
    }

    @Test
    fun `parse rejects negative-like`() {
        assertThrows(ErrorCodeException::class.java) {
            CreativeFlowVersionNumUtil.parse("V-1")
        }
    }

    @Test
    fun `format produces V prefix`() {
        assertEquals("V208", CreativeFlowVersionNumUtil.format(208))
        assertEquals("V1", CreativeFlowVersionNumUtil.format(1))
    }
}
