/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.resources.job

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.constant.EnvironmentMessageCode.ERROR_QUERY_LOG_HOST_NUM_EXCEEDED
import com.tencent.devops.environment.pojo.job.jobreq.IpInfo
import com.tencent.devops.environment.pojo.job.jobreq.QueryExecuteObjectLogsReq
import com.tencent.devops.environment.pojo.job.jobresp.QueryExecuteObjectLogsResult
import com.tencent.devops.environment.service.job.JobService
import com.tencent.devops.environment.service.job.PermissionManageService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TencentServiceJobResourceImplBatchGetLogTest {

    private val jobService: JobService = mockk()
    private val permissionManageService: PermissionManageService = mockk()
    private val client: Client = mockk()
    private val projectAuthResource: ServiceProjectAuthResource = mockk()
    private lateinit var resource: TencentServiceJobResourceImpl
    private val reqSlot = slot<QueryExecuteObjectLogsReq>()

    @BeforeEach
    fun setUp() {
        resource = TencentServiceJobResourceImpl(
            jobService = jobService,
            opService = mockk(),
            permissionManageService = permissionManageService,
            updateCmdbNodeService = mockk(),
            updateGseAgentInfoService = mockk(),
            tencentStockDataUpdateService = mockk(),
            installTaskService = mockk(),
            apiGwAgentService = mockk(),
            client = client,
            maxQueryLogHostNum = MAX_HOST_NUM
        )
        every { client.get(ServiceProjectAuthResource::class) } returns projectAuthResource
        every { projectAuthResource.isProjectMember(USER_ID, PROJECT_ID) } returns Result(true)
        every { permissionManageService.isJobInsBelongToProj(PROJECT_ID, JOB_INSTANCE_ID) } returns true
        every { jobService.batchGetExecuteObjectLog(PROJECT_ID, capture(reqSlot)) } answers {
            QueryExecuteObjectLogsResult(
                jobInstanceId = JOB_INSTANCE_ID,
                stepInstanceId = STEP_INSTANCE_ID,
                logType = 1
            )
        }
    }

    @Test
    fun `both hostIdList and ipList empty throws ParamBlankException`() {
        assertThrows<ParamBlankException> {
            resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = emptyList(), ipList = null))
        }
        verify(exactly = 0) { jobService.batchGetExecuteObjectLog(any(), any()) }
    }

    @Test
    fun `hostIdList with max hosts passes`() {
        val hostIdList = (1L..MAX_HOST_NUM).toList()

        val result = resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = hostIdList))

        assertThat(result.data?.jobInstanceId).isEqualTo(JOB_INSTANCE_ID)
        assertThat(reqSlot.captured.hostIdList).containsExactlyElementsOf(hostIdList)
    }

    @Test
    fun `hostIdList exceeding max hosts throws ErrorCodeException`() {
        val ex = assertThrows<ErrorCodeException> {
            resource.batchGetExecuteObjectLog(
                USER_ID, PROJECT_ID, buildReq(hostIdList = (1L..MAX_HOST_NUM + 1).toList())
            )
        }

        assertThat(ex.errorCode).isEqualTo(ERROR_QUERY_LOG_HOST_NUM_EXCEEDED)
        assertThat(ex.params).containsExactly(MAX_HOST_NUM.toString())
        verify(exactly = 0) { jobService.batchGetExecuteObjectLog(any(), any()) }
    }

    @Test
    fun `non-empty hostIdList ignores oversized ipList`() {
        val ipList = (1..20).map { IpInfo(bkCloudId = 0, ip = "127.0.0.$it") }

        resource.batchGetExecuteObjectLog(
            USER_ID, PROJECT_ID, buildReq(hostIdList = listOf(1L, 2L), ipList = ipList)
        )

        assertThat(reqSlot.captured.hostIdList).containsExactly(1L, 2L)
        assertThat(reqSlot.captured.ipList).isNull()
    }

    @Test
    fun `empty hostIdList falls back to ipList`() {
        val ipList = listOf(IpInfo(bkCloudId = 0, ip = "127.0.0.1"))

        resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = emptyList(), ipList = ipList))

        assertThat(reqSlot.captured.hostIdList).isNull()
        assertThat(reqSlot.captured.ipList).containsExactlyElementsOf(ipList)
    }

    @Test
    fun `ipList exceeding max hosts throws ErrorCodeException when hostIdList is empty`() {
        val ipList = (1..MAX_HOST_NUM + 1).map { IpInfo(bkCloudId = 0, ip = "127.0.0.$it") }

        val ex = assertThrows<ErrorCodeException> {
            resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = null, ipList = ipList))
        }

        assertThat(ex.errorCode).isEqualTo(ERROR_QUERY_LOG_HOST_NUM_EXCEEDED)
    }

    @Test
    fun `duplicated hosts are counted after distinct`() {
        val hostIdList = (1L..MAX_HOST_NUM).toList() + (1L..MAX_HOST_NUM).toList()

        resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = hostIdList))

        assertThat(reqSlot.captured.hostIdList).hasSize(MAX_HOST_NUM)
    }

    @Test
    fun `job instance not belonging to project throws CustomException`() {
        every { permissionManageService.isJobInsBelongToProj(PROJECT_ID, JOB_INSTANCE_ID) } returns false
        mockkObject(I18nUtil)
        try {
            every { I18nUtil.getCodeLanMessage(messageCode = any()) } returns "not belong"

            assertThrows<CustomException> {
                resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = listOf(1L)))
            }
            verify(exactly = 0) { jobService.batchGetExecuteObjectLog(any(), any()) }
        } finally {
            unmockkObject(I18nUtil)
        }
    }

    @Test
    fun `user not in project throws PermissionForbiddenException`() {
        every { projectAuthResource.isProjectMember(USER_ID, PROJECT_ID) } returns Result(false)
        assertNotProjectMemberRejected()
    }

    @Test
    fun `null member check result is treated as not project member`() {
        every { projectAuthResource.isProjectMember(USER_ID, PROJECT_ID) } returns Result(status = 0, data = null)
        assertNotProjectMemberRejected()
    }

    private fun assertNotProjectMemberRejected() {
        mockkObject(I18nUtil)
        try {
            every { I18nUtil.getCodeLanMessage(messageCode = any(), params = any()) } returns "not project member"

            assertThrows<PermissionForbiddenException> {
                resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = listOf(1L)))
            }
            verify(exactly = 0) { permissionManageService.isJobInsBelongToProj(any(), any()) }
            verify(exactly = 0) { jobService.batchGetExecuteObjectLog(any(), any()) }
        } finally {
            unmockkObject(I18nUtil)
        }
    }

    @Test
    fun `invalid host params are rejected before project member check`() {
        assertThrows<ParamBlankException> {
            resource.batchGetExecuteObjectLog(USER_ID, PROJECT_ID, buildReq(hostIdList = null, ipList = null))
        }
        verify(exactly = 0) { projectAuthResource.isProjectMember(any(), any()) }
    }

    private fun buildReq(hostIdList: List<Long>? = null, ipList: List<IpInfo>? = null) =
        QueryExecuteObjectLogsReq(
            jobInstanceId = JOB_INSTANCE_ID,
            hostIdList = hostIdList,
            ipList = ipList
        )

    companion object {
        private const val USER_ID = "user"
        private const val PROJECT_ID = "project"
        private const val JOB_INSTANCE_ID = 100L
        private const val STEP_INSTANCE_ID = 200L
        private const val MAX_HOST_NUM = 10
    }
}
