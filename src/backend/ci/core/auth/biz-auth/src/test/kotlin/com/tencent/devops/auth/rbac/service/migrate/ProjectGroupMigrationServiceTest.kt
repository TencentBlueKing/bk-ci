package com.tencent.devops.auth.rbac.service.migrate

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.dao.AuthResourceGroupPermissionDao
import com.tencent.devops.auth.pojo.AuthResourceGroup
import com.tencent.devops.auth.pojo.AuthResourceGroupMember
import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.auth.pojo.dto.ProjectGroupIncrementalMigrationDTO
import com.tencent.devops.auth.pojo.dto.ProjectGroupMigrationDTO
import com.tencent.devops.auth.pojo.dto.ResourceGroupPermissionDTO
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.pojo.enum.ProjectGroupMigrationStatus
import com.tencent.devops.auth.provider.rbac.service.AuthResourceCodeConverter
import com.tencent.devops.auth.provider.rbac.service.AuthResourceService
import com.tencent.devops.auth.provider.rbac.service.migrate.ProjectGroupMigrationService
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupPermissionService
import com.tencent.devops.auth.service.iam.PermissionResourceGroupService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthResourceType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ProjectGroupMigrationServiceTest {

    private val dslContext = mockk<DSLContext>()
    private val iamConfiguration = mockk<IamConfiguration>()
    private val authResourceService = mockk<AuthResourceService>()
    private val authResourceCodeConverter = mockk<AuthResourceCodeConverter>()
    private val authResourceGroupDao = mockk<AuthResourceGroupDao>()
    private val authResourceGroupMemberDao = mockk<AuthResourceGroupMemberDao>()
    private val authResourceGroupPermissionDao = mockk<AuthResourceGroupPermissionDao>()
    private val permissionResourceGroupService = mockk<PermissionResourceGroupService>()
    private val permissionResourceGroupPermissionService = mockk<PermissionResourceGroupPermissionService>()
    private val permissionResourceMemberService = mockk<PermissionResourceMemberService>()
    private val deptService = mockk<DeptService>()

    private val service = ProjectGroupMigrationService(
        dslContext = dslContext,
        iamConfiguration = iamConfiguration,
        authResourceService = authResourceService,
        authResourceCodeConverter = authResourceCodeConverter,
        authResourceGroupDao = authResourceGroupDao,
        authResourceGroupMemberDao = authResourceGroupMemberDao,
        authResourceGroupPermissionDao = authResourceGroupPermissionDao,
        permissionResourceGroupService = permissionResourceGroupService,
        permissionResourceGroupPermissionService = permissionResourceGroupPermissionService,
        permissionResourceMemberService = permissionResourceMemberService,
        deptService = deptService
    )

    @BeforeEach
    fun setUp() {
        every { iamConfiguration.systemId } returns "bk_ci"
        every {
            authResourceCodeConverter.code2IamCode(any(), any(), any())
        } answers { thirdArg<String>() }
        every {
            authResourceService.get(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns projectResource("source", "Source Project")
        every {
            authResourceService.get(
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns projectResource("target", "Target Project")
    }

    @Test
    fun `migrate should fail when target has undeletable non manager group`() {
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns emptyList()
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns listOf(
            projectGroup(projectCode = "target", groupCode = "manager", groupName = "管理员"),
            projectGroup(
                projectCode = "target",
                groupCode = "developer",
                groupName = "开发",
                defaultGroup = true,
                relationId = 2
            )
        )

        val result = service.migrate(
            ProjectGroupMigrationDTO(sourceProjectCode = "source", targetProjectCode = "target")
        )

        assertEquals(ProjectGroupMigrationStatus.FAILED, result.status)
        assertEquals(listOf("开发"), result.blockedTargetGroups)
    }

    @Test
    fun `dry run should map single resource scope by resource type and name`() {
        val sourceGroup = projectGroup(
            projectCode = "source",
            groupCode = "custom",
            groupName = "流水线维护",
            relationId = 10
        )
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns listOf(sourceGroup)
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns emptyList()
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "source", 10)
        } returns listOf(
            ResourceGroupPermissionDTO(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source",
                iamResourceCode = "source",
                groupCode = "custom",
                iamGroupId = 10,
                action = "pipeline_view",
                actionRelatedResourceType = "pipeline",
                relatedResourceType = "pipeline",
                relatedResourceCode = "p-source",
                relatedIamResourceCode = "p-source"
            )
        )
        every {
            authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = "source",
                iamGroupId = 10
            )
        } returns emptyList()
        every {
            authResourceService.listByResourceCodes("source", "pipeline", listOf("p-source"))
        } returns listOf(
            resourceInfo(
                projectCode = "source",
                resourceType = "pipeline",
                resourceCode = "p-source",
                resourceName = "Pipeline A"
            )
        )
        every { authResourceService.count("target", "pipeline", null) } returns 1L
        every {
            authResourceService.list("target", "pipeline", null, 200, 0)
        } returns listOf(
            resourceInfo(
                projectCode = "target",
                resourceType = "pipeline",
                resourceCode = "p-target",
                resourceName = "Pipeline A"
            )
        )

        val result = service.migrate(
            ProjectGroupMigrationDTO(
                sourceProjectCode = "source",
                targetProjectCode = "target",
                dryRun = true
            )
        )

        assertEquals(ProjectGroupMigrationStatus.DRY_RUN, result.status)
        assertEquals(1, result.groupResults.size)
        val groupResult = result.groupResults.single()
        assertEquals(ProjectGroupMigrationStatus.DRY_RUN, groupResult.status)
        assertEquals("p-target", groupResult.details.single().targetResourceCode)
    }

    @Test
    fun `dry run should skip single resource permission when target resource is missing`() {
        val sourceGroup = projectGroup(
            projectCode = "source",
            groupCode = "qc",
            groupName = "质检",
            relationId = 11
        )
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns listOf(sourceGroup)
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns emptyList()
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "source", 11)
        } returns listOf(
            ResourceGroupPermissionDTO(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source",
                iamResourceCode = "source",
                groupCode = "qc",
                iamGroupId = 11,
                action = "pipeline_view",
                actionRelatedResourceType = "pipeline",
                relatedResourceType = "pipeline",
                relatedResourceCode = "p-source-missing",
                relatedIamResourceCode = "p-source-missing"
            )
        )
        every {
            authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = "source",
                iamGroupId = 11
            )
        } returns emptyList()
        every {
            authResourceService.listByResourceCodes("source", "pipeline", listOf("p-source-missing"))
        } returns listOf(
            resourceInfo(
                projectCode = "source",
                resourceType = "pipeline",
                resourceCode = "p-source-missing",
                resourceName = "Pipeline Missing"
            )
        )
        every { authResourceService.count("target", "pipeline", null) } returns 0L

        val result = service.migrate(
            ProjectGroupMigrationDTO(
                sourceProjectCode = "source",
                targetProjectCode = "target",
                dryRun = true
            )
        )

        assertEquals(ProjectGroupMigrationStatus.DRY_RUN, result.status)
        val detail = result.groupResults.single().details.single()
        assertEquals(ProjectGroupMigrationStatus.SKIPPED, detail.status)
        assertEquals("Pipeline Missing", detail.sourceResourceName)
    }

    @Test
    fun `execute should migrate members with batch and template paths`() {
        val sourceGroup = projectGroup(
            projectCode = "source",
            groupCode = "custom",
            groupName = "测试组",
            relationId = 20
        )
        val targetGroup = projectGroup(
            projectCode = "target",
            groupCode = "custom",
            groupName = "测试组",
            relationId = 120
        )
        val targetRecord = mockk<com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord>()
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns listOf(sourceGroup)
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns emptyList()
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "source", 20)
        } returns emptyList()
        every {
            authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = "source",
                iamGroupId = 20
            )
        } returns listOf(
            member(memberId = "user1", memberType = MemberType.USER.type),
            member(memberId = "tpl1", memberType = MemberType.TEMPLATE.type)
        )
        every {
            permissionResourceGroupService.createEmptyProjectGroupWithCode("target", "custom", any())
        } returns 120
        every {
            authResourceGroupDao.getByRelationId(dslContext, "target", "120")
        } returns targetRecord
        every { authResourceGroupDao.convert(targetRecord) } returns targetGroup
        every {
            permissionResourceMemberService.batchAddResourceGroupMembers(
                projectCode = "target",
                iamGroupId = 120,
                expiredTime = any(),
                members = listOf("user1"),
                departments = emptyList()
            )
        } returns Result(true)
        every {
            permissionResourceMemberService.addGroupMember(
                projectCode = "target",
                memberId = "tpl1",
                memberType = MemberType.TEMPLATE.type,
                expiredAt = any(),
                iamGroupId = 120
            )
        } returns true
        every { deptService.isUserDeparted("user1") } returns false

        val result = service.migrate(
            ProjectGroupMigrationDTO(sourceProjectCode = "source", targetProjectCode = "target")
        )

        assertEquals(ProjectGroupMigrationStatus.SUCCESS, result.status)
        val groupResult = result.groupResults.single()
        assertEquals(2, groupResult.migratedMemberCount)
        verify(exactly = 1) {
            permissionResourceMemberService.batchAddResourceGroupMembers(
                projectCode = "target",
                iamGroupId = 120,
                expiredTime = any(),
                members = listOf("user1"),
                departments = emptyList()
            )
        }
        verify(exactly = 1) {
            permissionResourceMemberService.addGroupMember(
                projectCode = "target",
                memberId = "tpl1",
                memberType = MemberType.TEMPLATE.type,
                expiredAt = any(),
                iamGroupId = 120
            )
        }
    }

    @Test
    fun `execute should recreate non custom group as empty group before replaying permissions`() {
        val sourceGroup = projectGroup(
            projectCode = "source",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 21
        )
        val targetGroup = projectGroup(
            projectCode = "target",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 121
        )
        val targetRecord = mockk<com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord>()
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns listOf(sourceGroup)
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns emptyList()
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "source", 21)
        } returns emptyList()
        every {
            authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = "source",
                iamGroupId = 21
            )
        } returns emptyList()
        every {
            permissionResourceGroupService.createEmptyProjectGroupWithCode("target", "qc", any())
        } returns 121
        every {
            authResourceGroupDao.getByRelationId(dslContext, "target", "121")
        } returns targetRecord
        every { authResourceGroupDao.convert(targetRecord) } returns targetGroup

        val result = service.migrate(
            ProjectGroupMigrationDTO(sourceProjectCode = "source", targetProjectCode = "target")
        )

        assertEquals(ProjectGroupMigrationStatus.SUCCESS, result.status)
        verify(exactly = 1) {
            permissionResourceGroupService.createEmptyProjectGroupWithCode("target", "qc", any())
        }
        assertEquals("qc", result.groupResults.single().sourceGroupCode)
        assertEquals("qc", targetGroup.groupCode)
        verify(exactly = 0) {
            permissionResourceGroupService.createGroupAndPermissionsByGroupCode(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `execute should grant project scopes without monitor defaults`() {
        val sourceGroup = projectGroup(
            projectCode = "source",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 31
        )
        val targetGroup = projectGroup(
            projectCode = "target",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 131
        )
        val targetRecord = mockk<com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord>()
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns listOf(sourceGroup)
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns emptyList()
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "source", 31)
        } returns listOf(
            ResourceGroupPermissionDTO(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source",
                iamResourceCode = "source",
                groupCode = "qc",
                iamGroupId = 31,
                action = "pipeline_view",
                actionRelatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceCode = "source",
                relatedIamResourceCode = "source"
            )
        )
        every {
            authResourceGroupMemberDao.listResourceGroupMember(
                dslContext = dslContext,
                projectCode = "source",
                iamGroupId = 31
            )
        } returns emptyList()
        every {
            permissionResourceGroupService.createEmptyProjectGroupWithCode("target", "qc", any())
        } returns 131
        every {
            authResourceGroupDao.getByRelationId(dslContext, "target", "131")
        } returns targetRecord
        every { authResourceGroupDao.convert(targetRecord) } returns targetGroup
        every {
            permissionResourceGroupPermissionService.buildProjectPermissions("target", "Target Project", listOf("pipeline_view"))
        } returns "project-scope"
        every {
            permissionResourceGroupPermissionService.grantGroupPermission(
                authorizationScopesStr = "project-scope",
                projectCode = "target",
                projectName = "Target Project",
                resourceType = AuthResourceType.PROJECT.value,
                groupCode = "qc",
                iamResourceCode = "target",
                resourceName = "Target Project",
                iamGroupId = 131,
                registerMonitorPermission = false,
                filterResourceTypes = emptyList(),
                filterActions = emptyList()
            )
        } returns true

        val result = service.migrate(
            ProjectGroupMigrationDTO(sourceProjectCode = "source", targetProjectCode = "target")
        )

        assertEquals(ProjectGroupMigrationStatus.SUCCESS, result.status)
        verify(exactly = 1) {
            permissionResourceGroupPermissionService.grantGroupPermission(
                authorizationScopesStr = "project-scope",
                projectCode = "target",
                projectName = "Target Project",
                resourceType = AuthResourceType.PROJECT.value,
                groupCode = "qc",
                iamResourceCode = "target",
                resourceName = "Target Project",
                iamGroupId = 131,
                registerMonitorPermission = false,
                filterResourceTypes = emptyList(),
                filterActions = emptyList()
            )
        }
    }

    @Test
    fun `incremental should reuse target group and only grant missing permissions`() {
        val sourceGroup = projectGroup(
            projectCode = "source",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 41
        )
        val targetGroup = projectGroup(
            projectCode = "target",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 141
        )
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns listOf(sourceGroup)
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns listOf(targetGroup)
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "source", 41)
        } returns listOf(
            ResourceGroupPermissionDTO(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source",
                iamResourceCode = "source",
                groupCode = "qc",
                iamGroupId = 41,
                action = "pipeline_view",
                actionRelatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceCode = "source",
                relatedIamResourceCode = "source"
            ),
            ResourceGroupPermissionDTO(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source",
                iamResourceCode = "source",
                groupCode = "qc",
                iamGroupId = 41,
                action = "pipeline_edit",
                actionRelatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceCode = "source",
                relatedIamResourceCode = "source"
            ),
            ResourceGroupPermissionDTO(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source",
                iamResourceCode = "source",
                groupCode = "qc",
                iamGroupId = 41,
                action = "pipeline_view",
                actionRelatedResourceType = "pipeline",
                relatedResourceType = "pipeline",
                relatedResourceCode = "p-source-1",
                relatedIamResourceCode = "p-source-1"
            ),
            ResourceGroupPermissionDTO(
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source",
                iamResourceCode = "source",
                groupCode = "qc",
                iamGroupId = 41,
                action = "pipeline_edit",
                actionRelatedResourceType = "pipeline",
                relatedResourceType = "pipeline",
                relatedResourceCode = "p-source-2",
                relatedIamResourceCode = "p-source-2"
            )
        )
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "target", 141)
        } returns listOf(
            ResourceGroupPermissionDTO(
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target",
                iamResourceCode = "target",
                groupCode = "qc",
                iamGroupId = 141,
                action = "pipeline_view",
                actionRelatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceType = AuthResourceType.PROJECT.value,
                relatedResourceCode = "target",
                relatedIamResourceCode = "target"
            ),
            ResourceGroupPermissionDTO(
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target",
                iamResourceCode = "target",
                groupCode = "qc",
                iamGroupId = 141,
                action = "pipeline_view",
                actionRelatedResourceType = "pipeline",
                relatedResourceType = "pipeline",
                relatedResourceCode = "p-target-1",
                relatedIamResourceCode = "p-target-1"
            )
        )
        every {
            authResourceService.listByResourceCodes("source", "pipeline", listOf("p-source-1", "p-source-2"))
        } returns listOf(
            resourceInfo("source", "pipeline", "p-source-1", "Pipeline 1"),
            resourceInfo("source", "pipeline", "p-source-2", "Pipeline 2")
        )
        every { authResourceService.count("target", "pipeline", null) } returns 2L
        every {
            authResourceService.list("target", "pipeline", null, 200, 0)
        } returns listOf(
            resourceInfo("target", "pipeline", "p-target-1", "Pipeline 1"),
            resourceInfo("target", "pipeline", "p-target-2", "Pipeline 2")
        )
        every {
            permissionResourceGroupPermissionService.buildProjectPermissions(
                "target",
                "Target Project",
                listOf("pipeline_edit")
            )
        } returns "project-scope"
        every {
            permissionResourceGroupPermissionService.grantGroupPermission(
                authorizationScopesStr = "project-scope",
                projectCode = "target",
                projectName = "Target Project",
                resourceType = AuthResourceType.PROJECT.value,
                groupCode = "qc",
                iamResourceCode = "target",
                resourceName = "Target Project",
                iamGroupId = 141,
                registerMonitorPermission = false,
                filterResourceTypes = emptyList(),
                filterActions = emptyList()
            )
        } returns true
        every {
            permissionResourceGroupPermissionService.grantGroupPermission(
                authorizationScopesStr = any(),
                projectCode = "target",
                projectName = "Target Project",
                resourceType = AuthResourceType.PROJECT.value,
                groupCode = "qc",
                iamResourceCode = "target",
                resourceName = "Target Project",
                iamGroupId = 141,
                registerMonitorPermission = false,
                filterResourceTypes = emptyList(),
                filterActions = emptyList()
            )
        } returns true

        val result = service.migrateIncremental(
            ProjectGroupIncrementalMigrationDTO(
                sourceProjectCode = "source",
                targetProjectCode = "target"
            )
        )

        assertEquals(ProjectGroupMigrationStatus.SUCCESS, result.status)
        val groupResult = result.groupResults.single()
        assertEquals(listOf("pipeline_edit"), groupResult.projectActions)
        assertEquals(1, groupResult.singleResourcePermissionCount)
        assertEquals(ProjectGroupMigrationStatus.SUCCESS, groupResult.status)
        verify(exactly = 0) { permissionResourceGroupService.createEmptyProjectGroupWithCode(any(), any(), any()) }
        verify(exactly = 0) {
            permissionResourceMemberService.batchAddResourceGroupMembers(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            permissionResourceMemberService.addGroupMember(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `incremental should create missing target group with source group code`() {
        val sourceGroup = projectGroup(
            projectCode = "source",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 51
        )
        val targetGroup = projectGroup(
            projectCode = "target",
            groupCode = "qc",
            groupName = "质检组",
            relationId = 151
        )
        val targetRecord = mockk<com.tencent.devops.model.auth.tables.records.TAuthResourceGroupRecord>()
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "source",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "source"
            )
        } returns listOf(sourceGroup)
        every {
            authResourceGroupDao.getByResourceCode(
                dslContext = dslContext,
                projectCode = "target",
                resourceType = AuthResourceType.PROJECT.value,
                resourceCode = "target"
            )
        } returns emptyList()
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "source", 51)
        } returns emptyList()
        every {
            permissionResourceGroupService.createEmptyProjectGroupWithCode("target", "qc", any())
        } returns 151
        every {
            authResourceGroupDao.getByRelationId(dslContext, "target", "151")
        } returns targetRecord
        every { authResourceGroupDao.convert(targetRecord) } returns targetGroup
        every {
            authResourceGroupPermissionDao.listByGroupId(dslContext, "target", 151)
        } returns emptyList()

        val result = service.migrateIncremental(
            ProjectGroupIncrementalMigrationDTO(
                sourceProjectCode = "source",
                targetProjectCode = "target"
            )
        )

        assertEquals(ProjectGroupMigrationStatus.SUCCESS, result.status)
        verify(exactly = 1) {
            permissionResourceGroupService.createEmptyProjectGroupWithCode("target", "qc", any())
        }
        verify(exactly = 0) {
            permissionResourceMemberService.batchAddResourceGroupMembers(any(), any(), any(), any(), any())
        }
        verify(exactly = 0) {
            permissionResourceMemberService.addGroupMember(any(), any(), any(), any(), any())
        }
    }

    private fun projectResource(projectCode: String, projectName: String): AuthResourceInfo {
        return resourceInfo(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectName
        )
    }

    private fun resourceInfo(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): AuthResourceInfo {
        val now = LocalDateTime.now()
        return AuthResourceInfo(
            projectCode = projectCode,
            resourceType = resourceType,
            resourceCode = resourceCode,
            resourceName = resourceName,
            iamResourceCode = resourceCode,
            enable = true,
            relationId = projectCode,
            createUser = "tester",
            updateUser = "tester",
            createTime = now,
            updateTime = now
        )
    }

    private fun projectGroup(
        projectCode: String,
        groupCode: String,
        groupName: String,
        defaultGroup: Boolean = false,
        relationId: Int = 1
    ): AuthResourceGroup {
        return AuthResourceGroup(
            projectCode = projectCode,
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = projectCode,
            resourceName = projectCode,
            iamResourceCode = projectCode,
            groupCode = groupCode,
            groupName = groupName,
            defaultGroup = defaultGroup,
            relationId = relationId,
            description = groupName
        )
    }

    private fun member(memberId: String, memberType: String): AuthResourceGroupMember {
        return AuthResourceGroupMember(
            projectCode = "source",
            resourceType = AuthResourceType.PROJECT.value,
            resourceCode = "source",
            groupCode = "custom",
            iamGroupId = 20,
            memberId = memberId,
            memberName = memberId,
            memberType = memberType,
            expiredTime = LocalDateTime.now().plusDays(7)
        )
    }
}
