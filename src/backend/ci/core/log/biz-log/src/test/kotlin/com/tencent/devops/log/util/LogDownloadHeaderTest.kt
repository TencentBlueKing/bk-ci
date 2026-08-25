package com.tencent.devops.log.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class LogDownloadHeaderTest {

    @Test
    fun contentDisposition_stripsHeaderInjection() {
        val injected = "ok\r\nSet-Cookie: a=1\"evil"
        val header = LogDownloadHeader.contentDisposition(injected, "p-1", "b-1")
        assertFalse(header.contains("\r"))
        assertFalse(header.contains("\n"))
        assertEquals(
            "attachment; filename=\"okSet-Cookiea1evil.log\"; filename*=UTF-8''okSet-Cookiea1evil.log",
            header
        )
    }

    @Test
    fun contentDisposition_fallbackWhenBlankAfterSanitize() {
        val header = LogDownloadHeader.contentDisposition("@@@", "p-abc", "b-xyz")
        assertEquals(
            "attachment; filename=\"p-abc-b-xyz-log.log\"; filename*=UTF-8''p-abc-b-xyz-log.log",
            header
        )
    }

    @Test
    fun sanitize_keepsSafeCharsOnly() {
        assertEquals("p-1_build.log", LogDownloadHeader.sanitize("p-1_build.log"))
        assertEquals("ab", LogDownloadHeader.sanitize("a b/c"))
    }
}
