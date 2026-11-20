package com.tencent.devops.repository.sdk.tapd.service

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.scm.pojo.tapd.ParticipantWorkspaceResponse
import com.tencent.devops.scm.pojo.tapd.TapdBug
import com.tencent.devops.scm.pojo.tapd.TapdBugFieldConfig
import com.tencent.devops.scm.pojo.tapd.TapdStory
import com.tencent.devops.scm.pojo.tapd.TapdStoryFieldConfig
import com.tencent.devops.scm.pojo.tapd.TapdStatusField
import com.tencent.devops.scm.pojo.tapd.TapdWorkspace
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TapdDropdownServiceTest {

    private class FakeItemService(
        private val workspaces: List<ParticipantWorkspaceResponse> = emptyList(),
        private val granted: Set<String>? = emptySet(),
        private val installed: Set<String>? = emptySet(),
        private val storyField: TapdStoryFieldConfig? = null,
        private val bugField: TapdBugFieldConfig? = null
    ) : ITapdItemService {
        var grantedCalls = 0
        var installedCalls = 0
        var storyCalls = 0
        var bugCalls = 0

        override fun getStoryInfo(workspaceId: String, storyId: String): TapdStory? = null

        override fun getBugInfo(workspaceId: String, bugId: String): TapdBug? = null

        override fun getBugFieldsInfo(workspaceId: String): TapdBugFieldConfig? = null

        override fun getWorkspaceInfo(workspaceId: String): TapdWorkspace? = null

        override fun listParticipantWorkspaces(
            nick: String,
            status: String?
        ): List<ParticipantWorkspaceResponse> = workspaces

        override fun listGrantedWorkspaceIds(): Set<String>? {
            grantedCalls++
            return granted
        }

        override fun listInstalledWorkspaceIds(): Set<String>? {
            installedCalls++
            return installed
        }

        override fun getStoryFieldConfig(workspaceId: String): TapdStoryFieldConfig? {
            storyCalls++
            return storyField
        }

        override fun getBugFieldConfig(workspaceId: String, allOptions: Boolean): TapdBugFieldConfig? {
            bugCalls++
            return bugField
        }
    }

    private fun workspace(id: String, name: String) = ParticipantWorkspaceResponse(
        workspace = TapdWorkspace(id = id, name = name)
    )

    @Test
    fun `workspace options only return authorized projects`() {
        val service = TapdDropdownService(
            FakeItemService(
                workspaces = listOf(
                    workspace("70247651", "ivfzhou-test"),
                    workspace("10104091", "蓝鲸")
                ),
                granted = setOf("70247651"),
                installed = setOf("70247651")
            )
        )
        val options = service.workspaceOptions("v_ivfzhou")
        assertEquals(1, options.size)
        assertEquals("70247651", options[0].id)
        assertEquals("ivfzhou-test", options[0].name)
        assertFalse(options[0].disabled)
        assertTrue(options[0].name.contains("未安装或未授权").not())
    }

    @Test
    fun `unknown authorization returns empty workspace options`() {
        val service = TapdDropdownService(
            FakeItemService(
                workspaces = listOf(workspace("70247651", "ivfzhou-test")),
                granted = null,
                installed = setOf("70247651")
            )
        )
        assertFalse(service.authorizedWorkspaces().known)
        assertTrue(service.workspaceOptions("v_ivfzhou").isEmpty())
    }

    @Test
    fun `blank user id returns empty workspace options`() {
        val service = TapdDropdownService(FakeItemService())
        assertTrue(service.workspaceOptions("").isEmpty())
    }

    @Test
    fun `authorized workspaces are cached only when known`() {
        val fake = FakeItemService(
            granted = setOf("70247651"),
            installed = setOf("70247651")
        )
        val service = TapdDropdownService(fake)
        assertTrue(service.authorizedWorkspaces().known)
        assertTrue(service.authorizedWorkspaces().known)
        assertEquals(1, fake.grantedCalls)
        assertEquals(1, fake.installedCalls)
    }

    @Test
    fun `unknown authorized workspaces are not cached`() {
        val fake = FakeItemService(granted = null, installed = setOf("70247651"))
        val service = TapdDropdownService(fake)
        assertFalse(service.authorizedWorkspaces().known)
        assertFalse(service.authorizedWorkspaces().known)
        assertEquals(2, fake.grantedCalls)
        assertEquals(2, fake.installedCalls)
    }

    @Test
    fun `story status options are converted from map`() {
        val service = TapdDropdownService(
            FakeItemService(
                granted = setOf("70247651"),
                installed = setOf("70247651"),
                storyField = TapdStoryFieldConfig(
                    status = TapdStatusField(
                        options = mapOf("planning" to "规划中", "developing" to "实现中")
                    )
                )
            )
        )
        val options = service.statusOptions("70247651", "STORY")
        assertEquals(listOf("developing", "planning"), options.map { it.id })
        assertEquals("规划中", options.first { it.id == "planning" }.name)
    }

    @Test
    fun `bug status options use bug field config`() {
        val service = TapdDropdownService(
            FakeItemService(
                granted = setOf("70247651"),
                installed = setOf("70247651"),
                bugField = TapdBugFieldConfig(
                    status = TapdStatusField(options = mapOf("new" to "新", "closed" to "已关闭"))
                )
            )
        )
        val options = service.statusOptions("70247651", "bug")
        assertEquals(listOf("closed", "new"), options.map { it.id })
    }

    @Test
    fun `status options are cached by workspace and event type`() {
        val fake = FakeItemService(
            granted = setOf("70247651"),
            installed = setOf("70247651"),
            storyField = TapdStoryFieldConfig(
                status = TapdStatusField(options = mapOf("developing" to "实现中"))
            )
        )
        val service = TapdDropdownService(fake)
        assertEquals(1, service.statusOptions("70247651", "story").size)
        assertEquals(1, service.statusOptions("70247651", "STORY").size)
        assertEquals(1, fake.storyCalls)
    }

    @Test
    fun `status options empty for unauthorized workspace`() {
        val fake = FakeItemService(
            granted = setOf("70247651"),
            installed = setOf("70247651"),
            storyField = TapdStoryFieldConfig(
                status = TapdStatusField(options = mapOf("developing" to "实现中"))
            )
        )
        val service = TapdDropdownService(fake)
        assertTrue(service.statusOptions("999", "STORY").isEmpty())
        assertEquals(0, fake.storyCalls)
    }

    @Test
    fun `status options empty when authorized workspaces unknown`() {
        val fake = FakeItemService(
            granted = null,
            installed = setOf("70247651"),
            storyField = TapdStoryFieldConfig(
                status = TapdStatusField(options = mapOf("developing" to "实现中"))
            )
        )
        val service = TapdDropdownService(fake)
        assertTrue(service.statusOptions("70247651", "STORY").isEmpty())
        assertEquals(0, fake.storyCalls)
    }

    @Test
    fun `status options missing field config is not cached`() {
        val fake = FakeItemService(
            granted = setOf("70247651"),
            installed = setOf("70247651")
        )
        val service = TapdDropdownService(fake)
        assertTrue(service.statusOptions("70247651", "STORY").isEmpty())
        assertTrue(service.statusOptions("70247651", "STORY").isEmpty())
        assertEquals(2, fake.storyCalls)
    }

    @Test
    fun `invalid status request is rejected`() {
        val service = TapdDropdownService(FakeItemService())
        assertThrows(InvalidParamException::class.java) {
            service.statusOptions("", "STORY")
        }
        assertThrows(InvalidParamException::class.java) {
            service.statusOptions("70247651", "TASK")
        }
    }
}
