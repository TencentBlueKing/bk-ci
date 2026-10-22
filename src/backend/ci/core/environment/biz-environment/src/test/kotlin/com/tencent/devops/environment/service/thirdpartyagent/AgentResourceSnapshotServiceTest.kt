package com.tencent.devops.environment.service.thirdpartyagent

import com.tencent.devops.environment.dao.thirdpartyagent.AgentResourceSnapshotDao
import io.mockk.every
import io.mockk.mockk
import org.jooq.DSLContext
import org.jooq.TransactionalRunnable
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

class AgentResourceSnapshotServiceTest {
    @Test
    fun `optional database outage does not fail primary telemetry`() {
        val context = mockk<DSLContext>()
        every { context.transaction(any<TransactionalRunnable>()) } throws DataAccessException("test outage")
        val service = AgentResourceSnapshotService(context, AgentResourceSnapshotDao())
        assertDoesNotThrow { service.report("test", "agent", "secret", "{}") }
    }
}
