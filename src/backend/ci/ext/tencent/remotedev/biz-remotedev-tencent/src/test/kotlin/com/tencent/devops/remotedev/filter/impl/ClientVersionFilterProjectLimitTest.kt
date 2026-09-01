package com.tencent.devops.remotedev.filter.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.remotedev.dao.ClientDao
import com.tencent.devops.remotedev.dao.ClientVersionDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.redis.RedisKeys
import com.tencent.devops.remotedev.service.workspace.NotifyControl
import io.mockk.every
import io.mockk.mockk
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.UriInfo
import jakarta.ws.rs.container.ContainerRequestContext
import java.net.URI
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ClientVersionFilterProjectLimitTest {

    private lateinit var cacheService: ConfigCacheService
    private lateinit var clientVersionDao: ClientVersionDao
    private lateinit var dslContext: DSLContext
    private lateinit var notifyControl: NotifyControl
    private lateinit var clientDao: ClientDao
    private lateinit var workspaceJoinDao: WorkspaceJoinDao
    private lateinit var filter: ClientVersionFilter

    @BeforeEach
    fun setUp() {
        cacheService = mockk(relaxed = true)
        clientVersionDao = mockk(relaxed = true)
        dslContext = mockk(relaxed = true)
        notifyControl = mockk(relaxed = true)
        clientDao = mockk(relaxed = true)
        workspaceJoinDao = mockk(relaxed = true)

        every {
            cacheService.get(RedisKeys.REDIS_CLIENT_VERSION_CHECK)
        } returns "true"

        every { clientVersionDao.fetchAll(any()) } returns emptyList()

        filter = ClientVersionFilter(
            cacheService = cacheService,
            clientVersionDao = clientVersionDao,
            dslContext = dslContext,
            notifyControl = notifyControl,
            clientDao = clientDao,
            workspaceJoinDao = workspaceJoinDao
        )
    }

    @Nested
    inner class CompareVersionTest {

        @Test
        fun `equal versions return 0`() {
            assertEquals(
                0,
                filter.compareVersion(listOf(2, 0, 0), listOf(2, 0, 0))
            )
        }

        @Test
        fun `first version greater returns positive`() {
            assertTrue(
                filter.compareVersion(listOf(2, 1, 0), listOf(2, 0, 0)) > 0
            )
        }

        @Test
        fun `first version lesser returns negative`() {
            assertTrue(
                filter.compareVersion(listOf(1, 9, 9), listOf(2, 0, 0)) < 0
            )
        }

        @Test
        fun `different lengths - shorter padded with zero`() {
            assertEquals(
                0,
                filter.compareVersion(listOf(2, 0), listOf(2, 0, 0))
            )
            assertTrue(
                filter.compareVersion(listOf(2, 0, 1), listOf(2, 0)) > 0
            )
            assertTrue(
                filter.compareVersion(listOf(2, 0), listOf(2, 0, 1)) < 0
            )
        }

        @Test
        fun `empty lists return 0`() {
            assertEquals(0, filter.compareVersion(emptyList(), emptyList()))
        }

        @Test
        fun `one empty list treated as zeros`() {
            assertTrue(
                filter.compareVersion(listOf(1, 0, 0), emptyList()) > 0
            )
            assertTrue(
                filter.compareVersion(emptyList(), listOf(1, 0, 0)) < 0
            )
        }

        @Test
        fun `multi-segment comparison`() {
            assertTrue(
                filter.compareVersion(
                    listOf(2, 1, 2),
                    listOf(2, 1, 0)
                ) > 0
            )
            assertTrue(
                filter.compareVersion(
                    listOf(3, 0, 0),
                    listOf(2, 9, 9)
                ) > 0
            )
        }
    }

    @Nested
    inner class VerifyWithProjectLimitTest {

        private fun buildRequestContext(
            userId: String,
            version: String,
            path: String = "/api/user/remotedev/settings"
        ): ContainerRequestContext {
            val ctx = mockk<ContainerRequestContext>(relaxed = true)
            val uriInfo = mockk<UriInfo>()
            every { uriInfo.requestUri } returns URI.create(path)
            every { ctx.uriInfo } returns uriInfo

            val headers = MultivaluedHashMap<String, String>()
            headers.putSingle(AUTH_HEADER_USER_ID, userId)
            headers.putSingle("BK-CI-CLIENT-VERSION", version)
            headers.putSingle("x-client-ip", "127.0.0.1")
            headers.putSingle("BK-CI-CLIENT-MAC", "AA:BB:CC:DD:EE:FF")
            headers.putSingle("BK-CI-CLIENT-OS", "mac")
            headers.putSingle("BK-CI-CLIENT-START-VERSION", version)
            every { ctx.headers } returns headers

            return ctx
        }

        @Test
        fun `no project config - uses global baseline`() {
            every { cacheService.get("remotedev:clientVersionLimit") } returns "2.0.0"
            every { cacheService.get("remotedev:clientVersionWarning") } returns null
            every {
                workspaceJoinDao.fetchProjectFromUser(any(), "testUser")
            } returns emptySet()

            val ctx = buildRequestContext("testUser", "2.0.0")
            assertTrue(filter.verify(ctx))
        }

        @Test
        fun `no project config - version below global blocked`() {
            every { cacheService.get("remotedev:clientVersionLimit") } returns "2.0.0"
            every { cacheService.get("remotedev:clientVersionWarning") } returns null
            every {
                workspaceJoinDao.fetchProjectFromUser(any(), "testUser")
            } returns emptySet()

            val ctx = buildRequestContext("testUser", "1.9.9")
            assertFalse(filter.verify(ctx))
        }

        @Test
        fun `project config higher than global - uses project baseline`() {
            every { cacheService.get("remotedev:clientVersionLimit") } returns "2.0.0"
            every {
                cacheService.get("remotedev:clientVersionLimit:gts")
            } returns "2.1.2"
            every { cacheService.get("remotedev:clientVersionWarning") } returns null
            every {
                workspaceJoinDao.fetchProjectFromUser(any(), "testUser")
            } returns setOf("gts")

            val ctxPass = buildRequestContext("testUser", "2.1.2")
            assertTrue(filter.verify(ctxPass))

            val ctxFail = buildRequestContext("testUser", "2.1.1")
            assertFalse(filter.verify(ctxFail))
        }

        @Test
        fun `project config lower than global - still uses global`() {
            every { cacheService.get("remotedev:clientVersionLimit") } returns "2.0.0"
            every {
                cacheService.get("remotedev:clientVersionLimit:lowproj")
            } returns "1.5.0"
            every { cacheService.get("remotedev:clientVersionWarning") } returns null
            every {
                workspaceJoinDao.fetchProjectFromUser(any(), "testUser")
            } returns setOf("lowproj")

            val ctx = buildRequestContext("testUser", "2.0.0")
            assertTrue(filter.verify(ctx))

            val ctxFail = buildRequestContext("testUser", "1.9.9")
            assertFalse(filter.verify(ctxFail))
        }

        @Test
        fun `multiple projects - uses highest project baseline`() {
            every { cacheService.get("remotedev:clientVersionLimit") } returns "2.0.0"
            every {
                cacheService.get("remotedev:clientVersionLimit:projA")
            } returns "2.1.0"
            every {
                cacheService.get("remotedev:clientVersionLimit:projB")
            } returns "2.2.0"
            every {
                cacheService.get("remotedev:clientVersionLimit:projC")
            } returns null
            every { cacheService.get("remotedev:clientVersionWarning") } returns null
            every {
                workspaceJoinDao.fetchProjectFromUser(any(), "testUser")
            } returns setOf("projA", "projB", "projC")

            val ctxPass = buildRequestContext("testUser", "2.2.0")
            assertTrue(filter.verify(ctxPass))

            val ctxFail = buildRequestContext("testUser", "2.1.9")
            assertFalse(filter.verify(ctxFail))
        }

        @Test
        fun `version check switch off - always passes`() {
            every {
                cacheService.get(RedisKeys.REDIS_CLIENT_VERSION_CHECK)
            } returns "false"

            val ctx = buildRequestContext("testUser", "0.0.1")
            assertTrue(filter.verify(ctx))
        }

        @Test
        fun `version above project baseline passes`() {
            every { cacheService.get("remotedev:clientVersionLimit") } returns "2.0.0"
            every {
                cacheService.get("remotedev:clientVersionLimit:gts")
            } returns "2.1.0"
            every { cacheService.get("remotedev:clientVersionWarning") } returns null
            every {
                workspaceJoinDao.fetchProjectFromUser(any(), "testUser")
            } returns setOf("gts")

            val ctx = buildRequestContext("testUser", "3.0.0")
            assertTrue(filter.verify(ctx))
        }

        @Test
        fun `no global config and no project config - passes`() {
            every { cacheService.get("remotedev:clientVersionLimit") } returns null
            every { cacheService.get("remotedev:clientVersionWarning") } returns null
            every {
                workspaceJoinDao.fetchProjectFromUser(any(), "testUser")
            } returns emptySet()

            val ctx = buildRequestContext("testUser", "0.0.1")
            assertTrue(filter.verify(ctx))
        }
    }
}
