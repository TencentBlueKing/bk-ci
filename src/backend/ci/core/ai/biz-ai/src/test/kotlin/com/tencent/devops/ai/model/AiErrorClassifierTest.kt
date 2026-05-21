package com.tencent.devops.ai.model

import io.agentscope.core.model.ModelException
import io.agentscope.core.model.exception.AuthenticationException
import io.agentscope.core.model.exception.BadRequestException
import io.agentscope.core.model.exception.InternalServerException
import io.agentscope.core.model.exception.NotFoundException
import io.agentscope.core.model.exception.OpenAIException
import io.agentscope.core.model.exception.PermissionDeniedException
import io.agentscope.core.model.exception.RateLimitException
import io.agentscope.core.model.exception.UnprocessableEntityException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.TimeoutException

class AiErrorClassifierTest {

    private val classifier = AiErrorClassifier()

    @Test
    fun `4xx client errors should not be retryable`() {
        assertFalse(classifier.isRetryable(BadRequestException("bad", null, null)))
        assertFalse(classifier.isRetryable(AuthenticationException("unauthorized", null, null)))
        assertFalse(classifier.isRetryable(PermissionDeniedException("forbidden", null, null)))
        assertFalse(classifier.isRetryable(NotFoundException("not found", null, null)))
        assertFalse(classifier.isRetryable(UnprocessableEntityException("422", null, null)))
    }

    @Test
    fun `rate limit and 5xx errors should be retryable`() {
        assertTrue(classifier.isRetryable(RateLimitException("too many", null, null)))
        assertTrue(
            classifier.isRetryable(
                InternalServerException("ise", 500, null, null)
            )
        )
        assertTrue(
            classifier.isRetryable(
                OpenAIException("bad gateway", 502, null, null)
            )
        )
        assertTrue(
            classifier.isRetryable(
                OpenAIException("gateway timeout", 504, null, null)
            )
        )
    }

    @Test
    fun `OpenAI exception with non-5xx status should not be retryable by status branch`() {
        val unknown = OpenAIException("teapot", 418, null, null)
        // 418 既不在 4xx 显式黑名单，也不在 5xx 白名单，
        // 走兜底 RETRYABLE_ERRORS：未知异常默认 false。
        assertFalse(classifier.isRetryable(unknown))
    }

    @Test
    fun `ModelException with streaming retryable keyword should be retryable`() {
        assertTrue(classifier.isRetryable(ModelException("Stream timeout exceeded")))
        assertTrue(classifier.isRetryable(ModelException("Connection reset by peer")))
        assertTrue(classifier.isRetryable(ModelException("broken pipe writing to remote")))
        assertTrue(classifier.isRetryable(ModelException("premature EOF on SSE stream")))
        assertTrue(classifier.isRetryable(ModelException("unexpected end of stream")))
    }

    @Test
    fun `ModelException without keyword should fall back to retryable check`() {
        assertFalse(classifier.isRetryable(ModelException("some unrelated failure")))
        assertFalse(classifier.isRetryable(ModelException(null)))
    }

    @Test
    fun `IOException and TimeoutException should be retryable via fallback`() {
        assertTrue(classifier.isRetryable(IOException("socket read failed")))
        assertTrue(classifier.isRetryable(TimeoutException("execution timed out")))
    }

    @Test
    fun `wrapped IOException should be retryable via cause chain`() {
        val wrapped = RuntimeException("outer", IOException("Connection reset"))
        assertTrue(classifier.isRetryable(wrapped))
    }

    @Test
    fun `unknown runtime exceptions should not be retryable by default`() {
        assertFalse(classifier.isRetryable(IllegalStateException("oops")))
        assertFalse(classifier.isRetryable(IllegalArgumentException("bad arg")))
    }

    @Test
    fun `describeCauseChain returns none when no cause present`() {
        val err = RuntimeException("standalone")
        assertEquals("none", classifier.describeCauseChain(err))
    }

    @Test
    fun `describeCauseChain renders cause chain with type and message`() {
        val root = IOException("Connection reset")
        val mid = RuntimeException("layer-1", root)
        val top = IllegalStateException("layer-0", mid)
        val rendered = classifier.describeCauseChain(top)
        assertEquals(
            "RuntimeException(layer-1) -> IOException(Connection reset)",
            rendered
        )
    }

    @Test
    fun `describeCauseChain truncates after five levels`() {
        // 构造深度 7 的 cause 链：top -> c1 -> c2 -> c3 -> c4 -> c5 -> c6
        val c6 = RuntimeException("c6")
        val c5 = RuntimeException("c5", c6)
        val c4 = RuntimeException("c4", c5)
        val c3 = RuntimeException("c3", c4)
        val c2 = RuntimeException("c2", c3)
        val c1 = RuntimeException("c1", c2)
        val top = RuntimeException("top", c1)

        val rendered = classifier.describeCauseChain(top)
        val segments = rendered.split(" -> ")
        assertEquals(5, segments.size)
        assertEquals("RuntimeException(c1)", segments.first())
        assertEquals("RuntimeException(c5)", segments.last())
    }

    @Test
    fun `describeCauseChain handles cause with null message`() {
        val root = RuntimeException(null as String?)
        val top = RuntimeException("outer", root)
        val rendered = classifier.describeCauseChain(top)
        assertEquals("RuntimeException(null)", rendered)
    }
}
