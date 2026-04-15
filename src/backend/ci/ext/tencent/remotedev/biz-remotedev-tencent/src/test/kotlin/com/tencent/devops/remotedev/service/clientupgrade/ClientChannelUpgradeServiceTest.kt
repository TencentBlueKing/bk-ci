package com.tencent.devops.remotedev.service.clientupgrade

import com.tencent.devops.remotedev.pojo.ClientUpgradeComp
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientChannel
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientOS
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeOpType
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ClientChannelUpgradeServiceTest {

    private lateinit var configCacheService: ConfigCacheService
    private lateinit var upgradeProps: UpgradeProps
    private lateinit var service: ClientChannelUpgradeService

    @BeforeEach
    fun setUp() {
        configCacheService = mockk()
        upgradeProps = mockk(relaxed = true)
        service = ClientChannelUpgradeService(
            configCacheService = configCacheService,
            upgradeProps = upgradeProps
        )
    }

    // ==================== isValidVersion ====================

    @Test
    fun `isValidVersion accepts release format`() {
        assertTrue(ClientChannel.isValidVersion("3.0.1"))
        assertTrue(ClientChannel.isValidVersion("10.20.30"))
    }

    @Test
    fun `isValidVersion accepts channel format`() {
        assertTrue(ClientChannel.isValidVersion("3.0.1-alpha.1"))
        assertTrue(ClientChannel.isValidVersion("3.0.1-beta.2"))
        assertTrue(ClientChannel.isValidVersion("3.0.1-rc.1"))
        assertTrue(ClientChannel.isValidVersion("3.0.1-RC.10"))
    }

    @Test
    fun `isValidVersion rejects invalid format`() {
        assertFalse(ClientChannel.isValidVersion("3.0.1-release"))
        assertFalse(ClientChannel.isValidVersion("3.0.1-gray.1"))
        assertFalse(ClientChannel.isValidVersion("3.0"))
        assertFalse(ClientChannel.isValidVersion("abc"))
        assertFalse(ClientChannel.isValidVersion(""))
    }

    // ==================== isReleaseVersion ====================

    @Test
    fun `isReleaseVersion returns true for pure semver`() {
        assertTrue(ClientChannel.isReleaseVersion("3.0.1"))
        assertTrue(ClientChannel.isReleaseVersion("1.0.0"))
        assertTrue(ClientChannel.isReleaseVersion("10.20.30"))
    }

    @Test
    fun `isReleaseVersion returns false for channel versions`() {
        assertFalse(ClientChannel.isReleaseVersion("3.0.1-alpha.1"))
        assertFalse(ClientChannel.isReleaseVersion("3.0.1-beta.1"))
        assertFalse(ClientChannel.isReleaseVersion("3.0.1-rc.1"))
        assertFalse(ClientChannel.isReleaseVersion("3.0.1-release"))
        assertFalse(ClientChannel.isReleaseVersion(""))
    }

    // ==================== parseFromVersion ====================

    @Test
    fun `parseFromVersion parses alpha channel`() {
        assertEquals(
            ClientChannel.ALPHA,
            ClientChannel.parseFromVersion("3.0.2-alpha.1")
        )
    }

    @Test
    fun `parseFromVersion parses beta channel`() {
        assertEquals(
            ClientChannel.BETA,
            ClientChannel.parseFromVersion("3.0.2-beta.1")
        )
    }

    @Test
    fun `parseFromVersion parses rc channel`() {
        assertEquals(
            ClientChannel.RC,
            ClientChannel.parseFromVersion("3.0.1-rc.15")
        )
    }

    @Test
    fun `parseFromVersion returns null for pure semver`() {
        assertNull(ClientChannel.parseFromVersion("3.0.1"))
    }

    @Test
    fun `parseFromVersion returns null for empty string`() {
        assertNull(ClientChannel.parseFromVersion(""))
    }

    // ==================== getAffectedChannels ====================

    @Test
    fun `getAffectedChannels for alpha returns only alpha`() {
        assertEquals(
            listOf(ClientChannel.ALPHA),
            ClientChannel.getAffectedChannels(ClientChannel.ALPHA)
        )
    }

    @Test
    fun `getAffectedChannels for beta returns alpha and beta`() {
        assertEquals(
            listOf(ClientChannel.ALPHA, ClientChannel.BETA),
            ClientChannel.getAffectedChannels(ClientChannel.BETA)
        )
    }

    @Test
    fun `getAffectedChannels for rc returns all channels`() {
        assertEquals(
            listOf(
                ClientChannel.ALPHA,
                ClientChannel.BETA,
                ClientChannel.RC
            ),
            ClientChannel.getAffectedChannels(ClientChannel.RC)
        )
    }

    @Test
    fun `getAffectedChannels for null returns empty`() {
        assertEquals(
            emptyList<ClientChannel>(),
            ClientChannel.getAffectedChannels(null)
        )
    }

    // ==================== setChannelVersion ====================

    @Test
    fun `setChannelVersion with alpha sets only alpha users`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1;user2"

        service.setChannelVersion("3.0.2-alpha.1")

        val expected = mapOf(
            "user1" to "3.0.2-alpha.1",
            "user2" to "3.0.2-alpha.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.WINDOWS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with beta sets alpha and beta users`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1;user2"
        every {
            configCacheService.get("remotedev:clientChannel:beta")
        } returns "user3;user4"

        service.setChannelVersion("3.0.2-beta.1")

        val expected = mapOf(
            "user1" to "3.0.2-beta.1",
            "user2" to "3.0.2-beta.1",
            "user3" to "3.0.2-beta.1",
            "user4" to "3.0.2-beta.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.WINDOWS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with rc sets all channel users`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1"
        every {
            configCacheService.get("remotedev:clientChannel:beta")
        } returns "user2"
        every {
            configCacheService.get("remotedev:clientChannel:rc")
        } returns "user3"

        service.setChannelVersion("3.0.1-rc.1")

        val expected = mapOf(
            "user1" to "3.0.1-rc.1",
            "user2" to "3.0.1-rc.1",
            "user3" to "3.0.1-rc.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.WINDOWS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with release sets all channel users`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1"
        every {
            configCacheService.get("remotedev:clientChannel:beta")
        } returns "user2"
        every {
            configCacheService.get("remotedev:clientChannel:rc")
        } returns "user3"

        service.setChannelVersion("3.0.2")

        val expected = mapOf(
            "user1" to "3.0.2",
            "user2" to "3.0.2",
            "user3" to "3.0.2"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.WINDOWS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion deduplicates users across channels`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1;user2"
        every {
            configCacheService.get("remotedev:clientChannel:beta")
        } returns "user2;user3"

        service.setChannelVersion("3.0.2-beta.1")

        val expected = mapOf(
            "user1" to "3.0.2-beta.1",
            "user2" to "3.0.2-beta.1",
            "user3" to "3.0.2-beta.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with empty config skips without error`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns null

        service.setChannelVersion("3.0.2-alpha.1")

        verify(exactly = 0) {
            upgradeProps.setUserVersion(
                comp = any(),
                os = any(),
                version = any(),
                opType = any()
            )
        }
    }

    @Test
    fun `setChannelVersion supports comma separator`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1,user2, user3"

        service.setChannelVersion("3.0.2-alpha.1")

        val expected = mapOf(
            "user1" to "3.0.2-alpha.1",
            "user2" to "3.0.2-alpha.1",
            "user3" to "3.0.2-alpha.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expected,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with invalid format skips`() {
        service.setChannelVersion("3.0.1-gray.1")

        verify(exactly = 0) {
            upgradeProps.setUserVersion(
                comp = any(),
                os = any(),
                version = any(),
                opType = any()
            )
        }
    }

    @Test
    fun `setChannelVersion with blank version skips`() {
        service.setChannelVersion("")

        verify(exactly = 0) {
            upgradeProps.setUserVersion(
                comp = any(),
                os = any(),
                version = any(),
                opType = any()
            )
        }
    }
}
