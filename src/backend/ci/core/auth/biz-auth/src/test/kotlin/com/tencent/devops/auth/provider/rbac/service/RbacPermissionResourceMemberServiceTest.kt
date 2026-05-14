package com.tencent.devops.auth.provider.rbac.service

import com.tencent.bk.sdk.iam.dto.manager.RoleGroupMemberInfo
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.dao.AuthResourceSyncDao
import com.tencent.devops.auth.pojo.enum.MemberType
import com.tencent.devops.auth.provider.rbac.pojo.event.AuthProjectLevelPermissionsSyncEvent
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.test.BkCiAbstractTest
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class RbacPermissionResourceMemberServiceTest : BkCiAbstractTest() {

    private val authResourceService: AuthResourceService = mockk(relaxed = true)
    private val iamV2ManagerService: V2ManagerService = mockk(relaxed = true)
    private val authResourceGroupDao: AuthResourceGroupDao = mockk(relaxed = true)
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao = mockk(relaxed = true)
    private val deptService: DeptService = mockk(relaxed = true)
    private val authResourceSyncDao: AuthResourceSyncDao = mockk(relaxed = true)
    private val traceEventDispatcher: TraceEventDispatcher = mockk(relaxed = true)

    private lateinit var service: RbacPermissionResourceMemberService

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = RbacPermissionResourceMemberService(
            authResourceService = authResourceService,
            iamV2ManagerService = iamV2ManagerService,
            authResourceGroupDao = authResourceGroupDao,
            authResourceGroupMemberDao = authResourceGroupMemberDao,
            dslContext = dslContext,
            deptService = deptService,
            authResourceSyncDao = authResourceSyncDao,
            traceEventDispatcher = traceEventDispatcher
        )
    }

    @Test
    fun `batchAddResourceGroupMembers returns skipped details when nothing is added`() {
        val now = System.currentTimeMillis() / 1000
        val groupMembers = listOf(
            RoleGroupMemberInfo().apply {
                id = "existingUser"
                type = MemberType.USER.type
                expiredAt = now + TimeUnit.DAYS.toSeconds(365)
            },
            RoleGroupMemberInfo().apply {
                id = "deptA"
                type = MemberType.DEPARTMENT.type
                expiredAt = now + TimeUnit.DAYS.toSeconds(365)
            }
        )

        every {
            authResourceGroupDao.isGroupBelongToProject(
                dslContext = dslContext,
                projectCode = "p",
                groupId = "1"
            )
        } returns true
        every { iamV2ManagerService.getRoleGroupMemberV2(1, any()) } returns mockk {
            every { results } returns groupMembers
        }
        every {
            deptService.listDepartedMembers(
                memberIds = listOf("ghost", "existingUser", "deptCoveredUser")
            )
        } returns listOf("ghost")
        every { deptService.getUserDeptInfo("deptCoveredUser") } returns setOf("deptA")

        val result = service.batchAddResourceGroupMembers(
            projectCode = "p",
            iamGroupId = 1,
            expiredTime = now + TimeUnit.DAYS.toSeconds(30),
            members = listOf("ghost", "existingUser", "deptCoveredUser"),
            departments = listOf("deptA")
        )

        assertFalse(result.data!!)
        assertEquals(
            "未新增任何成员；以下用户不存在或已离职，未添加：ghost；" +
                "以下用户已在当前组中，无需重复添加：existingUser；" +
                "以下用户已通过所在组织加入当前组，无需重复添加：deptCoveredUser；" +
                "以下组织已在当前组中，无需重复添加：deptA",
            result.message
        )
        verify(exactly = 0) { iamV2ManagerService.createRoleGroupMemberV2(any(), any()) }
        verify(exactly = 1) {
            traceEventDispatcher.dispatch(ofType(AuthProjectLevelPermissionsSyncEvent::class))
        }
    }
}
