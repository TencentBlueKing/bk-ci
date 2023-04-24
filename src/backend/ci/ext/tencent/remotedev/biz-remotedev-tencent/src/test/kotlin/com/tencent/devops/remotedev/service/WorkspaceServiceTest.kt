package com.tencent.devops.remotedev.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import io.mockk.mockk
import io.mockk.spyk
import java.time.format.DateTimeFormatter

internal class WorkspaceServiceTest : BkCiAbstractTest() {
    private val redisOperation: RedisOperation = mockk()
    private val workspaceDao: WorkspaceDao = mockk()
    private val workspaceHistoryDao: WorkspaceHistoryDao = mockk()
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao = mockk()
    private val workspaceSharedDao: WorkspaceSharedDao = mockk()
    private val remoteDevGitTransfer: RemoteDevGitTransfer = mockk()
    private val permissionService: PermissionService = mockk()
    private val sshService: SshPublicKeysService = mockk()
    private val dispatcher: RemoteDevDispatcher = mockk()
    private var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val remoteDevSettingDao: RemoteDevSettingDao = mockk()
    private val redisHeartBeat: RedisHeartBeat = mockk()
    private val webSocketDispatcher: WebSocketDispatcher = mockk()
    private val remoteDevBillingDao: RemoteDevBillingDao = mockk()
    private val commonService: CommonService = mockk()
    private val profile: Profile = mockk()
    private val redisCache: RedisCacheService = mockk()
    private val bkTicketServie: BkTicketService = mockk()
    private val self: WorkspaceService = spyk(
        WorkspaceService(
            dslContext = dslContext,
            redisOperation = redisOperation,
            workspaceDao = workspaceDao,
            workspaceHistoryDao = workspaceHistoryDao,
            workspaceOpHistoryDao = workspaceOpHistoryDao,
            workspaceSharedDao = workspaceSharedDao,
            remoteDevGitTransfer = remoteDevGitTransfer,
            permissionService = permissionService,
            sshService = sshService,
            client = client,
            dispatcher = dispatcher,
            remoteDevSettingDao = remoteDevSettingDao,
            webSocketDispatcher = webSocketDispatcher,
            redisHeartBeat = redisHeartBeat,
            remoteDevBillingDao = remoteDevBillingDao,
            redisCache = redisCache,
            bkTicketServie = bkTicketServie,
            profile = profile
        ),
        recordPrivateCalls = true
    )
}
