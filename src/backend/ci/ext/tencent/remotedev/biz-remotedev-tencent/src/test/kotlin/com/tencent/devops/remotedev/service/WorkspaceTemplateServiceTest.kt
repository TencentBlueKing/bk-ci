package com.tencent.devops.remotedev.service

import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.remotedev.dao.WorkspaceTemplateDao
import com.tencent.devops.remotedev.pojo.WorkspaceTemplate
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

internal class WorkspaceTemplateServiceTest : BkCiAbstractTest() {
    private val workspaceTemplateDao: WorkspaceTemplateDao = mockk()
    private var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val self: WorkspaceTemplateService = spyk(
        WorkspaceTemplateService(
            dslContext = dslContext,
            client = client,
            workspaceTemplateDao = workspaceTemplateDao
        ),
        recordPrivateCalls = true
    )

    private val workspaceTemplate = WorkspaceTemplate(
        wsTemplateId = 1,
        image = "123",
        name = "test",
        source = "test",
        logo = "111",
        description = "test desc"
    )

    @Nested
    inner class AddWorkspaceTemplateTest {
        @Test
        @DisplayName("正常用例")
        fun addWorkspaceTemplateTest_01() {
            every {
                workspaceTemplateDao.createWorkspaceTemplate(
                    userId = any(),
                    workspaceTemplate = any(),
                    dslContext = anyDslContext()
                )
            } returns Unit

            Assertions.assertEquals(
                self.addWorkspaceTemplate(
                    userId = "user00", workspaceTemplate = workspaceTemplate
                ),
                true
            )
        }
    }

    @Nested
    inner class UpdateWorkspaceTemplateTest {
        @Test
        @DisplayName("正常用例")
        fun updateWorkspaceTemplateTest_01() {

            every {
                workspaceTemplateDao.updateWorkspaceTemplate(
                    wsTemplateId = any(),
                    workspaceTemplate = any(),
                    dslContext = anyDslContext()
                )
            } returns Unit

            Assertions.assertEquals(
                self.updateWorkspaceTemplate(
                    wsTemplateId = 1, workspaceTemplate = workspaceTemplate
                ),
                true
            )
        }
    }

    @Nested
    inner class DeleteWorkspaceTemplateTest {
        @Test
        @DisplayName("正常用例")
        fun deleteWorkspaceTemplateTest_01() {
            every {
                workspaceTemplateDao.deleteWorkspaceTemplate(
                    wsTemplateId = any(),
                    dslContext = anyDslContext()
                )
            } returns Unit

            Assertions.assertEquals(
                self.deleteWorkspaceTemplate(
                    wsTemplateId = 1
                ),
                true
            )
        }
    }
}
