package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceHistoryRecord
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceOpHisRecord
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceRecord
import com.tencent.devops.model.remotedev.tables.records.TWorkspaceSharedRecord
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class WorkspaceServiceTest : BkCiAbstractTest() {
    private val redisOperation: RedisOperation = mockk()
    private val workspaceDao: WorkspaceDao = mockk()
    private val workspaceHistoryDao: WorkspaceHistoryDao = mockk()
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao = mockk()
    private val workspaceSharedDao: WorkspaceSharedDao = mockk()
    private val gitTransferService: GitTransferService = mockk()
    private val permissionService: PermissionService = mockk()
    private val sshService: SshPublicKeysService = mockk()
    private val dispatcher: RemoteDevDispatcher = mockk()
    private var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val remoteDevSettingDao: RemoteDevSettingDao = mockk()
    private val redisHeartBeat: RedisHeartBeat = mockk()
    private val webSocketDispatcher: WebSocketDispatcher = mockk()
    private val self: WorkspaceService = spyk(
        WorkspaceService(
            dslContext = dslContext,
            redisOperation = redisOperation,
            workspaceDao = workspaceDao,
            workspaceHistoryDao = workspaceHistoryDao,
            workspaceOpHistoryDao = workspaceOpHistoryDao,
            workspaceSharedDao = workspaceSharedDao,
            gitTransferService = gitTransferService,
            permissionService = permissionService,
            sshService = sshService,
            client = client,
            dispatcher = dispatcher,
            remoteDevSettingDao = remoteDevSettingDao,
            webSocketDispatcher = webSocketDispatcher,
            redisHeartBeat = redisHeartBeat
        ),
        recordPrivateCalls = true
    )

    private val ws4Default = TWorkspaceRecord(
        /* id = */ 1,
        /* projectId = */ "",
        /* name = */ "user001671019236319-cibkuizx",
        /* templateId = */ 1,
        /* url = */ "https://xxx/xxx/xx/xx.git",
        /* branch = */ "master",
        /* yaml = */ "",
        /* yamlPath = */ "",
        /* dockerfile = */ "",
        /* imagePath = */ "",
        /* workPath = */ "/data/landun/workspace/xxx",
        /* hostName = */ "127.0.0.1",
        /* cpu = */ 8,
        /* memory = */ 16,
        /* disk = */ 100,
        /* creator = */ "user00",
        /* creatorBgName = */ "xx事业群",
        /* creatorDeptName = */ "xx部",
        /* creatorCenterName = */ "xx中心",
        /* status = */ 1,
        /* createTime = */ LocalDateTime.parse("2022-11-14 20:00:36", formatter),
        /* updateTime = */ LocalDateTime.parse("2022-12-14 20:00:39", formatter),
        /* lastStatusUpdateTime = */ LocalDateTime.parse("2022-12-14 20:00:39", formatter),
        /* usageTime = */ 0,
        /* sleepingTime = */ 0
    )

    private val wsh4Default = TWorkspaceHistoryRecord(
        /* id = */ 1,
        /* workspaceId = */ 8,
        /* starter = */ "user00",
        /* stopper = */ "",
        /* startTime = */ LocalDateTime.parse("2022-11-28 00:27:16", formatter),
        /* endTime = */ null,
        /* lastSleepTimeCost = */ 0,
        /* updateTime = */ LocalDateTime.parse("2022-11-28 00:27:16", formatter),
        /* createdTime = */ LocalDateTime.parse("2022-11-28 00:27:16", formatter),
        ""

    )

    private val wsoh4Default = TWorkspaceOpHisRecord(
        /* id = */ 1,
        /* workspaceId = */ 1,
        /* operator = */ "user00",
        /* action = */ 0,
        /* actionMsg = */ "基于xxx的master分支创建了一个xxx的开发环境",
        /* createdTime = */ LocalDateTime.parse("2022-11-20 15:04:21", formatter),
        ""

    )

    private val wss4Default = TWorkspaceSharedRecord(
        /* id = */ 1,
        /* workspaceId = */ 1,
        /* operator = */ "user00",
        /* sharedUser = */ "user01",
        /* createdTime = */ LocalDateTime.parse("2022-12-09 16:17:08", formatter),
        "",
    )

    @Nested
    inner class ThrowableTest {
        @Test
        fun getAuthorizedGitRepository_1() {
            every {
                gitTransferService.getProjectList(
                    any(), any(), any(), any(), any(), any()
                )
            } throws RemoteServiceException(
                errorMessage = "ops! it's error!下游服务抛出了异常，并且返回了401", httpStatus = HTTP_401
            )
            Assertions.assertThrows(ErrorCodeException::class.java) {
                self.getAuthorizedGitRepository(
                    userId = "user00", search = null, page = null, pageSize = null
                )
            }
        }

        @Test
        fun getAuthorizedGitRepository_2() {
            every {
                gitTransferService.getProjectList(
                    any(), any(), any(), any(), any(), any()
                )
            } throws OauthForbiddenException("用户[user00]尚未进行OAUTH授权，请先授权。")
            Assertions.assertThrows(ErrorCodeException::class.java) {
                self.getAuthorizedGitRepository(
                    userId = "user00", search = null, page = null, pageSize = null
                )
            }
        }
    }
}
