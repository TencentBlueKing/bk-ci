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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun `parseFromVersion should parse alpha channel`() {
        assertEquals(
            ClientChannel.ALPHA,
            ClientChannel.parseFromVersion("3.0.2-alpha.1")
        )
    }

    @Test
    fun `parseFromVersion should parse beta channel`() {
        assertEquals(
            ClientChannel.BETA,
            ClientChannel.parseFromVersion("3.0.2-beta.1")
        )
    }

    @Test
    fun `parseFromVersion should parse gray channel`() {
        assertEquals(
            ClientChannel.GRAY,
            ClientChannel.parseFromVersion("3.0.1-gray.15")
        )
    }

    @Test
    fun `parseFromVersion should return null for release`() {
        assertNull(ClientChannel.parseFromVersion("3.0.2-release"))
    }

    @Test
    fun `parseFromVersion should return null for no suffix`() {
        assertNull(ClientChannel.parseFromVersion("3.0.2"))
    }

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
    fun `getAffectedChannels for gray returns all channels`() {
        assertEquals(
            listOf(
                ClientChannel.ALPHA,
                ClientChannel.BETA,
                ClientChannel.GRAY
            ),
            ClientChannel.getAffectedChannels(ClientChannel.GRAY)
        )
    }

    @Test
    fun `getAffectedChannels for release returns all channels`() {
        assertEquals(
            listOf(
                ClientChannel.ALPHA,
                ClientChannel.BETA,
                ClientChannel.GRAY
            ),
            ClientChannel.getAffectedChannels(null)
        )
    }

    @Test
    fun `setChannelVersion with alpha version sets only alpha users`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1;user2"

        service.setChannelVersion("3.0.2-alpha.1")

        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = mapOf("user1" to "3.0.2-alpha.1", "user2" to "3.0.2-alpha.1"),
                opType = ClientUpgradeOpType.ADD
            )
        }
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.WINDOWS,
                version = mapOf("user1" to "3.0.2-alpha.1", "user2" to "3.0.2-alpha.1"),
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with beta version sets alpha and beta users`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1;user2"
        every {
            configCacheService.get("remotedev:clientChannel:beta")
        } returns "user3;user4"

        service.setChannelVersion("3.0.2-beta.1")

        val expectedMap = mapOf(
            "user1" to "3.0.2-beta.1",
            "user2" to "3.0.2-beta.1",
            "user3" to "3.0.2-beta.1",
            "user4" to "3.0.2-beta.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expectedMap,
                opType = ClientUpgradeOpType.ADD
            )
        }
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.WINDOWS,
                version = expectedMap,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with release version sets all channel users`() {
        every {
            configCacheService.get("remotedev:clientChannel:alpha")
        } returns "user1"
        every {
            configCacheService.get("remotedev:clientChannel:beta")
        } returns "user2"
        every {
            configCacheService.get("remotedev:clientChannel:gray")
        } returns "user3"

        service.setChannelVersion("3.0.2-release")

        val expectedMap = mapOf(
            "user1" to "3.0.2-release",
            "user2" to "3.0.2-release",
            "user3" to "3.0.2-release"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expectedMap,
                opType = ClientUpgradeOpType.ADD
            )
        }
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.WINDOWS,
                version = expectedMap,
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

        val expectedMap = mapOf(
            "user1" to "3.0.2-beta.1",
            "user2" to "3.0.2-beta.1",
            "user3" to "3.0.2-beta.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expectedMap,
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

        val expectedMap = mapOf(
            "user1" to "3.0.2-alpha.1",
            "user2" to "3.0.2-alpha.1",
            "user3" to "3.0.2-alpha.1"
        )
        verify(exactly = 1) {
            upgradeProps.setUserVersion(
                comp = ClientUpgradeComp.CLIENT,
                os = ClientOS.MACOS,
                version = expectedMap,
                opType = ClientUpgradeOpType.ADD
            )
        }
    }

    @Test
    fun `setChannelVersion with blank version throws exception`() {
        assertThrows<IllegalArgumentException> {
            service.setChannelVersion("")
        }
    }
}
